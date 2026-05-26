package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.LoanProductCode;
import org.example.account.dto.AcquisitionCostRequest;
import org.example.account.dto.AcquisitionCostResponse;
import org.example.account.dto.ApartmentDealsResponse;
import org.example.account.dto.LoanCostRequest;
import org.example.account.dto.LoanCostResponse;
import org.example.account.dto.LoanProductInfo;
import org.example.account.dto.LoanRepaymentRequest;
import org.example.account.dto.LoanRepaymentResponse;
import org.example.account.dto.RegionInfo;
import org.example.account.service.HousingAcquisitionCostService;
import org.example.account.service.HousingLoanCostService;
import org.example.account.service.HousingLoanRepaymentService;
import org.example.account.service.HousingMarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/housing")
@RequiredArgsConstructor
public class HousingController {

    private final HousingAcquisitionCostService acquisitionCostService;
    private final HousingLoanCostService loanCostService;
    private final HousingLoanRepaymentService loanRepaymentService;
    private final HousingMarketService marketService;

    @PostMapping("/acquisition-cost")
    public ResponseEntity<AcquisitionCostResponse> calculateAcquisitionCost(@RequestBody AcquisitionCostRequest request) {
        return ResponseEntity.ok(acquisitionCostService.calculate(request));
    }

    @PostMapping("/loan-cost")
    public ResponseEntity<LoanCostResponse> calculateLoanCost(@RequestBody LoanCostRequest request) {
        return ResponseEntity.ok(loanCostService.calculate(request));
    }

    @PostMapping("/loan-repayment")
    public ResponseEntity<LoanRepaymentResponse> calculateLoanRepayment(@RequestBody LoanRepaymentRequest request) {
        return ResponseEntity.ok(loanRepaymentService.calculate(request));
    }

    @GetMapping("/loan-products")
    public ResponseEntity<List<LoanProductInfo>> getLoanProducts() {
        return ResponseEntity.ok(List.of(
                new LoanProductInfo(LoanProductCode.DIDIMDOL,
                        "디딤돌 대출",
                        "주택도시기금 무주택자 대상 저금리 정책 대출",
                        new BigDecimal("400000000"),
                        new BigDecimal("3.0"),
                        List.of("무주택 세대주", "부부합산 연소득 6천만원 이하 (생애최초 7천만)",
                                "주택가격 5억 이하 (생애최초·2자녀+ 6억 이하)", "전용면적 85㎡ 이하")),
                new LoanProductInfo(LoanProductCode.BOGEUMJARI,
                        "보금자리론",
                        "주택금융공사 장기 고정금리 대출",
                        new BigDecimal("700000000"),
                        new BigDecimal("3.95"),
                        List.of("무주택 또는 1주택자(처분 조건)", "부부합산 연소득 7천만원 이하 (신혼·다자녀 별도)",
                                "주택가격 6억 이하", "10~50년 만기")),
                new LoanProductInfo(LoanProductCode.GENERAL,
                        "일반 주택담보대출",
                        "시중은행 변동/혼합금리 주담대",
                        new BigDecimal("1000000000"),
                        new BigDecimal("4.5"),
                        List.of("소득증빙 가능", "LTV/DSR 규제 적용",
                                "조정대상지역은 LTV 50%, 비조정 70%", "DSR 40% 한도 (은행 기준)"))
        ));
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionInfo.RegionTree>> getRegions() {
        return ResponseEntity.ok(marketService.getRegions());
    }

    @GetMapping("/apartment-deals")
    public ResponseEntity<ApartmentDealsResponse> getApartmentDeals(
            @RequestParam String lawdCd,
            @RequestParam String dealYearMonth,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea) {
        return ResponseEntity.ok(marketService.getDeals(lawdCd, dealYearMonth, minPrice, maxPrice, minArea));
    }
}
