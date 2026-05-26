import React, { useState } from 'react';
import { calculateStockTax, simulateYearEnd } from '../api/services';
import type { TaxStockResponse, YearEndSettlementResponse } from '../types';
import { formatCurrency, cn } from '../utils';
import { Calculator, TrendingUp, DollarSign, Info, CheckCircle2 } from 'lucide-react';

const TaxPage = () => {
    const [activeTab, setActiveTab] = useState<'STOCK' | 'YEAR_END'>('STOCK');

    return (
        <div className="space-y-8 max-w-5xl mx-auto">
            <div className="text-center md:text-left">
                <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Tax & Simulation</h1>
                <p className="text-slate-500 mt-2">Estimate your taxes and year-end settlements with our smart calculators.</p>
            </div>

            {/* Tabs */}
            <div className="flex justify-center md:justify-start">
                <div className="bg-white p-1.5 rounded-2xl shadow-sm border border-slate-200 inline-flex">
                    <button
                        onClick={() => setActiveTab('STOCK')}
                        className={cn(
                            "px-6 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 flex items-center gap-2",
                            activeTab === 'STOCK'
                                ? "bg-blue-600 text-white shadow-md shadow-blue-600/20"
                                : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
                        )}
                    >
                        <TrendingUp size={16} />
                        Stock Tax
                    </button>
                    <button
                        onClick={() => setActiveTab('YEAR_END')}
                        className={cn(
                            "px-6 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 flex items-center gap-2",
                            activeTab === 'YEAR_END'
                                ? "bg-blue-600 text-white shadow-md shadow-blue-600/20"
                                : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
                        )}
                    >
                        <Calculator size={16} />
                        Year-End Settlement
                    </button>
                </div>
            </div>

            {/* Content */}
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                {activeTab === 'STOCK' ? <StockTaxCalculator /> : <YearEndSettlementSimulator />}
            </div>
        </div>
    );
};

const StockTaxCalculator = () => {
    const [inputs, setInputs] = useState({ sell: '', buy: '' });
    const [result, setResult] = useState<TaxStockResponse | null>(null);

    const handleCalculate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const data = await calculateStockTax({
                totalSellAmount: Number(inputs.sell),
                totalBuyAmount: Number(inputs.buy)
            });
            setResult(data);
        } catch (error) {
            alert('Calculation failed');
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
                <div className="flex items-center gap-3 mb-6">
                     <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
                        <TrendingUp size={24} />
                     </div>
                     <h2 className="text-xl font-bold text-slate-800">Stock Tax Calculator</h2>
                </div>
                
                <form onSubmit={handleCalculate} className="space-y-6">
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Total Sell Amount</label>
                        <div className="relative">
                            <span className="absolute left-4 top-3.5 text-slate-400 font-semibold">₩</span>
                            <input 
                                type="number" 
                                required
                                className="w-full border border-slate-200 rounded-xl pl-10 pr-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all font-medium"
                                value={inputs.sell}
                                onChange={e => setInputs({...inputs, sell: e.target.value})}
                                placeholder="0"
                            />
                        </div>
                        <p className="text-xs text-slate-400 mt-2">Total amount sold during the tax year.</p>
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Total Buy Amount (Fees included)</label>
                        <div className="relative">
                            <span className="absolute left-4 top-3.5 text-slate-400 font-semibold">₩</span>
                            <input 
                                type="number" 
                                required
                                className="w-full border border-slate-200 rounded-xl pl-10 pr-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all font-medium"
                                value={inputs.buy}
                                onChange={e => setInputs({...inputs, buy: e.target.value})}
                                placeholder="0"
                            />
                        </div>
                    </div>
                    <button type="submit" className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold hover:bg-slate-800 shadow-lg shadow-slate-900/20 transition-all mt-4">
                        Calculate Tax
                    </button>
                </form>
            </div>

            {result ? (
                <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-6">
                    <h2 className="text-xl font-bold text-slate-800 mb-4">Calculation Result</h2>
                    
                    <div className="space-y-4">
                        <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl">
                            <span className="text-slate-500 font-medium">Profit</span>
                            <span className="text-lg font-bold text-slate-900">{formatCurrency(result.profit)}</span>
                        </div>
                        <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl">
                            <span className="text-slate-500 font-medium">Basic Deduction</span>
                            <span className="text-lg font-bold text-green-600">-{formatCurrency(result.deduction)}</span>
                        </div>
                         <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                            <span className="text-slate-500 font-medium">Tax Base</span>
                            <span className="text-lg font-bold text-slate-900">{formatCurrency(result.taxBase)}</span>
                        </div>
                    </div>

                    <div className="border-t-2 border-dashed border-slate-100 my-6"></div>

                    <div className="bg-blue-600 p-6 rounded-2xl text-white shadow-xl shadow-blue-600/30">
                        <p className="text-blue-100 font-medium mb-1">Estimated Tax (22%)</p>
                        <p className="text-4xl font-bold tracking-tight">{formatCurrency(result.estimatedTax)}</p>
                        <p className="text-xs text-blue-200 mt-4 opacity-80">* Includes 20% Capital Gains Tax + 2% Local Income Tax.</p>
                    </div>
                </div>
            ) : (
                <div className="hidden lg:flex flex-col items-center justify-center h-full bg-slate-50 rounded-3xl border-2 border-dashed border-slate-200 p-12 text-center text-slate-400">
                    <Calculator size={48} className="mb-4 opacity-50"/>
                    <p className="font-medium">Enter your trade details to see the estimated tax breakdown.</p>
                </div>
            )}
        </div>
    );
};

