package org.example.account.service;

import org.example.account.dto.TaxStockRequest;
import org.example.account.dto.TaxStockResponse;
import org.example.account.dto.YearEndSettlementRequest;
import org.example.account.dto.YearEndSettlementResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

    private static final BigDecimal STOCK_DEDUCTION = new BigDecimal("2500000"); // 주식 양도 기본공제 250만원
    private static final BigDecimal TAX_RATE = new BigDecimal("0.22"); // 양도세 20% + 지방세 2%

    // 연말정산 공제율
    private static final BigDecimal RATE_CREDIT = new BigDecimal("0.15");
    private static final BigDecimal RATE_DEBIT = new BigDecimal("0.30");
    private static final BigDecimal RATE_MARKET = new BigDecimal("0.40");
    private static final BigDecimal RATE_TRANSPORT = new BigDecimal("0.40");

    // 최저사용금액 (총급여의 25%)
    private static final BigDecimal MIN_USAGE_RATIO = new BigDecimal("0.25");

    // 일반 공제한도 (총급여 7천만원 기준)
    private static final BigDecimal SALARY_THRESHOLD = new BigDecimal("70000000");
    private static final BigDecimal GENERAL_LIMIT_LOW = new BigDecimal("3000000");  // 7천만원 이하
    private static final BigDecimal GENERAL_LIMIT_HIGH = new BigDecimal("2500000"); // 7천만원 초과

    // 추가 공제한도 (각 100만원)
    private static final BigDecimal EXTRA_LIMIT = new BigDecimal("1000000");

    public TaxStockResponse calculateStockTax(TaxStockRequest request) {
        BigDecimal profit = request.totalSellAmount().subtract(request.totalBuyAmount());

        if (profit.compareTo(STOCK_DEDUCTION) <= 0) {
            return new TaxStockResponse(profit, STOCK_DEDUCTION, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal taxBase = profit.subtract(STOCK_DEDUCTION);
        BigDecimal estimatedTax = taxBase.multiply(TAX_RATE).setScale(0, RoundingMode.FLOOR);

        return new TaxStockResponse(profit, STOCK_DEDUCTION, taxBase, estimatedTax);
    }

    public YearEndSettlementResponse simulateYearEndSettlement(YearEndSettlementRequest request) {
        BigDecimal salary = nz(request.totalSalary());
        BigDecimal credit = nz(request.creditCardAmount());
        BigDecimal debit = nz(request.debitCashAmount());
        BigDecimal market = nz(request.traditionalMarketAmount());
        BigDecimal transport = nz(request.publicTransportAmount());

        BigDecimal minThreshold = salary.multiply(MIN_USAGE_RATIO).setScale(0, RoundingMode.FLOOR);
        BigDecimal totalUsage = credit.add(debit).add(market).add(transport);

        // 최저사용금액 미달: 공제 0
        if (totalUsage.compareTo(minThreshold) <= 0) {
            BigDecimal needed = minThreshold.subtract(totalUsage);
            String msg = String.format("최저 사용금액(총급여 25%%)까지 %s원 부족합니다. 우선 신용카드로 채워 카드 혜택을 챙기세요.",
                    formatWon(needed));
            return new YearEndSettlementResponse(minThreshold,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    generalLimit(salary), BigDecimal.ZERO, msg);
        }

        // 공제율 낮은 순(신용→체크→시장→교통)으로 최저사용금액 차감 → 각 항목의 "공제대상액" 산출
        BigDecimal remaining = minThreshold;
        BigDecimal creditTarget = subtractThreshold(credit, remaining);
        remaining = remaining.subtract(credit.subtract(creditTarget));
        BigDecimal debitTarget = subtractThreshold(debit, remaining);
        remaining = remaining.subtract(debit.subtract(debitTarget));
        BigDecimal marketTarget = subtractThreshold(market, remaining);
        remaining = remaining.subtract(market.subtract(marketTarget));
        BigDecimal transportTarget = subtractThreshold(transport, remaining);

        // 항목별 공제액
        BigDecimal creditDed = creditTarget.multiply(RATE_CREDIT).setScale(0, RoundingMode.FLOOR);
        BigDecimal debitDed = debitTarget.multiply(RATE_DEBIT).setScale(0, RoundingMode.FLOOR);
        BigDecimal marketDedRaw = marketTarget.multiply(RATE_MARKET).setScale(0, RoundingMode.FLOOR);
        BigDecimal transportDedRaw = transportTarget.multiply(RATE_TRANSPORT).setScale(0, RoundingMode.FLOOR);

        // 일반한도 적용 (신용+체크)
        BigDecimal general = generalLimit(salary);
        BigDecimal generalDed = creditDed.add(debitDed);
        BigDecimal generalCapped = generalDed.min(general);

        // 일반한도 초과분이 있으면 신용/체크 공제는 비율대로 줄여 한도에 맞춤
        BigDecimal creditFinal = creditDed;
        BigDecimal debitFinal = debitDed;
        if (generalDed.compareTo(general) > 0 && generalDed.signum() > 0) {
            creditFinal = creditDed.multiply(generalCapped).divide(generalDed, 0, RoundingMode.FLOOR);
            debitFinal = generalCapped.subtract(creditFinal);
        }

        // 전통시장/대중교통 각 100만원 한도
        BigDecimal marketDed = marketDedRaw.min(EXTRA_LIMIT);
        BigDecimal transportDed = transportDedRaw.min(EXTRA_LIMIT);

        BigDecimal totalDed = creditFinal.add(debitFinal).add(marketDed).add(transportDed);

        String guide = buildGuide(generalDed, general, marketDedRaw, transportDedRaw, credit);

        return new YearEndSettlementResponse(minThreshold,
                creditFinal, debitFinal, marketDed, transportDed,
                general, totalDed, guide);
    }

    private BigDecimal subtractThreshold(BigDecimal amount, BigDecimal remaining) {
        if (remaining.signum() <= 0) return amount;
        BigDecimal used = amount.min(remaining);
        return amount.subtract(used);
    }

    private BigDecimal generalLimit(BigDecimal salary) {
        return salary.compareTo(SALARY_THRESHOLD) <= 0 ? GENERAL_LIMIT_LOW : GENERAL_LIMIT_HIGH;
    }

    private String buildGuide(BigDecimal generalDed, BigDecimal generalLimit,
                              BigDecimal marketDed, BigDecimal transportDed, BigDecimal credit) {
        if (generalDed.compareTo(generalLimit) >= 0) {
            if (marketDed.compareTo(EXTRA_LIMIT) < 0 || transportDed.compareTo(EXTRA_LIMIT) < 0) {
                return "일반 공제한도에 도달했습니다. 추가 절세는 전통시장(40%) 또는 대중교통(40%) 사용으로 가능합니다.";
            }
            return "모든 공제한도에 도달했습니다. 추가 사용은 공제 효과가 없습니다.";
        }
        if (credit.signum() > 0) {
            return "체크카드/현금영수증은 공제율이 30%로 신용카드(15%)의 2배입니다. 가능하면 체크카드 사용 비중을 늘리세요.";
        }
        return "현재 체크카드/현금영수증 위주로 사용 중입니다. 신용카드 혜택과 공제율 차이를 비교해 균형 있게 사용하세요.";
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String formatWon(BigDecimal v) {
        return String.format("%,d", v.setScale(0, RoundingMode.FLOOR).toBigInteger());
    }
}
