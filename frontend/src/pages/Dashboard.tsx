import { useEffect, useState } from 'react';
import { getNetWorth, getTransactions, getCategories, getCards, getBudgets } from '../api/services';
import type { TransactionResponse, CategoryResponse, Card, BudgetResponse } from '../types';
import { formatCurrency, cn } from '../utils';
import { format, startOfMonth, endOfMonth } from 'date-fns';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { Wallet, TrendingUp, TrendingDown, ArrowUpRight, ArrowDownLeft, Activity, ArrowRightLeft, Landmark, Coins } from 'lucide-react';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

const StatCard = ({ title, value, subtext, icon: Icon, colorClass }: { title: string; value: string; subtext?: string; icon: React.ComponentType<any>; colorClass: string }) => (
    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow duration-300">
        <div className="flex items-start justify-between">
            <div>
                <p className="text-sm font-medium text-slate-500 mb-1">{title}</p>
                <h3 className="text-3xl font-bold text-slate-800 tracking-tight">{value}</h3>
                {subtext && <p className="text-xs text-slate-400 mt-2 flex items-center">{subtext}</p>}
            </div>
            <div className={cn("p-3 rounded-xl", colorClass)}>
                <Icon size={24} />
            </div>
        </div>
    </div>
);

const Dashboard = () => {
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [cards, setCards] = useState<Card[]>([]);
    const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const today = new Date();
                const start = format(startOfMonth(today), 'yyyy-MM-dd');
                const end = format(endOfMonth(today), 'yyyy-MM-dd');
                const currentYear = today.getFullYear();
                const currentMonth = today.getMonth() + 1;

                const [, txData, catData, cardData, budgetData] = await Promise.all([
                    getNetWorth(),
                    getTransactions(start, end),
                    getCategories(),
                    getCards(),
                    getBudgets({ year: currentYear, month: currentMonth })
                ]);

                setTransactions(txData);
                setCategories(catData);
                setCards(cardData);
                setBudgets(budgetData);
            } catch (error) {
                console.error("대시보드 데이터를 가져오는 데 실패했습니다.", error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    if (loading) return (
        <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
    );

    // 1. 카테고리 맵 & 카드 맵 구성
    const categoryMap = new Map(categories.map(c => [c.id, c]));
    const cardMap = new Map(cards.map(c => [c.id, c]));

    // 2. 통합 현금흐름 요약 연산
    let totalInflow = 0; // 소득
    let totalOutflow = 0; // 지출
    let totalTransfer = 0; // 이체
    let totalInvestment = 0; // 저축/투자
    let totalInsurance = 0; // 보험료

    // 결제 수단별 지출 집계
    let expenseCredit = 0;
    let expenseCheck = 0;
    let expenseCash = 0;
    let expenseTransfer = 0;

    // 카테고리별 지출 누적 (예산 대비 지출용)
    const categoryExpenses: Record<number, number> = {};

    transactions.forEach(tx => {
        const cat = categoryMap.get(tx.categoryId);
        const amountVal = Number(tx.amount);
        const absAmount = Math.abs(amountVal);

        if (cat) {
            if (cat.type === 'INCOME') {
                totalInflow += amountVal;
            } else if (cat.type === 'EXPENSE') {
                totalOutflow += absAmount;

                // 예산용 카테고리 지출 누적
                categoryExpenses[tx.categoryId] = (categoryExpenses[tx.categoryId] || 0) + absAmount;

                // 특수 분류 집계 (저축/투자, 보험)
                if (cat.name === '저축/투자') {
                    totalInvestment += absAmount;
                } else if (cat.name === '보험료' || cat.name === '보험') {
                    totalInsurance += absAmount;
                }

                // 결제 수단별 집계
                if (tx.paymentMethod === 'CARD') {
                    const card = tx.cardId ? cardMap.get(tx.cardId) : null;
                    if (card && card.type === 'CHECK') {
                        expenseCheck += absAmount;
                    } else {
                        expenseCredit += absAmount; // 카드사 미등록이거나 CREDIT인 경우 신용카드로 분류
                    }
                } else if (tx.paymentMethod === 'CASH') {
                    expenseCash += absAmount;
                } else if (tx.paymentMethod === 'BANK_TRANSFER') {
                    expenseTransfer += absAmount;
                }
            } else if (cat.type === 'TRANSFER' || cat.name === '저축/투자') {
                totalTransfer += absAmount;
                if (cat.name === '저축/투자') {
                    totalInvestment += absAmount;
                }
            }
        }
    });

    const netCashFlow = totalInflow - totalOutflow;

    // 결제수단 통계 차트 데이터
    const paymentChartData = [
        { name: '신용카드', value: expenseCredit },
        { name: '체크카드', value: expenseCheck },
        { name: '현금 지출', value: expenseCash },
        { name: '계좌 이체', value: expenseTransfer }
    ].filter(d => d.value > 0);



    return (
        <div className="space-y-8">
            {/* 상단 헤더 */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">현금흐름 대시보드</h1>
                    <p className="text-slate-500 mt-1">이번 달 자금 흐름과 예산 대비 지출 현황을 모니터링합니다.</p>
                </div>
                <div className="flex items-center space-x-2 text-sm text-slate-500 bg-white px-4 py-2 rounded-lg border shadow-sm w-fit">
                    <span className="w-2 h-2 rounded-full bg-green-500"></span>
                    <span>실시간 업데이트 완료</span>
                </div>
            </div>
            
            {/* 1. 핵심 현금흐름 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <StatCard 
                    title="이번 달 총 소득 (유입)" 
                    value={formatCurrency(totalInflow)} 
                    icon={TrendingUp}
                    colorClass="bg-emerald-50 text-emerald-600"
                    subtext="이번 달 들어온 모든 소득 합계"
                />
                <StatCard 
                    title="이번 달 총 지출 (유출)" 
                    value={formatCurrency(totalOutflow)} 
                    icon={TrendingDown}
                    colorClass="bg-rose-50 text-rose-500"
                    subtext="저축/보험 제외 순수 소비 합계"
                />
                <StatCard 
                    title="순 현금흐름" 
                    value={formatCurrency(netCashFlow)} 
                    icon={Wallet}
                    colorClass={netCashFlow >= 0 ? "bg-blue-50 text-blue-600" : "bg-red-50 text-red-600"}
                    subtext="총 소득 - 총 지출 잔액"
                />
            </div>

            {/* 기타 특수 흐름 요약 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <StatCard 
                    title="저축 및 투자" 
                    value={formatCurrency(totalInvestment)} 
                    icon={Coins}
                    colorClass="bg-amber-50 text-amber-600"
                    subtext="미래를 위한 저축/주식 투자액"
                />
                <StatCard 
                    title="고정 보험료" 
                    value={formatCurrency(totalInsurance)} 
                    icon={Landmark}
                    colorClass="bg-purple-50 text-purple-600"
                    subtext="보장성/저축성 보험 총합"
                />
                <StatCard 
                    title="자산 간 이체" 
                    value={formatCurrency(totalTransfer)} 
                    icon={ArrowRightLeft}
                    colorClass="bg-slate-50 text-slate-600"
                    subtext="계좌이체 등 자산 간 이동 금액"
                />
            </div>

            {/* 2. 시각화 차트 및 예산 진척도 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 결제 수단별 지출 비중 */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col">
                    <div className="flex items-center justify-between mb-6">
                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Activity size={20} className="text-blue-500"/>
                            결제수단별 지출 비중
                        </h3>
                    </div>
                    <div className="flex-1 min-h-[300px]">
                        {paymentChartData.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-full text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                                <Activity size={32} className="mb-2 opacity-50"/>
                                <p>이번 달 등록된 지출 거래가 없습니다.</p>
                            </div>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={paymentChartData}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={70}
                                        outerRadius={90}
                                        paddingAngle={5}
                                        dataKey="value"
                                        cornerRadius={6}
                                    >
                                        {paymentChartData.map((_, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} strokeWidth={0} />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        formatter={(value) => formatCurrency(value as number)}
                                        contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                    />
                                    <Legend verticalAlign="bottom" height={36}/>
                                </PieChart>
                            </ResponsiveContainer>
                        )}
                    </div>
                </div>

                {/* 예산 대비 지출 현황 (Progress Bar) */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col">
                    <div className="flex items-center justify-between mb-6">
                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Landmark size={20} className="text-blue-500"/>
                            카테고리별 예산 대비 지출
                        </h3>
                    </div>
                    <div className="space-y-6 flex-1 overflow-y-auto max-h-[300px] pr-2">
                        {budgets.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-full text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200 py-12">
                                <Landmark size={32} className="mb-2 opacity-50"/>
                                <p>이번 달 설정된 예산이 없습니다.</p>
                            </div>
                        ) : (
                            budgets.map(b => {
                                const spent = categoryExpenses[b.categoryId] || 0;
                                const percent = b.amount > 0 ? (spent / b.amount) * 100 : 0;
                                const isOver = percent >= 100;
                                const isWarning = percent >= 80 && percent < 100;

                                return (
                                    <div key={b.id} className="space-y-2">
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="font-semibold text-slate-700">{b.categoryName}</span>
                                            <span className="text-slate-500 text-xs">
                                                {formatCurrency(spent)} / {formatCurrency(b.amount)} ({percent.toFixed(1)}%)
                                            </span>
                                        </div>
                                        <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden">
                                            <div 
                                                className={cn(
                                                    "h-full rounded-full transition-all duration-500",
                                                    isOver ? "bg-red-500" : isWarning ? "bg-amber-500" : "bg-blue-500"
                                                )}
                                                style={{ width: `${Math.min(percent, 100)}%` }}
                                            />
                                        </div>
                                        {isOver && (
                                            <p className="text-xxs text-red-500 font-semibold flex items-center gap-1 animate-pulse">
                                                ⚠️ 예산 한도 초과! 지출 관리가 필요합니다.
                                            </p>
                                        )}
                                        {isWarning && (
                                            <p className="text-xxs text-amber-500 font-semibold flex items-center gap-1">
                                                💡 예산의 80%에 도달했습니다. 주의하세요.
                                            </p>
                                        )}
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>
            </div>
            
            {/* 최근 거래 내역 리스트 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
                    <TrendingUp size={20} className="text-blue-500"/>
                    이번 달 최근 거래 활동
                </h3>
                <div className="space-y-4">
                    {transactions.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                            <Activity size={32} className="mb-2 opacity-50"/>
                            <p>조회된 최근 거래 내역이 없습니다.</p>
                        </div>
                    ) : (
                        transactions.slice(0, 5).map((tx) => {
                            const isExpense = tx.amount < 0;
                            return (
                                <div key={tx.id} className="flex justify-between items-center p-4 hover:bg-slate-50 rounded-xl transition-colors border border-transparent hover:border-slate-100 group">
                                    <div className="flex items-center gap-4">
                                        <div className={cn(
                                            "w-10 h-10 rounded-full flex items-center justify-center",
                                            isExpense 
                                                ? "bg-rose-50 text-rose-500" 
                                                : "bg-emerald-50 text-emerald-500",
                                            "group-hover:bg-white group-hover:shadow-sm transition-all"
                                        )}>
                                            {isExpense ? <ArrowDownLeft size={18}/> : <ArrowUpRight size={18}/>}
                                        </div>
                                        <div>
                                            <p className="font-semibold text-slate-900">{tx.memo || tx.categoryName}</p>
                                            <div className="flex items-center gap-2 text-xs text-slate-500">
                                                <span>{tx.date}</span>
                                                <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
                                                <span>{tx.categoryName}</span>
                                                <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
                                                <span>
                                                    {tx.paymentMethod === 'CARD' ? `카드 (${tx.cardName || '미지정'})` : 
                                                     tx.paymentMethod === 'CASH' ? '현금' : '계좌이체'}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                    <span className={cn(
                                        "font-bold text-base",
                                        isExpense ? "text-rose-500" : "text-emerald-500"
                                    )}>
                                        {isExpense ? '-' : '+'}{formatCurrency(Math.abs(tx.amount))}
                                    </span>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
};

export default Dashboard;