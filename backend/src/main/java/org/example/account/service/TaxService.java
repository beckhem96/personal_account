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

    private static final BigDecimal STOCK_DEDUCTION = new BigDecimal("2500000"); // 250만원 기본공제
    private static final BigDecimal TAX_RATE = new BigDecimal("0.22"); // 22% (양도세 20% + 지방세 2%)

    public TaxStockResponse calculateStockTax(TaxStockRequest request) {
        BigDecimal profit = request.totalSellAmount().subtract(request.totalBuyAmount());
        
        if (profit.compareTo(STOCK_DEDUCTION) <= 0) {
            return new TaxStockResponse(profit, STOCK_DEDUCTION, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal taxBase = profit.subtract(STOCK_DEDUCTION);
        BigDecimal estimatedTax = taxBase.multiply(TAX_RATE).setScale(0, RoundingMode.FLOOR); // 원단위 절사

        return new TaxStockResponse(profit, STOCK_DEDUCTION, taxBase, estimatedTax);
    }

    public YearEndSettlementResponse simulateYearEndSettlement(YearEndSettlementRequest request) {
        // 1. 최저 사용금액 (총급여의 25%)
        BigDecimal minUsageThreshold = request.totalSalary().multiply(new BigDecimal("0.25"));
        
        BigDecimal totalUsage = request.creditCardAmount().add(request.debitCashAmount());
        BigDecimal estimatedDeduction = BigDecimal.ZERO;
        String guideMessage = "총 급여의 25%까지는 신용카드를 사용하여 혜택을 챙기세요.";

        // 총 사용액이 최저 사용금액을 넘은 경우
        if (totalUsage.compareTo(minUsageThreshold) > 0) {
            // 초과분 계산 (단순화: 신용카드부터 채웠다고 가정하거나, 비율대로 계산해야 함.
            // 여기서는 일반적인 절세 전략에 따라: 최저한도까지는 신용카드(혜택), 초과분은 체크카드(30%) 유리)
            
            // 시뮬레이션 로직:
            // - 신용카드 공제율: 15%
            // - 체크/현금 공제율: 30%
            
            // 초과 사용액
            BigDecimal excessAmount = totalUsage.subtract(minUsageThreshold);
            
            // (단순화) 공제액 계산: 실제로는 복잡하지만, 여기서는 단순히 초과분의 구성비율을 고려하지 않고
            // "체크카드를 더 썼다면 공제가 얼마나 더 되었을지" 가이드를 주는 방향으로 구현.
            
            // 현재 공제액 추정 (매우 단순화된 버전)
            // 가정: 최저한도는 신용카드로 채웠다고 가정 (혜택 때문)
            // 남은 신용카드 사용액 * 15% + 체크카드 사용액 * 30%
            
            BigDecimal remainingCredit = request.creditCardAmount().subtract(minUsageThreshold);
            if (remainingCredit.compareTo(BigDecimal.ZERO) < 0) {
                remainingCredit = BigDecimal.ZERO;
            }
            
            // 체크카드는 최저한도 채우는데 쓰이지 않았다고 가정 (신용카드 우선 전략)
            // 만약 신용카드만으로 최저한도 못 채웠으면 체크카드 일부가 최저한도로 들어감.
            
            BigDecimal credidCardUsedForThreshold = request.creditCardAmount().min(minUsageThreshold);
            BigDecimal thresholdRemains = minUsageThreshold.subtract(credidCardUsedForThreshold);
            
            BigDecimal debitUsedForThreshold = request.debitCashAmount().min(thresholdRemains);
            BigDecimal remainingDebit = request.debitCashAmount().subtract(debitUsedForThreshold);
            
            // 공제액 계산
            BigDecimal creditDeduction = remainingCredit.multiply(new BigDecimal("0.15"));
            BigDecimal debitDeduction = remainingDebit.multiply(new BigDecimal("0.30"));
            
            estimatedDeduction = creditDeduction.add(debitDeduction).setScale(0, RoundingMode.FLOOR);
            
            guideMessage = "최저 사용금액을 초과했습니다. 이제부터는 공제율이 높은 체크카드/현금영수증을 사용하는 것이 유리합니다.";
            
            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                 guideMessage += String.format(" 신용카드 초과 사용분(%.0f원)을 체크카드로 썼다면 약 %.0f원을 더 공제받을 수 있었습니다.", 
                         remainingCredit.doubleValue(), 
                         remainingCredit.multiply(new BigDecimal("0.15")).doubleValue());
            }
        } else {
            BigDecimal needed = minUsageThreshold.subtract(totalUsage);
            guideMessage = String.format("최저 사용금액(총 급여의 25%%)까지 %.0f원 남았습니다. 신용카드 혜택을 우선적으로 챙기세요.", needed.doubleValue());
        }

        return new YearEndSettlementResponse(minUsageThreshold, estimatedDeduction, guideMessage);
    }
}
