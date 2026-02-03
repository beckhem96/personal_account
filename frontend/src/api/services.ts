import api from './axios';
import type { 
    CategoryResponse, CategoryRequest,
    BudgetResponse, BudgetRequest,
    TransactionResponse, TransactionRequest,
    AssetResponse, AssetRequest, NetWorthResponse,
    TaxStockRequest, TaxStockResponse,
    YearEndSettlementRequest, YearEndSettlementResponse,
    Card, CardRequest,
    RecurringTransaction, RecurringTransactionRequest,
    PaymentMethod
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

// Cards
export const getCards = async () => {
    const response = await api.get<Card[]>('/cards');
    return response.data;
};

export const createCard = async (data: CardRequest) => {
    const response = await api.post<Card>('/cards', data);
    return response.data;
};

export const deleteCard = async (id: number) => {
    await api.delete(`/cards/${id}`);
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

export const deleteRecurringTransaction = async (id: number) => {
    await api.delete(`/recurring/${id}`);
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
