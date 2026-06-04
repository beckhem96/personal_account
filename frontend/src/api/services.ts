import api from './axios';
import type {
    CategoryResponse, CategoryRequest,
    BudgetResponse, BudgetRequest,
    TransactionResponse, TransactionRequest,
    AssetResponse, AssetRequest, NetWorthResponse,
    TaxStockRequest, TaxStockResponse,
    YearEndSettlementRequest, YearEndSettlementResponse,
    YearEndFullRequest, YearEndFullResponse,
    Card, CardRequest,
    RecurringTransaction, RecurringTransactionRequest,
    ApplyRecurringResponse,
    PaymentMethod,
    MyStock, MyStockRequest, SymbolSearchResult, StockAnalysis,
    MarketOutlookResponse,
    AcquisitionCostRequest, AcquisitionCostResponse,
    LoanCostRequest, LoanCostResponse,
    LoanRepaymentRequest, LoanRepaymentResponse,
    LoanProductInfo,
    ApartmentDealsResponse, RegionTree,
    SupportedCardCompany, StatementImportResponse,
    SubscriptionsResponse, LhNoticesResponse
} from '../types';

// Categories
export const getCategories = async () => {
    const response = await api.get<CategoryResponse[]>('/categories');
    return response.data;
};

export const createCategory = async (data: CategoryRequest) => {
    const response = await api.post<CategoryResponse>('/categories', data);
    return response.data;
};

export const updateCategory = async (id: number, data: CategoryRequest) => {
    const response = await api.put<CategoryResponse>(`/categories/${id}`, data);
    return response.data;
};

export const deleteCategory = async (id: number) => {
    await api.delete(`/categories/${id}`);
};

// Cards
export const getCards = async () => {
    const response = await api.get<Card[]>('/cards');
    return response.data;
};

export const createCard = async (data: CardRequest) => {
    const response = await api.post<Card>('/cards', data);
    return response.data;
};

export const updateCard = async (id: number, data: CardRequest) => {
    const response = await api.put<Card>(`/cards/${id}`, data);
    return response.data;
};

export const deleteCard = async (id: number) => {
    await api.delete(`/cards/${id}`);
};

// Statements
export const getSupportedCardCompanies = async () => {
    const response = await api.get<SupportedCardCompany[]>('/statements/supported');
    return response.data;
};

