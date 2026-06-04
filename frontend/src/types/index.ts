// Common
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER';
export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER';
export type AssetType = 'CASH' | 'SAVINGS' | 'STOCK' | 'DEBT';
export type CardType = 'CREDIT' | 'CHECK';
export type CardCompany = 'HANA' | 'SAMSUNG' | 'HYUNDAI' | 'SHINHAN' | 'KB';

// Card
export interface Card {
    id: number;
    name: string;
    type: CardType;
    company?: CardCompany | null;
}

export interface CardRequest {
    name: string;
    type: CardType;
    company?: CardCompany | null;
}

// Statement import
export interface SupportedCardCompany {
    code: CardCompany;
    displayName: string;
}

export interface ImportedItem {
    date: string;
    merchant: string;
    amount: number;
    categoryName: string;
    installmentSeq?: number | null;
    installmentMonths?: number | null;
}

export interface StatementImportResponse {
    imported: number;
    skipped: number;
    failed: number;
    unclassified: number;
    summary: ImportedItem[];
}

// Category
export type YearEndCategory = 'NONE' | 'TRADITIONAL_MARKET' | 'PUBLIC_TRANSPORT';

export interface Category {
    id: number;
    name: string;
    type: TransactionType;
    yearEndCategory?: YearEndCategory;
}

export interface CategoryRequest {
    name: string;
    type: TransactionType;
    yearEndCategory?: YearEndCategory | null;
}

// Budget
export interface Budget {
    id: number;
    year: number;
    month: number;
    amount: number;
    categoryName: string;
    categoryId: number;
}

export interface BudgetRequest {
    year: number;
    month: number;
    categoryId: number;
    amount: number;
}

// Transaction
export interface Transaction {
    id: number;
    date: string; // ISO Date string
    amount: number;
    memo: string;
    paymentMethod: PaymentMethod;
    categoryName: string;
    categoryId: number;
    isConfirmed: boolean;
    cardName?: string;
    cardId?: number;
    assetId?: number;
    assetName?: string;
    toAssetId?: number;
    toAssetName?: string;
}

export interface TransactionRequest {
    date: string;
    amount: number;
    memo: string;
    paymentMethod: PaymentMethod;
    categoryId: number;
    isConfirmed: boolean;
    cardId?: number;
    assetId?: number;
    toAssetId?: number;
}

// Recurring Transaction
export interface RecurringTransaction {
    id: number;
    name: string;
    amount: number;
    dayOfMonth: number;
    paymentMethod: PaymentMethod;
    categoryName: string;
    categoryId: number;
    cardName?: string;
    cardId?: number;
    assetId?: number;
    assetName?: string;
    toAssetId?: number;
    toAssetName?: string;
    startDate?: string;
    endDate?: string;
}

export interface RecurringTransactionRequest {
    name: string;
    amount: number;
    dayOfMonth: number;
    paymentMethod: PaymentMethod;
    categoryId: number;
    cardId?: number;
    assetId?: number;
    toAssetId?: number;
    startDate?: string;
    endDate?: string;
}

export interface ApplyRecurringResponse {
    appliedCount: number;
    deletedCount: number;
}

// Asset
export interface Asset {
    id: number;
    type: AssetType;
    name: string;
    balance: number;
    purchasePrice?: number;
    returnRate?: number;
    isDefault: boolean;
}

export interface AssetRequest {
    type: AssetType;
    name: string;
    balance: number;
    purchasePrice?: number;
}

export interface NetWorthResponse {
    totalAssets: number;
    totalLiabilities: number;
    netWorth: number;
    assetsByType: Record<string, number>;
}

// Tax
export interface TaxStockRequest {
    totalSellAmount: number;
    totalBuyAmount: number;
}

export interface TaxStockResponse {
    profit: number;
    deduction: number;
    taxBase: number;
    estimatedTax: number;
}

export interface YearEndSettlementRequest {
    totalSalary: number;
    creditCardAmount: number;
    debitCashAmount: number;
    traditionalMarketAmount: number;
    publicTransportAmount: number;
}

