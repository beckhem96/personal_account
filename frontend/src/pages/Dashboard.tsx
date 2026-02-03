import { useEffect, useState } from 'react';
import { getNetWorth, getTransactions } from '../api/services';
import type { NetWorthResponse, TransactionResponse } from '../types';
import { formatCurrency, cn } from '../utils';
import { format, startOfMonth, endOfMonth } from 'date-fns';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { Wallet, TrendingUp, TrendingDown, ArrowUpRight, ArrowDownLeft, Activity } from 'lucide-react';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

const StatCard = ({ title, value, subtext, icon: Icon, colorClass }: { title: string; value: string; subtext?: string; icon: any; colorClass: string }) => (
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
    const [netWorth, setNetWorth] = useState<NetWorthResponse | null>(null);
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const today = new Date();
                const start = format(startOfMonth(today), 'yyyy-MM-dd');
                const end = format(endOfMonth(today), 'yyyy-MM-dd');

                const [netWorthData, txData] = await Promise.all([
                    getNetWorth(),
                    getTransactions(start, end)
                ]);

                setNetWorth(netWorthData);
                setTransactions(txData);
            } catch (error) {
                console.error("Failed to fetch dashboard data", error);
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

    const chartData = netWorth?.assetsByType 
        ? Object.entries(netWorth.assetsByType).map(([name, value]) => ({ name, value }))
        : [];

    return (
        <div className="space-y-8">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Dashboard</h1>
                    <p className="text-slate-500 mt-1">Overview of your financial health.</p>
                </div>
                <div className="flex items-center space-x-2 text-sm text-slate-500 bg-white px-4 py-2 rounded-lg border shadow-sm">
                    <span className="w-2 h-2 rounded-full bg-green-500"></span>
                    <span>Updated just now</span>
                </div>
            </div>
            
            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <StatCard 
                    title="Total Assets" 
                    value={netWorth ? formatCurrency(netWorth.totalAssets) : '-'} 
                    icon={Wallet}
                    colorClass="bg-blue-50 text-blue-600"
                    subtext="All accounts combined"
                />
                <StatCard 
                    title="Total Liabilities" 
                    value={netWorth ? formatCurrency(netWorth.totalLiabilities) : '-'} 
                    icon={TrendingDown}
                    colorClass="bg-red-50 text-red-500"
                    subtext="Loans and debts"
                />
                <StatCard 
                    title="Net Worth" 
                    value={netWorth ? formatCurrency(netWorth.netWorth) : '-'} 
                    icon={TrendingUp}
                    colorClass="bg-green-50 text-green-600"
                    subtext="Assets - Liabilities"
                />
            </div>

            {/* Charts & Recent Activity */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Asset Distribution */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col">
                    <div className="flex items-center justify-between mb-6">
                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Activity size={20} className="text-blue-500"/>
                            Asset Allocation
                        </h3>
                    </div>
                    <div className="flex-1 min-h-[300px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={chartData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={70}
                                    outerRadius={90}
                                    paddingAngle={5}
                                    dataKey="value"
                                    cornerRadius={6}
                                >
                                    {chartData.map((_, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} strokeWidth={0} />
                                    ))}
                                </Pie>
                                <Tooltip 
                                    formatter={(value: number | undefined) => formatCurrency(value || 0)}
                                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                                <Legend verticalAlign="bottom" height={36}/>
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Recent Transactions */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800 mb-6 flex items-center gap-2">
                        <TrendingUp size={20} className="text-blue-500"/>
                        Recent Activity
                    </h3>
                    <div className="space-y-4">
                        {transactions.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-64 text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                                <Activity size={32} className="mb-2 opacity-50"/>
                                <p>No recent transactions found.</p>
                            </div>
                        ) : (
                            transactions.slice(0, 5).map((tx) => (
                                <div key={tx.id} className="flex justify-between items-center p-4 hover:bg-slate-50 rounded-xl transition-colors border border-transparent hover:border-slate-100 group">
                                    <div className="flex items-center gap-4">
                                        <div className={cn(
                                            "w-10 h-10 rounded-full flex items-center justify-center",
                                            "bg-slate-100 text-slate-500 group-hover:bg-white group-hover:shadow-sm transition-all"
                                        )}>
                                            {/* Ideally, map category icon here */}
                                            {tx.amount < 0 ? <ArrowDownLeft size={18}/> : <ArrowUpRight size={18}/>}
                                        </div>
                                        <div>
                                            <p className="font-semibold text-slate-900">{tx.memo || tx.categoryName}</p>
                                            <div className="flex items-center gap-2 text-xs text-slate-500">
                                                <span>{tx.date}</span>
                                                <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
                                                <span>{tx.categoryName}</span>
                                                <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
                                                <span>{tx.paymentMethod}</span>
                                            </div>
                                        </div>
                                    </div>
                                    <span className={cn(
                                        "font-bold text-base",
                                        "text-slate-900" // We can color code expense vs income later
                                    )}>
                                        {formatCurrency(tx.amount)}
                                    </span>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;