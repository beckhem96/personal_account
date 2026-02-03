import React, { useState, useEffect } from 'react';
import {
    getBudgets, getCategories, getTransactions, createTransaction, updateTransaction, deleteTransaction, setBudget, createCategory,
    getCards, getRecurringTransactions, createRecurringTransaction, deleteRecurringTransaction, getTransactionsByCard
} from '../api/services';
import type { 
    BudgetResponse, CategoryResponse, TransactionResponse, TransactionType, PaymentMethod,
    Card, RecurringTransaction, RecurringTransactionRequest, TransactionRequest
} from '../types';
import { formatCurrency, cn } from '../utils';
import { format, startOfMonth, endOfMonth } from 'date-fns';
import { Plus, Settings, Calendar, CreditCard, Banknote, Landmark, X, ChevronLeft, ChevronRight, Repeat, Trash2, Edit2, Wallet, ArrowUpRight, ArrowDownLeft } from 'lucide-react';

const BudgetPage = () => {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [cards, setCards] = useState<Card[]>([]);
    const [recurringTxs, setRecurringTxs] = useState<RecurringTransaction[]>([]);
    
    // UI State
    const [isTxFormOpen, setIsTxFormOpen] = useState(false);
    const [editingTx, setEditingTx] = useState<TransactionResponse | null>(null);
    
    // Filter State: 'ALL' | 'CASH' | 'BANK_TRANSFER' | 'card_{id}'
    const [filterType, setFilterType] = useState<string>('ALL');
    
    const [filterStartDate, setFilterStartDate] = useState<string>(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
    const [filterEndDate, setFilterEndDate] = useState<string>(format(endOfMonth(new Date()), 'yyyy-MM-dd'));
    const [useCustomDateRange, setUseCustomDateRange] = useState(false);

    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
    const [isRecurringModalOpen, setIsRecurringModalOpen] = useState(false);
    
    // Forms
    const [txForm, setTxForm] = useState<TransactionRequest>({
        amount: 0,
        memo: '',
        categoryId: 0,
        paymentMethod: 'CARD',
        date: format(new Date(), 'yyyy-MM-dd'),
        isConfirmed: true,
        cardId: undefined
    });

    const [newCategory, setNewCategory] = useState({ name: '', type: 'EXPENSE' as TransactionType });

    const [newRecurring, setNewRecurring] = useState<RecurringTransactionRequest>({
        name: '',
        amount: 0,
        dayOfMonth: 1,
        paymentMethod: 'CARD',
        categoryId: 0,
        cardId: undefined
    });

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;

    useEffect(() => {
        fetchData();
    }, [currentDate, filterType, filterStartDate, filterEndDate, useCustomDateRange]);

    // 월 변경 시 커스텀 날짜 범위 초기화
    useEffect(() => {
        if (!useCustomDateRange) {
            setFilterStartDate(format(startOfMonth(currentDate), 'yyyy-MM-dd'));
            setFilterEndDate(format(endOfMonth(currentDate), 'yyyy-MM-dd'));
        }
    }, [currentDate, useCustomDateRange]);

    const fetchData = async () => {
        try {
            const start = useCustomDateRange ? filterStartDate : format(startOfMonth(currentDate), 'yyyy-MM-dd');
            const end = useCustomDateRange ? filterEndDate : format(endOfMonth(currentDate), 'yyyy-MM-dd');

            const [rawBudgets, cData, cardData, rData] = await Promise.all([
                getBudgets(useCustomDateRange ? { startDate: start, endDate: end } : { year, month }),
                getCategories(),
                getCards(),
                getRecurringTransactions()
            ]);

            // Aggregate budgets by category if custom range is used
            const bData = useCustomDateRange
                ? Object.values(rawBudgets.reduce((acc, curr) => {
                    if (!acc[curr.categoryId]) {
                        acc[curr.categoryId] = { ...curr };
                    } else {
                        acc[curr.categoryId].amount += curr.amount;
                    }
                    return acc;
                }, {} as Record<number, BudgetResponse>))
                : rawBudgets;

            let tData: TransactionResponse[] = [];
            
            if (filterType === 'ALL') {
                tData = await getTransactions(start, end);
            } else if (filterType === 'CASH') {
                tData = await getTransactions(start, end, 'CASH');
            } else if (filterType === 'BANK_TRANSFER') {
                tData = await getTransactions(start, end, 'BANK_TRANSFER');
            } else if (filterType.startsWith('card_')) {
                const cardId = Number(filterType.split('_')[1]);
                tData = await getTransactionsByCard(cardId, start, end);
            }

            setBudgets(bData);
            setCategories(cData);
            setTransactions(tData);
            setCards(cardData);
            setRecurringTxs(rData);
        } catch (error) {
            console.error(error);
        }
    };

    const handleMonthChange = (offset: number) => {
        const newDate = new Date(currentDate);
        newDate.setMonth(newDate.getMonth() + offset);
        setCurrentDate(newDate);
    };

    const handleTxSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!txForm.categoryId) return alert("Please select a category.");
        try {
            const payload = {
                ...txForm,
                amount: Number(txForm.amount),
                categoryId: Number(txForm.categoryId),
                cardId: txForm.paymentMethod === 'CARD' && txForm.cardId ? Number(txForm.cardId) : undefined
            };

            if (editingTx) {
                await updateTransaction(editingTx.id, payload);
            } else {
                await createTransaction(payload);
            }
            
            setIsTxFormOpen(false);
            setEditingTx(null);
            resetTxForm();
            fetchData();
        } catch (error) {
            alert('Failed to save transaction');
        }
    };

    const handleEditTx = (tx: TransactionResponse) => {
        setEditingTx(tx);
        setTxForm({
            date: tx.date,
            amount: tx.amount,
            memo: tx.memo,
            paymentMethod: tx.paymentMethod,
            categoryId: tx.categoryId,
            isConfirmed: tx.isConfirmed,
            cardId: tx.cardId
        });
        setIsTxFormOpen(true);
    };

    const handleDeleteTx = async (id: number) => {
        if (!confirm('Are you sure you want to delete this transaction?')) return;
        try {
            await deleteTransaction(id);
            fetchData();
        } catch (error) {
            alert('Failed to delete transaction');
        }
    };

    const resetTxForm = () => {
        setTxForm({
            amount: 0,
            memo: '',
            categoryId: 0,
            paymentMethod: 'CARD',
            date: format(new Date(), 'yyyy-MM-dd'),
            isConfirmed: true,
            cardId: undefined
        });
    };

    const handleBudgetUpdate = async (categoryId: number, amount: number) => {
        try {
            await setBudget({ year, month, categoryId, amount });
            fetchData();
        } catch (error) {
            alert('Failed to update budget');
        }
    };

    const handleCreateCategory = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await createCategory(newCategory);
            setIsCategoryModalOpen(false);
            setNewCategory({ name: '', type: 'EXPENSE' });
            fetchData();
        } catch (error) {
            alert('Failed to create category');
        }
    };

    const handleCreateRecurring = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await createRecurringTransaction({
                ...newRecurring,
                cardId: newRecurring.paymentMethod === 'CARD' ? newRecurring.cardId : undefined
            });
            setIsRecurringModalOpen(false);
            fetchData();
        } catch (error) {
            alert('Failed to create recurring transaction');
        }
    };

    const handleDeleteRecurring = async (id: number) => {
        if(!confirm('Delete this recurring expense?')) return;
        try {
            await deleteRecurringTransaction(id);
            fetchData();
        } catch (error) {
            alert('Failed to delete');
        }
    };

    const getCategoryStats = (categoryName: string) => {
        const budget = budgets.find(b => b.categoryName === categoryName);
        const actual = transactions
            .filter(t => t.categoryName === categoryName)
            .reduce((sum, t) => sum + Number(t.amount), 0);
        
        return {
            budgetAmount: budget?.amount || 0,
            actualAmount: actual,
            percent: budget && budget.amount > 0 ? (actual / budget.amount) * 100 : 0
        };
    };

    // Calculate Totals based on Transaction Type
    const totalIncome = transactions
        .filter(t => {
            const cat = categories.find(c => c.name === t.categoryName);
            return cat?.type === 'INCOME';
        })
        .reduce((sum, t) => sum + Number(t.amount), 0);

    const totalExpense = transactions
        .filter(t => {
            const cat = categories.find(c => c.name === t.categoryName);
            return cat?.type === 'EXPENSE';
        })
        .reduce((sum, t) => sum + Number(t.amount), 0);

    // Filtered Budget Total (Only Expenses)
    const totalExpenseBudget = budgets
        .filter(b => {
             const cat = categories.find(c => c.name === b.categoryName);
             return cat?.type === 'EXPENSE';
        })
        .reduce((sum, b) => sum + b.amount, 0);

    return (
        <div className="space-y-8">
            {/* Header */}
            <div className="flex flex-col md:flex-row items-center justify-between gap-6">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Budget & Ledger</h1>
                    <p className="text-slate-500 mt-1">Track your income, expenses, and fixed costs.</p>
                </div>
                
                <div className="flex items-center bg-white p-1.5 rounded-2xl shadow-sm border border-slate-200">
                    <button onClick={() => handleMonthChange(-1)} className="p-2 hover:bg-slate-100 rounded-xl transition-colors text-slate-600">
                        <ChevronLeft size={20} />
                    </button>
                    <div className="flex items-center space-x-2 px-6">
                        <Calendar size={18} className="text-blue-500" />
                        <span className="text-lg font-bold text-slate-800 min-w-[100px] text-center">
                            {format(currentDate, 'MMMM yyyy')}
                        </span>
                    </div>
                    <button onClick={() => handleMonthChange(1)} className="p-2 hover:bg-slate-100 rounded-xl transition-colors text-slate-600">
                        <ChevronRight size={20} />
                    </button>
                </div>
            </div>

            {/* Overview Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                 <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <p className="text-sm font-medium text-slate-500 mb-1">Total Income</p>
                    <p className="text-2xl font-bold text-green-600">{formatCurrency(totalIncome)}</p>
                </div>
                <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <p className="text-sm font-medium text-slate-500 mb-1">Total Budget (Exp)</p>
                    <p className="text-2xl font-bold text-slate-900">{formatCurrency(totalExpenseBudget)}</p>
                </div>
                <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <p className="text-sm font-medium text-slate-500 mb-1">Total Spent</p>
                    <p className="text-2xl font-bold text-slate-900">{formatCurrency(totalExpense)}</p>
                </div>
                <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <p className="text-sm font-medium text-slate-500 mb-1">Net Profit</p>
                    <p className={cn("text-2xl font-bold", totalIncome - totalExpense >= 0 ? "text-blue-600" : "text-red-500")}>
                        {formatCurrency(totalIncome - totalExpense)}
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 1. Status Column (Left) */}
                <div className="lg:col-span-1 space-y-8">
                    
                    {/* Income Status */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 flex flex-col h-fit">
                        <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <ArrowUpRight size={18} className="text-green-500"/>
                                Income Status
                            </h2>
                             <button 
                                onClick={() => setIsCategoryModalOpen(true)}
                                className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                title="Manage Categories"
                            >
                                <Settings size={18} />
                            </button>
                        </div>
                        
                        <div className="p-6 space-y-6">
                            {categories.filter(c => c.type === 'INCOME').map(category => {
                                const stats = getCategoryStats(category.name);
                                const percent = stats.percent;

                                return (
                                    <div key={category.id} className="group">
                                        <div className="flex justify-between items-end mb-2">
                                            <span className="font-semibold text-slate-700 text-sm">{category.name}</span>
                                            <div className="flex items-center space-x-1 text-sm">
                                                <input 
                                                    type="number" 
                                                    defaultValue={stats.budgetAmount || 0}
                                                    disabled={useCustomDateRange}
                                                    onBlur={(e) => handleBudgetUpdate(category.id, Number(e.target.value))}
                                                    className={cn(
                                                        "w-20 text-right font-medium text-slate-900 border-b border-dashed border-slate-300 focus:outline-none bg-transparent transition-colors py-0.5",
                                                        useCustomDateRange ? "opacity-50 cursor-not-allowed border-transparent" : "hover:border-blue-500 focus:border-blue-500"
                                                    )}
                                                />
                                                <span className="text-slate-400 font-light">/ {formatCurrency(stats.actualAmount)}</span>
                                            </div>
                                        </div>
                                        {/* Progress Bar for Income */}
                                        <div className="relative w-full bg-slate-100 rounded-full h-3 overflow-hidden">
                                            <div 
                                                className={cn(
                                                    "h-full rounded-full transition-all duration-500 ease-out", 
                                                    percent >= 100 ? "bg-green-500" : "bg-blue-400"
                                                )}
                                                style={{ width: `${Math.min(percent, 100)}%` }}
                                            ></div>
                                        </div>
                                    </div>
                                );
                            })}
                            {categories.filter(c => c.type === 'INCOME').length === 0 && (
                                <p className="text-center text-slate-400 text-sm">No income categories found.</p>
                            )}
                        </div>
                    </div>

                    {/* Expense Budget Status */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 flex flex-col h-fit">
                        <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <ArrowDownLeft size={18} className="text-red-500"/>
                                Expense Budget
                            </h2>
                        </div>
                        
                        <div className="p-6 space-y-6">
                            {categories.filter(c => c.type === 'EXPENSE').map(category => {
                                const stats = getCategoryStats(category.name);
                                const isOverBudget = stats.percent > 100;

                                return (
                                    <div key={category.id} className="group">
                                        <div className="flex justify-between items-end mb-2">
                                            <span className="font-semibold text-slate-700 text-sm">{category.name}</span>
                                            <div className="flex items-center space-x-1 text-sm">
                                                <input 
                                                    type="number" 
                                                    defaultValue={stats.budgetAmount || 0}
                                                    disabled={useCustomDateRange}
                                                    onBlur={(e) => handleBudgetUpdate(category.id, Number(e.target.value))}
                                                    className={cn(
                                                        "w-20 text-right font-medium text-slate-900 border-b border-dashed border-slate-300 focus:outline-none bg-transparent transition-colors py-0.5",
                                                        useCustomDateRange ? "opacity-50 cursor-not-allowed border-transparent" : "hover:border-blue-500 focus:border-blue-500"
                                                    )}
                                                />
                                                <span className="text-slate-400 font-light">/ {formatCurrency(stats.actualAmount)}</span>
                                            </div>
                                        </div>
                                        {/* Progress Bar for Expense */}
                                        <div className="relative w-full bg-slate-100 rounded-full h-3 overflow-hidden">
                                            <div 
                                                className={cn(
                                                    "h-full rounded-full transition-all duration-500 ease-out", 
                                                    isOverBudget ? "bg-red-500" : "bg-blue-500"
                                                )}
                                                style={{ width: `${Math.min(stats.percent, 100)}%` }}
                                            ></div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* Recurring Expenses */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 flex flex-col h-fit">
                        <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <Repeat size={18} className="text-purple-500"/>
                                Fixed Costs
                            </h2>
                            <button 
                                onClick={() => setIsRecurringModalOpen(true)}
                                className="text-xs font-semibold bg-slate-900 text-white px-3 py-1.5 rounded-lg hover:bg-slate-800"
                            >
                                + Add
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            {recurringTxs.map(rt => (
                                <div key={rt.id} className="flex justify-between items-center p-3 bg-slate-50 rounded-xl group hover:bg-slate-100 transition-colors">
                                    <div>
                                        <p className="font-semibold text-slate-800 text-sm">{rt.name}</p>
                                        <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5">
                                            <span className="bg-white px-1.5 py-0.5 rounded border border-slate-200">Day {rt.dayOfMonth}</span>
                                            <span>{rt.categoryName}</span>
                                            {rt.cardName && <span>· {rt.cardName}</span>}
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-bold text-slate-900 text-sm">{formatCurrency(rt.amount)}</p>
                                        <button 
                                            onClick={() => handleDeleteRecurring(rt.id)}
                                            className="text-slate-300 hover:text-red-500 text-xs mt-1 opacity-0 group-hover:opacity-100 transition-opacity"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {recurringTxs.length === 0 && (
                                <p className="text-center text-slate-400 text-sm py-4">No recurring expenses.</p>
                            )}
                        </div>
                    </div>
                </div>

                {/* 2. Transaction List (Right Column) */}
                <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-100 flex flex-col min-h-[600px]">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 rounded-t-2xl space-y-4">
                        <div className="flex justify-between items-center">
                            <h2 className="text-lg font-bold text-slate-800">Transactions</h2>
                            <button
                                onClick={() => {
                                    setEditingTx(null);
                                    resetTxForm();
                                    setIsTxFormOpen(!isTxFormOpen);
                                }}
                                className="bg-slate-900 text-white px-5 py-2 rounded-xl flex items-center text-sm font-medium hover:bg-slate-800 transition shadow-lg shadow-slate-900/10"
                            >
                                <Plus size={16} className="mr-2"/> Add Transaction
                            </button>
                        </div>

                        {/* Filters Row */}
                        <div className="flex flex-wrap items-center gap-4">
                            {/* Account/Method Filter */}
                            <div className="flex items-center gap-2">
                                <Wallet size={16} className="text-slate-400" />
                                <select
                                    value={filterType}
                                    onChange={(e) => setFilterType(e.target.value)}
                                    className="text-sm border border-slate-200 rounded-lg px-3 py-1.5 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white min-w-[140px]"
                                >
                                    <option value="ALL">All Transactions</option>
                                    <option value="CASH">Cash Only</option>
                                    <option value="BANK_TRANSFER">Transfer Only</option>
                                    <optgroup label="Cards">
                                        {cards.map(card => (
                                            <option key={card.id} value={`card_${card.id}`}>{card.name}</option>
                                        ))}
                                    </optgroup>
                                </select>
                            </div>

                            <div className="w-px h-6 bg-slate-200" />

                            {/* Date Range Filter */}
                            <div className="flex items-center gap-2">
                                <Calendar size={16} className="text-slate-400" />
                                <label className="flex items-center gap-2 text-sm text-slate-600">
                                    <input
                                        type="checkbox"
                                        checked={useCustomDateRange}
                                        onChange={(e) => setUseCustomDateRange(e.target.checked)}
                                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    Custom Range
                                </label>
                            </div>

                            {useCustomDateRange && (
                                <div className="flex items-center gap-2">
                                    <input
                                        type="date"
                                        value={filterStartDate}
                                        onChange={(e) => setFilterStartDate(e.target.value)}
                                        className="text-sm border border-slate-200 rounded-lg px-3 py-1.5 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    />
                                    <span className="text-slate-400">~</span>
                                    <input
                                        type="date"
                                        value={filterEndDate}
                                        onChange={(e) => setFilterEndDate(e.target.value)}
                                        className="text-sm border border-slate-200 rounded-lg px-3 py-1.5 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    />
                                </div>
                            )}

                            {/* Clear Filters */}
                            {(filterType !== 'ALL' || useCustomDateRange) && (
                                <button
                                    onClick={() => {
                                        setFilterType('ALL');
                                        setUseCustomDateRange(false);
                                    }}
                                    className="text-xs text-slate-400 hover:text-red-500 flex items-center gap-1"
                                >
                                    <X size={14} />
                                    Clear Filters
                                </button>
                            )}
                        </div>

                        {/* Active Filter Info */}
                        {(filterType !== 'ALL' || useCustomDateRange) && (
                            <div className="text-xs text-slate-500 bg-blue-50 px-3 py-2 rounded-lg">
                                Showing: {filterType === 'ALL' ? 'All' : 
                                         filterType === 'CASH' ? 'Cash Only' :
                                         filterType === 'BANK_TRANSFER' ? 'Transfers' :
                                         cards.find(c => `card_${c.id}` === filterType)?.name}
                                {' · '}
                                {useCustomDateRange ? `${filterStartDate} ~ ${filterEndDate}` : format(currentDate, 'MMMM yyyy')}
                                {' · '}
                                {transactions.length} transactions
                            </div>
                        )}
                    </div>

                    {isTxFormOpen && (
                        <div className="p-6 bg-blue-50/50 border-b border-blue-100 animate-in slide-in-from-top-2">
                             <div className="flex justify-between items-center mb-4">
                                <h3 className="text-sm font-bold text-blue-900 uppercase tracking-wide">
                                    {editingTx ? 'Edit Transaction' : 'New Transaction'}
                                </h3>
                                <button onClick={() => setIsTxFormOpen(false)} className="text-blue-400 hover:text-blue-700">
                                    <X size={18} />
                                </button>
                            </div>
                            <form onSubmit={handleTxSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Date</label>
                                    <input 
                                        type="date" 
                                        required
                                        value={txForm.date}
                                        onChange={e => setTxForm({...txForm, date: e.target.value})}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Category</label>
                                    <select 
                                        required
                                        value={txForm.categoryId}
                                        onChange={e => setTxForm({...txForm, categoryId: Number(e.target.value)})}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    >
                                        <option value={0}>Select Category</option>
                                        {categories.map(c => (
                                            <option key={c.id} value={c.id}>{c.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Amount</label>
                                    <input 
                                        type="number" 
                                        required
                                        placeholder="0"
                                        value={txForm.amount}
                                        onChange={e => setTxForm({...txForm, amount: Number(e.target.value)})}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Method</label>
                                    <select 
                                        value={txForm.paymentMethod}
                                        onChange={e => setTxForm({...txForm, paymentMethod: e.target.value as PaymentMethod})}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                    >
                                        <option value="CARD">Card</option>
                                        <option value="CASH">Cash</option>
                                        <option value="BANK_TRANSFER">Transfer</option>
                                    </select>
                                </div>
                                {txForm.paymentMethod === 'CARD' && (
                                    <div className="md:col-span-2">
                                        <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Select Card</label>
                                        <select 
                                            value={txForm.cardId || ''}
                                            onChange={e => setTxForm({...txForm, cardId: Number(e.target.value)})}
                                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                        >
                                            <option value="">Select a Card...</option>
                                            {cards.map(card => (
                                                <option key={card.id} value={card.id}>{card.name} ({card.type})</option>
                                            ))}
                                        </select>
                                    </div>
                                )}
                                <div className="md:col-span-2">
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Memo</label>
                                    <input 
                                        type="text" 
                                        value={txForm.memo}
                                        onChange={e => setTxForm({...txForm, memo: e.target.value})}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                                        placeholder="Description (Optional)"
                                    />
                                </div>
                                <div className="md:col-span-2 flex justify-end gap-3 pt-2">
                                    <button type="button" onClick={() => setIsTxFormOpen(false)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-slate-500 hover:bg-slate-100 transition-colors">Cancel</button>
                                    <button type="submit" className="bg-blue-600 text-white px-8 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 shadow-md transition-all">
                                        {editingTx ? 'Update Transaction' : 'Save Transaction'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    <div className="flex-1 overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-50 text-slate-500 font-medium uppercase tracking-wider text-xs">
                                <tr>
                                    <th className="px-6 py-4">Date</th>
                                    <th className="px-6 py-4">Category</th>
                                    <th className="px-6 py-4">Details</th>
                                    <th className="px-6 py-4 text-right">Amount</th>
                                    <th className="px-6 py-4 text-center">Action</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {transactions.length === 0 ? (
                                    <tr><td colSpan={5} className="px-6 py-12 text-center text-slate-400">No transactions this month.</td></tr>
                                ) : (
                                    transactions.map(tx => (
                                        <tr key={tx.id} className="hover:bg-slate-50/80 transition-colors group">
                                            <td className="px-6 py-4 text-slate-600 font-medium">{tx.date}</td>
                                            <td className="px-6 py-4">
                                                <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
                                                    {tx.categoryName}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex flex-col">
                                                    <span className="text-slate-900 font-medium">{tx.memo || '-'}</span>
                                                    <div className="flex items-center gap-1 text-[10px] text-slate-400 mt-0.5">
                                                        {tx.paymentMethod === 'CARD' ? <CreditCard size={10} /> : 
                                                         tx.paymentMethod === 'CASH' ? <Banknote size={10} /> : <Landmark size={10} />}
                                                        <span>{tx.paymentMethod}</span>
                                                        {tx.cardName && <span className="text-blue-500 font-medium">· {tx.cardName}</span>}
                                                    </div>
                                                </div>
                                            </td>
                                            <td className={cn(
                                                "px-6 py-4 text-right font-bold",
                                                categories.find(c => c.name === tx.categoryName)?.type === 'INCOME' ? "text-green-600" : "text-red-500"
                                            )}>
                                                {formatCurrency(tx.amount)}
                                            </td>
                                            <td className="px-6 py-4 text-center">
                                                <div className="flex items-center justify-center space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                                    <button 
                                                        onClick={() => handleEditTx(tx)}
                                                        className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    >
                                                        <Edit2 size={16} />
                                                    </button>
                                                    <button 
                                                        onClick={() => handleDeleteTx(tx.id)}
                                                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                    >
                                                        <Trash2 size={16} />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Category Modal */}
            {isCategoryModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsCategoryModalOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-8 relative animate-in zoom-in-95 duration-200">
                         <button onClick={() => setIsCategoryModalOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20}/>
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">Add New Category</h3>
                        <form onSubmit={handleCreateCategory} className="space-y-5">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Name</label>
                                <input 
                                    type="text" 
                                    required
                                    value={newCategory.name}
                                    onChange={e => setNewCategory({...newCategory, name: e.target.value})}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Type</label>
                                <select 
                                    value={newCategory.type}
                                    onChange={e => setNewCategory({...newCategory, type: e.target.value as TransactionType})}
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                >
                                    <option value="EXPENSE">Expense</option>
                                    <option value="INCOME">Income</option>
                                    <option value="TRANSFER">Transfer</option>
                                </select>
                            </div>
                            <button type="submit" className="w-full bg-blue-600 text-white py-3.5 rounded-xl font-semibold hover:bg-blue-700 transition mt-2">
                                Create Category
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Recurring Transaction Modal */}
            {isRecurringModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsRecurringModalOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative animate-in zoom-in-95 duration-200">
                        <button onClick={() => setIsRecurringModalOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20}/>
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">Add Fixed Cost</h3>
                        
                        <form onSubmit={handleCreateRecurring} className="space-y-4">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">Name</label>
                                <input 
                                    type="text" required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={newRecurring.name}
                                    onChange={e => setNewRecurring({...newRecurring, name: e.target.value})}
                                    placeholder="e.g. Netflix Subscription"
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">Amount</label>
                                    <input 
                                        type="number" required
                                        className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                        value={newRecurring.amount}
                                        onChange={e => setNewRecurring({...newRecurring, amount: Number(e.target.value)})}
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">Day of Month</label>
                                    <input 
                                        type="number" min="1" max="31" required
                                        className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                        value={newRecurring.dayOfMonth}
                                        onChange={e => setNewRecurring({...newRecurring, dayOfMonth: Number(e.target.value)})}
                                    />
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">Category</label>
                                <select 
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={newRecurring.categoryId}
                                    onChange={e => setNewRecurring({...newRecurring, categoryId: Number(e.target.value)})}
                                >
                                    <option value="">Select Category</option>
                                    {categories.map(c => (
                                        <option key={c.id} value={c.id}>{c.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">Method</label>
                                <select 
                                    className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={newRecurring.paymentMethod}
                                    onChange={e => setNewRecurring({...newRecurring, paymentMethod: e.target.value as PaymentMethod})}
                                >
                                    <option value="CARD">Card</option>
                                    <option value="CASH">Cash</option>
                                    <option value="BANK_TRANSFER">Transfer</option>
                                </select>
                            </div>
                            {newRecurring.paymentMethod === 'CARD' && (
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">Select Card</label>
                                    <select 
                                        className="w-full border border-slate-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                        value={newRecurring.cardId || ''}
                                        onChange={e => setNewRecurring({...newRecurring, cardId: Number(e.target.value)})}
                                    >
                                        <option value="">Select Card...</option>
                                        {cards.map(c => (
                                            <option key={c.id} value={c.id}>{c.name}</option>
                                        ))}
                                    </select>
                                </div>
                            )}
                            <button type="submit" className="w-full bg-purple-600 text-white py-3.5 rounded-xl font-semibold hover:bg-purple-700 transition mt-2 shadow-lg shadow-purple-600/20">
                                Save Fixed Cost
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default BudgetPage;