export const importStatement = async (cardId: number, file: File) => {
    const formData = new FormData();
    formData.append('cardId', String(cardId));
    formData.append('file', file);
    const response = await api.post<StatementImportResponse>('/statements/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
};

// Recurring Transactions
export const getRecurringTransactions = async () => {
    const response = await api.get<RecurringTransaction[]>('/recurring');
    return response.data;
};

export const createRecurringTransaction = async (data: RecurringTransactionRequest) => {
    const response = await api.post<RecurringTransaction>('/recurring', data);
    return response.data;
};

export const updateRecurringTransaction = async (id: number, data: RecurringTransactionRequest) => {
    const response = await api.put<RecurringTransaction>(`/recurring/${id}`, data);
    return response.data;
};

export const deleteRecurringTransaction = async (id: number) => {
    await api.delete(`/recurring/${id}`);
};

export const applyRecurringTransactions = async () => {
    const response = await api.post<ApplyRecurringResponse>('/recurring/apply');
    return response.data;
};

export const applySingleRecurringTransaction = async (id: number) => {
    const response = await api.post<ApplyRecurringResponse>(`/recurring/${id}/apply`);
    return response.data;
};

// Budgets
export const getBudgets = async (params: { year?: number, month?: number, startDate?: string, endDate?: string }) => {
    const response = await api.get<BudgetResponse[]>('/budgets', { params });
    return response.data;
};

export const setBudget = async (data: BudgetRequest) => {
    const response = await api.post<BudgetResponse>('/budgets', data);
    return response.data;
};

// Transactions
export const getTransactions = async (startDate: string, endDate: string, paymentMethod?: PaymentMethod) => {
    const params: any = { startDate, endDate };
    if (paymentMethod) params.paymentMethod = paymentMethod;
    const response = await api.get<TransactionResponse[]>('/transactions', { params });
    return response.data;
};

export const createTransaction = async (data: TransactionRequest) => {
    const response = await api.post<TransactionResponse>('/transactions', data);
    return response.data;
};

export const updateTransaction = async (id: number, data: TransactionRequest) => {
    const response = await api.put<TransactionResponse>(`/transactions/${id}`, data);
    return response.data;
};

export const deleteTransaction = async (id: number) => {
    await api.delete(`/transactions/${id}`);
};

export const confirmTransaction = async (id: number) => {
    const response = await api.patch<TransactionResponse>(`/transactions/${id}/confirm`);
    return response.data;
};

export const getPlannedTransactions = async () => {
    const response = await api.get<TransactionResponse[]>('/transactions/planned');
    return response.data;
};

export const getTransactionsByCard = async (cardId: number, startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    const response = await api.get<TransactionResponse[]>(`/transactions/by-card/${cardId}`, { params });
    return response.data;
};

// Assets
export const getAssets = async () => {
    const response = await api.get<AssetResponse[]>('/assets');
    return response.data;
};

export const createAsset = async (data: AssetRequest) => {
    const response = await api.post<AssetResponse>('/assets', data);
    return response.data;
};

export const updateAsset = async (id: number, data: AssetRequest) => {
    const response = await api.put<AssetResponse>(`/assets/${id}`, data);
    return response.data;
};

export const deleteAsset = async (id: number) => {
    await api.delete(`/assets/${id}`);
};

export const setDefaultAsset = async (id: number) => {
    const response = await api.patch<AssetResponse>(`/assets/${id}/set-default`);
    return response.data;
};

export const getNetWorth = async () => {
    const response = await api.get<NetWorthResponse>('/assets/net-worth');
    return response.data;
};

// Tax
export const calculateStockTax = async (data: TaxStockRequest) => {
    const response = await api.post<TaxStockResponse>('/tax/stock', data);
    return response.data;
};

export const simulateYearEnd = async (data: YearEndSettlementRequest) => {
    const response = await api.post<YearEndSettlementResponse>('/tax/year-end', data);
    return response.data;
};

export const getAutoYearEndSettlement = async (year: number) => {
    const response = await api.get<YearEndSettlementRequest>('/tax/year-end/auto', { params: { year } });
    return response.data;
};

export const calculateYearEndFull = async (data: YearEndFullRequest) => {
    const response = await api.post<YearEndFullResponse>('/tax/year-end/full', data);
    return response.data;
};

// My Stocks (미국 주식)
export const getMyStocks = async () => {
    const response = await api.get<MyStock[]>('/stocks');
    return response.data;
};

export const addMyStock = async (data: MyStockRequest) => {
    const response = await api.post<MyStock>('/stocks', data);
    return response.data;
};

export const updateMyStock = async (id: number, data: MyStockRequest) => {
    const response = await api.put<MyStock>(`/stocks/${id}`, data);
    return response.data;
};

export const deleteMyStock = async (id: number) => {
    await api.delete(`/stocks/${id}`);
};

export const searchSymbol = async (keywords: string) => {
    const response = await api.get<SymbolSearchResult[]>('/stocks/search', { params: { keywords } });
    return response.data;
};

export const syncStockPrice = async (id: number) => {
    const response = await api.post<MyStock>(`/stocks/${id}/sync`);
    return response.data;
};

export const syncAllStockPrices = async () => {
    const response = await api.post<MyStock[]>('/stocks/sync-all');
    return response.data;
};

export const analyzeStock = async (id: number) => {
    const response = await api.post<StockAnalysis>(`/stocks/${id}/analyze`);
    return response.data;
};

export const getMarketOutlook = async () => {
    const response = await api.get<MarketOutlookResponse>('/stocks/market-outlook');
    return response.data;
};

// Housing
export const calculateAcquisitionCost = async (data: AcquisitionCostRequest) => {
    const response = await api.post<AcquisitionCostResponse>('/housing/acquisition-cost', data);
    return response.data;
};

export const calculateLoanCost = async (data: LoanCostRequest) => {
    const response = await api.post<LoanCostResponse>('/housing/loan-cost', data);
    return response.data;
};

export const calculateLoanRepayment = async (data: LoanRepaymentRequest) => {
    const response = await api.post<LoanRepaymentResponse>('/housing/loan-repayment', data);
    return response.data;
};

export const getLoanProducts = async () => {
    const response = await api.get<LoanProductInfo[]>('/housing/loan-products');
    return response.data;
};

export const getHousingRegions = async () => {
    const response = await api.get<RegionTree[]>('/housing/regions');
    return response.data;
};

export const getApartmentDeals = async (params: {
    lawdCd: string;
    dealYearMonth: string;
    minPrice?: number;
    maxPrice?: number;
    minArea?: number;
}) => {
    const response = await api.get<ApartmentDealsResponse>('/housing/apartment-deals', { params });
    return response.data;
};

// Subscriptions (청약 일정)
export const getTodaySubscriptions = async () => {
    const response = await api.get<SubscriptionsResponse>('/subscriptions/today');
    return response.data;
};

// LH 공공분양·임대 청약
export const getLhSubscriptions = async () => {
    const response = await api.get<LhNoticesResponse>('/subscriptions/lh/today');
    return response.data;
};