const YearEndSettlementSimulator = () => {
    const [inputs, setInputs] = useState({ salary: '', card: '', cash: '', market: '', transport: '' });
    const [result, setResult] = useState<YearEndSettlementResponse | null>(null);

    const handleSimulate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const data = await simulateYearEnd({
                totalSalary: Number(inputs.salary),
                creditCardAmount: Number(inputs.card),
                debitCashAmount: Number(inputs.cash),
                traditionalMarketAmount: Number(inputs.market) || 0,
                publicTransportAmount: Number(inputs.transport) || 0
            });
            setResult(data);
        } catch (error) {
            alert('Simulation failed');
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
                <div className="flex items-center gap-3 mb-6">
                     <div className="p-3 bg-green-50 text-green-600 rounded-xl">
                        <DollarSign size={24} />
                     </div>
                     <h2 className="text-xl font-bold text-slate-800">연말정산 시뮬레이터</h2>
                </div>

                <form onSubmit={handleSimulate} className="space-y-4">
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">총급여 (연봉)</label>
                        <input
                            type="number"
                            required
                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
                            value={inputs.salary}
                            onChange={e => setInputs({...inputs, salary: e.target.value})}
                            placeholder="예: 50000000"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">신용카드 사용액 (15%)</label>
                        <input
                            type="number"
                            required
                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
                            value={inputs.card}
                            onChange={e => setInputs({...inputs, card: e.target.value})}
                            placeholder="0"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">체크카드/현금영수증 (30%)</label>
                        <input
                            type="number"
                            required
                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
                            value={inputs.cash}
                            onChange={e => setInputs({...inputs, cash: e.target.value})}
                            placeholder="0"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">전통시장 사용액 (40%)</label>
                        <input
                            type="number"
                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
                            value={inputs.market}
                            onChange={e => setInputs({...inputs, market: e.target.value})}
                            placeholder="0 (선택)"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">대중교통 사용액 (40%)</label>
                        <input
                            type="number"
                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
                            value={inputs.transport}
                            onChange={e => setInputs({...inputs, transport: e.target.value})}
                            placeholder="0 (선택)"
                        />
                    </div>
                    <button type="submit" className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold hover:bg-slate-800 shadow-lg shadow-slate-900/20 transition-all mt-4">
                        공제액 계산
                    </button>
                </form>
            </div>

            {result ? (
                <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-6">
                    <h2 className="text-xl font-bold text-slate-800">계산 결과</h2>

                    <div className="space-y-3">
                        <div className="bg-slate-50 p-4 rounded-xl">
                            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">최저 사용금액 (25%)</h3>
                            <p className="text-2xl font-bold text-slate-900">{formatCurrency(result.minUsageThreshold)}</p>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                            <div className="bg-blue-50 p-4 rounded-xl border border-blue-100">
                                <p className="text-xs text-blue-600 font-medium">신용카드 공제</p>
                                <p className="text-lg font-bold text-blue-800">{formatCurrency(result.creditDeduction)}</p>
                            </div>
                            <div className="bg-indigo-50 p-4 rounded-xl border border-indigo-100">
                                <p className="text-xs text-indigo-600 font-medium">체크/현금 공제</p>
                                <p className="text-lg font-bold text-indigo-800">{formatCurrency(result.debitDeduction)}</p>
                            </div>
                            <div className="bg-orange-50 p-4 rounded-xl border border-orange-100">
                                <p className="text-xs text-orange-600 font-medium">전통시장 공제</p>
                                <p className="text-lg font-bold text-orange-800">{formatCurrency(result.marketDeduction)}</p>
                            </div>
                            <div className="bg-purple-50 p-4 rounded-xl border border-purple-100">
                                <p className="text-xs text-purple-600 font-medium">대중교통 공제</p>
                                <p className="text-lg font-bold text-purple-800">{formatCurrency(result.transportDeduction)}</p>
                            </div>
                        </div>

                        <div className="bg-slate-100 p-4 rounded-xl text-center">
                            <p className="text-xs text-slate-500">일반 공제한도</p>
                            <p className="text-lg font-bold text-slate-700">{formatCurrency(result.generalLimit)}</p>
                        </div>

                        <div className="bg-green-600 p-6 rounded-2xl text-white shadow-lg shadow-green-600/30">
                            <p className="text-green-100 font-medium mb-1">최종 공제액</p>
                            <p className="text-3xl font-bold tracking-tight">{formatCurrency(result.totalDeduction)}</p>
                        </div>
                    </div>

                    <div className="bg-yellow-50 p-5 rounded-2xl border border-yellow-100 flex gap-4">
                        <div className="flex-shrink-0">
                            <div className="w-10 h-10 bg-yellow-100 rounded-full flex items-center justify-center text-yellow-600">
                                <Info size={20} />
                            </div>
                        </div>
                        <div>
                            <h3 className="font-bold text-slate-800 mb-1">절세 가이드</h3>
                            <p className="text-sm text-slate-700 leading-relaxed">
                                {result.guideMessage}
                            </p>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="hidden lg:flex flex-col items-center justify-center h-full bg-slate-50 rounded-3xl border-2 border-dashed border-slate-200 p-12 text-center text-slate-400">
                    <CheckCircle2 size={48} className="mb-4 opacity-50"/>
                    <p className="font-medium">급여와 사용액을 입력하면 예상 공제액을 계산합니다.</p>
                </div>
            )}
        </div>
    );
};

export default TaxPage;