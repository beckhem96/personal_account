package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.CardType;
import org.example.account.domain.Category;
import org.example.account.domain.PaymentMethod;
import org.example.account.domain.Transaction;
import org.example.account.domain.TransactionType;
import org.example.account.domain.YearEndCategory;
import org.example.account.dto.TaxStockRequest;
import org.example.account.dto.TaxStockResponse;
import org.example.account.dto.YearEndFullRequest;
import org.example.account.dto.YearEndFullResponse;
import org.example.account.dto.YearEndSettlementRequest;
import org.example.account.dto.YearEndSettlementResponse;
import org.example.account.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final TransactionRepository transactionRepository;

    private static final String SALARY_CATEGORY_NAME = "월급";

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

    // ── 연말정산 정밀 계산 (결정세액·환급액) ──────────────────────────────
    private static final BigDecimal MEDICAL_FLOOR_RATIO = new BigDecimal("0.03"); // 의료비 최저사용 3%
    private static final BigDecimal BASIC_PERSON = new BigDecimal("1500000"); // 기본공제 1인 150만
    private static final BigDecimal SENIOR_ADD = new BigDecimal("1000000");   // 경로우대 100만
    private static final BigDecimal DISABLED_ADD = new BigDecimal("2000000"); // 장애인 200만
    private static final BigDecimal INSURANCE_CAP = new BigDecimal("1000000"); // 보장성보험 100만 한도
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.12");   // 보장성보험 12%
    private static final BigDecimal PENSION_CAP = new BigDecimal("9000000");   // 연금계좌 900만 한도
    private static final BigDecimal DONATION_BOUND = new BigDecimal("10000000"); // 기부금 1천만 경계
    private static final BigDecimal LOCAL_TAX_RATE = new BigDecimal("0.10");   // 지방소득세 10%

    public YearEndFullResponse calculateYearEndFull(YearEndFullRequest r) {
        BigDecimal salary = nz(r.totalSalary());

        // 1단계: 근로소득금액
        BigDecimal earnedIncomeDeduction = earnedIncomeDeduction(salary);
        BigDecimal earnedIncome = salary.subtract(earnedIncomeDeduction).max(BigDecimal.ZERO);

        // 2단계: 종합소득공제 → 과세표준
        BigDecimal personalDed = BASIC_PERSON.multiply(BigDecimal.valueOf(1L + nzInt(r.dependents())))
                .add(SENIOR_ADD.multiply(BigDecimal.valueOf(nzInt(r.seniors()))))
                .add(DISABLED_ADD.multiply(BigDecimal.valueOf(nzInt(r.disabled()))));
        BigDecimal pensionInsuranceDed = nz(r.nationalPension());
        BigDecimal specialIncomeDed = nz(r.healthInsurance()).add(nz(r.employmentInsurance()));
        BigDecimal cardDed = simulateYearEndSettlement(new YearEndSettlementRequest(
                salary, r.creditCardAmount(), r.debitCashAmount(),
                r.traditionalMarketAmount(), r.publicTransportAmount())).totalDeduction();

        BigDecimal totalIncomeDed = personalDed.add(pensionInsuranceDed).add(specialIncomeDed).add(cardDed);
        BigDecimal taxBase = earnedIncome.subtract(totalIncomeDed).max(BigDecimal.ZERO);

        // 3단계: 산출세액 (기본세율 누진)
        BigDecimal calculatedTax = progressiveTax(taxBase);

        // 4단계: 세액공제 → 결정세액
        BigDecimal earnedCredit = earnedIncomeTaxCredit(calculatedTax, salary);
        BigDecimal insuranceCredit = nz(r.insurancePremium()).min(INSURANCE_CAP)
                .multiply(INSURANCE_RATE).setScale(0, RoundingMode.FLOOR);
        BigDecimal medicalBase = nz(r.medicalExpense())
                .subtract(salary.multiply(MEDICAL_FLOOR_RATIO)).max(BigDecimal.ZERO);
        BigDecimal medicalCredit = medicalBase.multiply(RATE_CREDIT).setScale(0, RoundingMode.FLOOR);
        BigDecimal educationCredit = nz(r.educationExpense()).multiply(RATE_CREDIT).setScale(0, RoundingMode.FLOOR);
        BigDecimal donationCredit = donationCredit(nz(r.donation()));
        BigDecimal pensionAccountCredit = pensionAccountCredit(nz(r.pensionSavings()), salary);

        BigDecimal totalCredit = earnedCredit.add(insuranceCredit).add(medicalCredit)
                .add(educationCredit).add(donationCredit).add(pensionAccountCredit);

        BigDecimal determinedTax = calculatedTax.subtract(totalCredit).max(BigDecimal.ZERO);
        BigDecimal localTax = determinedTax.multiply(LOCAL_TAX_RATE).setScale(0, RoundingMode.FLOOR);

        // 5단계: 정산 (소득세 기준)
        BigDecimal prepaid = nz(r.prepaidTax());
        BigDecimal refundOrPay = prepaid.subtract(determinedTax);

        return new YearEndFullResponse(
                salary, earnedIncomeDeduction, earnedIncome,
                personalDed, pensionInsuranceDed, specialIncomeDed, cardDed, totalIncomeDed, taxBase,
                calculatedTax,
                earnedCredit, insuranceCredit, medicalCredit, educationCredit, donationCredit, pensionAccountCredit,
                totalCredit, determinedTax, localTax,
                prepaid, refundOrPay, buildFullGuide(determinedTax, refundOrPay)
        );
    }

    /** 근로소득공제 (총급여 구간별 누진, 한도 2천만원). */
    static BigDecimal earnedIncomeDeduction(BigDecimal s) {
        BigDecimal d;
        if (s.compareTo(bd(5_000_000)) <= 0) {
            d = s.multiply(new BigDecimal("0.70"));
        } else if (s.compareTo(bd(15_000_000)) <= 0) {
            d = bd(3_500_000).add(s.subtract(bd(5_000_000)).multiply(new BigDecimal("0.40")));
        } else if (s.compareTo(bd(45_000_000)) <= 0) {
            d = bd(7_500_000).add(s.subtract(bd(15_000_000)).multiply(new BigDecimal("0.15")));
        } else if (s.compareTo(bd(100_000_000)) <= 0) {
            d = bd(12_000_000).add(s.subtract(bd(45_000_000)).multiply(new BigDecimal("0.05")));
        } else {
            d = bd(14_750_000).add(s.subtract(bd(100_000_000)).multiply(new BigDecimal("0.02")));
        }
        return d.min(bd(20_000_000)).setScale(0, RoundingMode.FLOOR);
    }

    /** 종합소득 기본세율 (누진공제 방식, 2023년~). */
    static BigDecimal progressiveTax(BigDecimal base) {
        BigDecimal tax;
        if (base.compareTo(bd(14_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.06"));
        } else if (base.compareTo(bd(50_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.15")).subtract(bd(1_260_000));
        } else if (base.compareTo(bd(88_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.24")).subtract(bd(5_760_000));
        } else if (base.compareTo(bd(150_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.35")).subtract(bd(15_440_000));
        } else if (base.compareTo(bd(300_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.38")).subtract(bd(19_940_000));
        } else if (base.compareTo(bd(500_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.40")).subtract(bd(25_940_000));
        } else if (base.compareTo(bd(1_000_000_000)) <= 0) {
            tax = base.multiply(new BigDecimal("0.42")).subtract(bd(35_940_000));
        } else {
            tax = base.multiply(new BigDecimal("0.45")).subtract(bd(65_940_000));
        }
        return tax.max(BigDecimal.ZERO).setScale(0, RoundingMode.FLOOR);
    }

    /** 근로소득세액공제 — 산출세액 130만 이하 55%/초과 30%, 총급여별 한도. */
    static BigDecimal earnedIncomeTaxCredit(BigDecimal calculatedTax, BigDecimal salary) {
        BigDecimal boundary = bd(1_300_000);
        BigDecimal credit = calculatedTax.compareTo(boundary) <= 0
                ? calculatedTax.multiply(new BigDecimal("0.55"))
                : bd(715_000).add(calculatedTax.subtract(boundary).multiply(new BigDecimal("0.30")));

        BigDecimal limit;
        if (salary.compareTo(bd(33_000_000)) <= 0) {
            limit = bd(740_000);
        } else if (salary.compareTo(bd(70_000_000)) <= 0) {
            limit = bd(740_000).subtract(salary.subtract(bd(33_000_000)).multiply(new BigDecimal("0.008")))
                    .max(bd(660_000));
        } else if (salary.compareTo(bd(120_000_000)) <= 0) {
            limit = bd(660_000).subtract(salary.subtract(bd(70_000_000)).multiply(new BigDecimal("0.5")))
                    .max(bd(500_000));
        } else {
            limit = bd(500_000).subtract(salary.subtract(bd(120_000_000)).multiply(new BigDecimal("0.5")))
                    .max(bd(200_000));
        }
        return credit.min(limit).setScale(0, RoundingMode.FLOOR);
    }

    /** 기부금 세액공제 — 1천만원 이하 15%, 초과분 30%. */
    static BigDecimal donationCredit(BigDecimal donation) {
        BigDecimal credit = donation.compareTo(DONATION_BOUND) <= 0
                ? donation.multiply(RATE_CREDIT)
                : DONATION_BOUND.multiply(RATE_CREDIT)
                        .add(donation.subtract(DONATION_BOUND).multiply(new BigDecimal("0.30")));
        return credit.setScale(0, RoundingMode.FLOOR);
    }

    /** 연금계좌 세액공제 — 900만 한도, 총급여 5500만 이하 15%/초과 12%. */
    static BigDecimal pensionAccountCredit(BigDecimal pensionSavings, BigDecimal salary) {
        BigDecimal base = pensionSavings.min(PENSION_CAP);
        BigDecimal rate = salary.compareTo(bd(55_000_000)) <= 0 ? RATE_CREDIT : new BigDecimal("0.12");
        return base.multiply(rate).setScale(0, RoundingMode.FLOOR);
    }

    private String buildFullGuide(BigDecimal determinedTax, BigDecimal refundOrPay) {
        StringBuilder sb = new StringBuilder();
        if (refundOrPay.signum() >= 0) {
            sb.append(String.format("예상 환급액은 약 %s원입니다. ", formatWon(refundOrPay)));
        } else {
            sb.append(String.format("예상 추가납부액은 약 %s원입니다. ", formatWon(refundOrPay.abs())));
        }
        if (determinedTax.signum() == 0) {
            sb.append("결정세액이 0원이라 세액공제를 더 늘려도 환급이 늘지 않습니다.");
        } else {
            sb.append("연금저축·IRP(최대 900만원), 기부금 등 세액공제 항목을 늘리면 결정세액을 더 줄일 수 있습니다.");
        }
        sb.append(" ※ 자녀세액공제·월세·표준세액공제 등 일부 항목은 미반영된 간이 계산입니다.");
        return sb.toString();
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    private int nzInt(Integer v) {
        return v == null ? 0 : v;
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

    @Transactional(readOnly = true)
    public YearEndSettlementRequest computeYearEndFromTransactions(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<Transaction> txs = transactionRepository.findConfirmedWithJoinsBetween(start, end);

        BigDecimal salary = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        BigDecimal debitCash = BigDecimal.ZERO;
        BigDecimal market = BigDecimal.ZERO;
        BigDecimal transport = BigDecimal.ZERO;

        for (Transaction t : txs) {
            Category category = t.getCategory();
            if (category == null) continue;
            BigDecimal amount = nz(t.getAmount());

            if (category.getType() == TransactionType.INCOME && SALARY_CATEGORY_NAME.equals(category.getName())) {
                salary = salary.add(amount);
                continue;
            }

            if (category.getType() != TransactionType.EXPENSE) continue;

            // 신용/체크/현금 분류 (PaymentMethod + Card.type)
            PaymentMethod pm = t.getPaymentMethod();
            if (pm == PaymentMethod.CARD && t.getCard() != null && t.getCard().getType() == CardType.CREDIT) {
                credit = credit.add(amount);
            } else if (pm == PaymentMethod.CARD || pm == PaymentMethod.CASH) {
                // 체크카드 + 현금 + (카드 정보 없는 CARD 결제는 안전하게 체크/현금으로)
                debitCash = debitCash.add(amount);
            }

            // 전통시장/대중교통 추가 합산 (신용/체크와 중복 합산 — 단순화 정책)
            YearEndCategory yec = category.yearEndCategoryOrNone();
            if (yec == YearEndCategory.TRADITIONAL_MARKET) {
                market = market.add(amount);
            } else if (yec == YearEndCategory.PUBLIC_TRANSPORT) {
                transport = transport.add(amount);
            }
        }

        return new YearEndSettlementRequest(salary, credit, debitCash, market, transport);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String formatWon(BigDecimal v) {
        return String.format("%,d", v.setScale(0, RoundingMode.FLOOR).toBigInteger());
    }
}
