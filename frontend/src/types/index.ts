// Common
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER';
export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER';
export type AssetType = 'CASH' | 'SAVINGS' | 'STOCK' | 'DEBT';
export type CardType = 'CREDIT' | 'CHECK';

// Card
export interface Card {
    id: number;
    name: string;
    type: CardType;
}

export interface CardRequest {
    name: string;
    type: CardType;
}

// Category
export interface Category {
    id: number;
    name: string;
    type: TransactionType;
}

export interface CategoryRequest {
    name: string;
    type: TransactionType;
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
}

export interface YearEndSettlementResponse {
    minUsageThreshold: number;
    estimatedDeduction: number;
    guideMessage: string;
}

// MyStock (미국 주식)
export interface MyStock {
    id: number;
    ticker: string;
    companyName: string;
    purchasePrice: number;
    quantity: number;
    currentPrice?: number;
    valuation?: number;
    returnRate?: number;
    lastSyncedAt?: string;
}

export interface MyStockRequest {
    ticker: string;
    companyName: string;
    purchasePrice: number;
    quantity: number;
}

export interface SymbolSearchResult {
    symbol: string;
    name: string;
    type: string;
    region: string;
    currency: string;
}

export interface StockNewsItem {
    title: string;
    url: string;
    summary: string;
    sentiment: string;
}

export interface StockAnalysis {
    ticker: string;
    report: string;
    indicators: Record<string, string>;
    news: StockNewsItem[];
}

// Market Outlook (시장 전망)
export interface RssArticle {
    title: string;
    description: string;
    link: string;
    pubDate: string;
    category: string;
}

export interface MarketOutlookResponse {
    report: string;
    generatedAt: string;
    sources: RssArticle[];
}

// Aliases for Response types to match conventions
export type CategoryResponse = Category;
export type BudgetResponse = Budget;
export type TransactionResponse = Transaction;
export type AssetResponse = Asset;
export type CardResponse = Card;
export type RecurringTransactionResponse = RecurringTransaction;