export interface YearEndSettlementResponse {
    minUsageThreshold: number;
    creditDeduction: number;
    debitDeduction: number;
    marketDeduction: number;
    transportDeduction: number;
    generalLimit: number;
    totalDeduction: number;
    guideMessage: string;
}

// Aliases for API responses (same as entity types)
export type CategoryResponse = Category;
export type BudgetResponse = Budget;
export type TransactionResponse = Transaction;
export type AssetResponse = Asset;

// Housing
export type HouseCount = 'SINGLE' | 'TWO' | 'THREE_OR_MORE';
export type RepaymentType = 'PRINCIPAL_INTEREST' | 'PRINCIPAL_ONLY' | 'BULLET';
export type LoanProductCode = 'DIDIMDOL' | 'BOGEUMJARI' | 'GENERAL';

export interface AcquisitionCostRequest {
    salePrice: number;
    houseCount: HouseCount;
    isRegulatedArea: boolean;
    isFirstTime: boolean;
    exclusiveAreaSqm: number;
    standardMarketPrice?: number | null;
    bondDiscountRate?: number | null;
}

export interface AcquisitionCostResponse {
    acquisitionTax: number;
    localEducationTax: number;
    ruralSpecialTax: number;
    brokerFee: number;
    judicialFee: number;
    stampDuty: number;
    nationalHousingBondLoss: number;
    totalCost: number;
    breakdown: Record<string, number>;
}

export interface LoanCostRequest {
    loanAmount: number;
    includeAppraisalFee: boolean;
}

export interface LoanCostResponse {
    mortgageRegistrationCost: number;
    stampDuty: number;
    appraisalFee: number;
    totalCost: number;
    breakdown: Record<string, number>;
}

export interface LoanRepaymentRequest {
    principal: number;
    annualRatePercent: number;
    termMonths: number;
    repaymentType: RepaymentType;
    gracePeriodMonths: number;
    productCode?: LoanProductCode | null;
}

export interface LoanScheduleRow {
    month: number;
    payment: number;
    principal: number;
    interest: number;
    remainingBalance: number;
}

export interface LoanRepaymentResponse {
    firstMonthPayment: number;
    lastMonthPayment: number;
    totalInterest: number;
    totalPayment: number;
    schedule: LoanScheduleRow[];
}

export interface LoanProductInfo {
    code: LoanProductCode;
    name: string;
    description: string;
    maxLoanAmount: number;
    referenceRate: number;
    eligibility: string[];
}

export interface ApartmentDealDto {
    apartmentName: string;
    dealAmount: number;
    exclusiveArea: number;
    floor: number;
    buildYear: number;
    dealDate: string | null;
    dong: string;
    lawdCd: string;
}

export interface ApartmentDealsResponse {
    averagePrice: number;
    averagePricePerSqm: number;
    totalDeals: number;
    filteredDeals: number;
    deals: ApartmentDealDto[];
}

export interface RegionDistrict {
    code: string;
    name: string;
    parentName: string;
}

export interface RegionTree {
    region: 'SEOUL' | 'GYEONGGI' | 'INCHEON';
    regionLabel: string;
    districts: RegionDistrict[];
}

// Subscriptions (청약 일정)
export type SubscriptionRank = 'FIRST' | 'SECOND' | 'REMAINDER';

export interface SubscriptionItem {
    houseManageNo: string | null;
    name: string;
    houseType: string | null;
    regionLabel: string | null;
    address: string | null;
    totalSupplyHouseholds: number | null;
    noticeDate: string | null;
    firstRcptBegin: string | null;
    firstRcptEnd: string | null;
    secondRcptBegin: string | null;
    secondRcptEnd: string | null;
    remainderRcptBegin: string | null;
    remainderRcptEnd: string | null;
    activeStages: SubscriptionRank[];
    applyhomeUrl: string | null;
}

export interface SubscriptionsResponse {
    asOf: string;
    apiKeyConfigured: boolean;
    firstRank: SubscriptionItem[];
    secondRank: SubscriptionItem[];
    remainder: SubscriptionItem[];
}
