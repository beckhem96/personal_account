import { useState, useEffect } from 'react';
import { getBudgets, getCategories, setBudget } from '../api/services';
import type { BudgetResponse, CategoryResponse } from '../types';
import { formatCurrency } from '../utils';
import { ChevronLeft, ChevronRight, Settings, Percent } from 'lucide-react';

const BudgetPage = () => {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [budgetInputs, setBudgetInputs] = useState<Record<number, string>>({});
    const [loading, setLoading] = useState(true);

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;

    useEffect(() => {
        fetchData();
    }, [currentDate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [rawBudgets, cData] = await Promise.all([
                getBudgets({ year, month }),
                getCategories()
            ]);

            // 오직 지출(EXPENSE) 카테고리만 예산 설정 대상으로 제한
            const expenseCategories = cData.filter(c => c.type === 'EXPENSE');

            setBudgets(rawBudgets);
            setCategories(expenseCategories);

            // 기존 설정된 예산 금액으로 인풋 필드 초기화
            const inputs: Record<number, string> = {};
            expenseCategories.forEach(cat => {
                const existing = rawBudgets.find(b => b.categoryId === cat.id);
                inputs[cat.id] = existing ? String(existing.amount) : '';
            });
            setBudgetInputs(inputs);
        } catch (error) {
            console.error("예산 데이터를 로드하는 데 실패했습니다.", error);
        } finally {
            setLoading(false);
        }
    };

    const handleMonthChange = (offset: number) => {
        const newDate = new Date(currentDate);
        newDate.setMonth(newDate.getMonth() + offset);
        setCurrentDate(newDate);
    };

    const handleInputChange = (categoryId: number, value: string) => {
        // 숫자만 입력 가능하도록 정제
        const cleaned = value.replace(/[^0-9]/g, '');
        setBudgetInputs(prev => ({
            ...prev,
            [categoryId]: cleaned
        }));
    };

    const handleSaveBudget = async (categoryId: number) => {
        const value = budgetInputs[categoryId];
        const amount = value === '' ? 0 : Number(value);

        try {
            await setBudget({
                year,
                month,
                categoryId,
                amount
            });
            alert("예산이 성공적으로 설정되었습니다.");
            fetchData(); // 갱신
        } catch (error) {
            console.error("예산 저장 실패", error);
            alert("예산 설정 중 오류가 발생했습니다.");
        }
    };

    if (loading) return (
        <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
    );

    const totalBudgetAmount = budgets.reduce((sum, b) => sum + Number(b.amount), 0);

    return (
        <div className="space-y-8 max-w-4xl mx-auto">
            {/* 상단 헤더 및 날짜 선택 */}
            <div className="flex flex-col sm:flex-row justify-between items-center bg-white p-6 rounded-2xl shadow-sm border border-slate-100 gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
                        <Settings className="text-blue-500" size={24} />
                        월간 예산 설정
                    </h1>
                    <p className="text-sm text-slate-500">각 카테고리별 한 달 소비 계획을 수립하고 관리합니다.</p>
                </div>

                <div className="flex items-center space-x-4 bg-slate-50 px-4 py-2 rounded-xl border">
                    <button 
                        onClick={() => handleMonthChange(-1)} 
                        className="p-1 rounded-lg hover:bg-slate-200 text-slate-600 transition-colors"
                    >
                        <ChevronLeft size={20} />
                    </button>
                    <span className="font-bold text-slate-800 tracking-wide min-w-[90px] text-center">
                        {year}년 {month}월
                    </span>
                    <button 
                        onClick={() => handleMonthChange(1)} 
                        className="p-1 rounded-lg hover:bg-slate-200 text-slate-600 transition-colors"
                    >
                        <ChevronRight size={20} />
                    </button>
                </div>
            </div>

            {/* 이번 달 총 예산 요약 */}
            <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-6 rounded-2xl shadow-lg text-white flex justify-between items-center">
                <div className="space-y-1">
                    <p className="text-blue-100 text-xs font-semibold uppercase tracking-wider">이번 달 총 설정 예산</p>
                    <h2 className="text-4xl font-extrabold tracking-tight">{formatCurrency(totalBudgetAmount)}</h2>
                </div>
                <div className="p-4 bg-white/10 rounded-2xl backdrop-blur-md">
                    <Percent size={32} />
                </div>
            </div>

            {/* 예산 설정 목록 */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                <div className="px-6 py-4 bg-slate-50 border-b border-slate-100">
                    <h3 className="font-bold text-slate-800 text-sm">지출 카테고리별 예산</h3>
                </div>
                <div className="divide-y divide-slate-100">
                    {categories.length === 0 ? (
                        <div className="p-8 text-center text-slate-400">
                            등록된 지출 카테고리가 없습니다. 카테고리를 먼저 설정해 주세요.
                        </div>
                    ) : (
                        categories.map(cat => {
                            const existing = budgets.find(b => b.categoryId === cat.id);
                            return (
                                <div key={cat.id} className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/50 transition-colors">
                                    <div className="space-y-1">
                                        <span className="font-bold text-slate-800 text-base">{cat.name}</span>
                                        {existing && existing.amount > 0 && (
                                            <p className="text-xs text-blue-500 font-medium">
                                                현재 설정액: {formatCurrency(existing.amount)}
                                            </p>
                                        )}
                                    </div>
                                    
                                    <div className="flex items-center gap-3">
                                        <div className="relative rounded-xl shadow-sm max-w-[200px]">
                                            <input
                                                type="text"
                                                value={budgetInputs[cat.id] || ''}
                                                onChange={(e) => handleInputChange(cat.id, e.target.value)}
                                                placeholder="예산 금액 입력"
                                                className="block w-full rounded-xl border-slate-200 pr-12 focus:border-blue-500 focus:ring-blue-500 text-sm font-semibold text-slate-800 py-2.5 px-3 border"
                                            />
                                            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                                                <span className="text-slate-400 text-xs font-bold">원</span>
                                            </div>
                                        </div>
                                        <button
                                            onClick={() => handleSaveBudget(cat.id)}
                                            className="px-5 py-2.5 bg-slate-900 text-white text-sm font-semibold rounded-xl hover:bg-blue-600 hover:shadow-lg hover:shadow-blue-500/20 active:scale-95 transition-all duration-200"
                                        >
                                            저장
                                        </button>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
};

export default BudgetPage;