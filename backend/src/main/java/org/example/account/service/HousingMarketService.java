package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.MolitClient;
import org.example.account.dto.ApartmentDealDto;
import org.example.account.dto.ApartmentDealsResponse;
import org.example.account.dto.RegionInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 국토부 실거래가 기반 시장 정보 서비스.
 * - 법정동 코드 트리 로딩 (앱 기동 시 1회)
 * - 권역/시군구별 거래 조회 (Caffeine 캐싱, 24h TTL)
 * - 가격/면적 필터 + 권역 평균가 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousingMarketService {

    private final MolitClient molitClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<RegionInfo.RegionTree> regionTree = List.of();
    private Map<String, RegionInfo> lawdIndex = new HashMap<>();

    @PostConstruct
    public void loadLawdCodes() {
        try (var is = new ClassPathResource("lawd-codes.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            List<RegionInfo.RegionTree> trees = new ArrayList<>();
            Map<String, RegionInfo> idx = new HashMap<>();

            for (JsonNode region : root.path("regions")) {
                String regionKey = region.path("region").asText();
                String regionLabel = region.path("regionLabel").asText();
                List<RegionInfo> districts = new ArrayList<>();
                for (JsonNode d : region.path("districts")) {
                    RegionInfo info = new RegionInfo(d.path("code").asText(), d.path("name").asText(), regionLabel);
                    districts.add(info);
                    idx.put(info.code(), info);
                }
                trees.add(new RegionInfo.RegionTree(regionKey, regionLabel, districts));
            }
            this.regionTree = List.copyOf(trees);
            this.lawdIndex = Map.copyOf(idx);
            log.info("법정동 코드 로드 완료: 권역 {}개, 시군구 {}개", regionTree.size(), lawdIndex.size());
        } catch (Exception e) {
            log.error("lawd-codes.json 로딩 실패", e);
        }
    }

    public List<RegionInfo.RegionTree> getRegions() {
        return regionTree;
    }

    /**
     * 시군구 × 거래월의 실거래 조회 + 필터 + 평균가.
     */
    public ApartmentDealsResponse getDeals(String lawdCd, String dealYearMonth,
                                            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minArea) {
        List<ApartmentDealDto> all = fetchCached(lawdCd, dealYearMonth);

        BigDecimal avgPrice = average(all.stream().map(ApartmentDealDto::dealAmount).toList());
        BigDecimal avgPerSqm = averagePerSqm(all);

        List<ApartmentDealDto> filtered = all.stream()
                .filter(d -> minPrice == null || d.dealAmount().compareTo(minPrice) >= 0)
                .filter(d -> maxPrice == null || d.dealAmount().compareTo(maxPrice) <= 0)
                .filter(d -> minArea == null || d.exclusiveArea().compareTo(minArea) >= 0)
                .sorted(Comparator.comparing(ApartmentDealDto::dealDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new ApartmentDealsResponse(avgPrice, avgPerSqm, all.size(), filtered.size(), filtered);
    }

    @Cacheable(value = "apartmentDeals", key = "#lawdCd + ':' + #dealYearMonth")
    public List<ApartmentDealDto> fetchCached(String lawdCd, String dealYearMonth) {
        return molitClient.fetchDeals(lawdCd, dealYearMonth);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(values.size()), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal averagePerSqm(List<ApartmentDealDto> deals) {
        var withArea = deals.stream()
                .filter(d -> d.exclusiveArea() != null && d.exclusiveArea().signum() > 0)
                .toList();
        if (withArea.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalPrice = withArea.stream().map(ApartmentDealDto::dealAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalArea = withArea.stream().map(ApartmentDealDto::exclusiveArea).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalPrice.divide(totalArea, 0, RoundingMode.HALF_UP);
    }
}
