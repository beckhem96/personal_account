import React, { useState } from 'react';
import { calculateStockTax, simulateYearEnd, getAutoYearEndSettlement } from '../api/services';
import type { TaxStockResponse, YearEndSettlementRequest, YearEndSettlementResponse } from '../types';
import { formatCurrency, cn } from '../utils';
import { Calculator, TrendingUp, DollarSign, Info, CheckCircle2, Database, Loader2, RefreshCw } from 'lucide-react';

type TabKey = 'STOCK' | 'YEAR_END_MANUAL' | 'YEAR_END_AUTO';

const TaxPage = () => {
    const [activeTab, setActiveTab] = useState<TabKey>('STOCK');

    return (
        <div className="space-y-8 max-w-5xl mx-auto">
            <div className="text-center md:text-left">
                <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Tax & Simulation</h1>
                <p className="text-slate-500 mt-2">세금과 연말정산 공제액을 시뮬레이션합니다.</p>
            </div>

            {/* Tabs */}
            <div className="flex justify-center md:justify-start">
                <div className="bg-white p-1.5 rounded-2xl shadow-sm border border-slate-200 inline-flex flex-wrap gap-1">
                    <TabButton active={activeTab === 'STOCK'} onClick={() => setActiveTab('STOCK')} icon={<TrendingUp size={16} />} label="주식 양도세" />
                    <TabButton active={activeTab === 'YEAR_END_MANUAL'} onClick={() => setActiveTab('YEAR_END_MANUAL')} icon={<Calculator size={16} />} label="연말정산 (직접 입력)" />
                    <TabButton active={activeTab === 'YEAR_END_AUTO'} onClick={() => setActiveTab('YEAR_END_AUTO')} icon={<Database size={16} />} label="연말정산 (내 거래 기반)" />
                </div>
            </div>

            {/* Content */}
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                {activeTab === 'STOCK' && <StockTaxCalculator />}
                {activeTab === 'YEAR_END_MANUAL' && <YearEndManualSimulator />}
                {activeTab === 'YEAR_END_AUTO' && <YearEndAutoSimulator />}
            </div>
        </div>
    );
};

const TabButton = ({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string }) => (
    <button
        onClick={onClick}
        className={cn(
            "px-6 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 flex items-center gap-2",
            active ? "bg-blue-600 text-white shadow-md shadow-blue-600/20" : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
        )}
    >
        {icon}
        {label}
    </button>
);

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
                     <h2 className="text-xl font-bold text-slate-800">주식 양도세 계산</h2>
                </div>

                <form onSubmit={handleCalculate} className="space-y-6">
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">총 매도금액</label>
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
                        <p className="text-xs text-slate-400 mt-2">과세연도 동안의 매도금액 합계.</p>
                    </div>
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">총 매수금액 (수수료 포함)</label>
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
                        세금 계산
                    </button>
                </form>
            </div>

            {result ? (
                <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-6">
                    <h2 className="text-xl font-bold text-slate-800 mb-4">계산 결과</h2>

                    <div className="space-y-4">
                        <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl">
                            <span className="text-slate-500 font-medium">차익</span>
                            <span className="text-lg font-bold text-slate-900">{formatCurrency(result.profit)}</span>
                        </div>
                        <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl">
                            <span className="text-slate-500 font-medium">기본 공제</span>
                            <span className="text-lg font-bold text-green-600">-{formatCurrency(result.deduction)}</span>
                        </div>
                         <div className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                            <span className="text-slate-500 font-medium">과세 표준</span>
                            <span className="text-lg font-bold text-slate-900">{formatCurrency(result.taxBase)}</span>
                        </div>
                    </div>

                    <div className="border-t-2 border-dashed border-slate-100 my-6"></div>

                    <div className="bg-blue-600 p-6 rounded-2xl text-white shadow-xl shadow-blue-600/30">
                        <p className="text-blue-100 font-medium mb-1">예상 세액 (22%)</p>
                        <p className="text-4xl font-bold tracking-tight">{formatCurrency(result.estimatedTax)}</p>
                        <p className="text-xs text-blue-200 mt-4 opacity-80">* 양도소득세 20% + 지방소득세 2%.</p>
                    </div>
                </div>
            ) : (
                <div className="hidden lg:flex flex-col items-center justify-center h-full bg-slate-50 rounded-3xl border-2 border-dashed border-slate-200 p-12 text-center text-slate-400">
                    <Calculator size={48} className="mb-4 opacity-50"/>
                    <p className="font-medium">매매 정보를 입력하면 예상 세액을 보여드립니다.</p>
                </div>
            )}
        </div>
    );
};

type YearEndInputs = { salary: string; card: string; cash: string; market: string; transport: string };

const emptyInputs: YearEndInputs = { salary: '', card: '', cash: '', market: '', transport: '' };

const requestFromInputs = (inputs: YearEndInputs): YearEndSettlementRequest => ({
    totalSalary: Number(inputs.salary) || 0,
    creditCardAmount: Number(inputs.card) || 0,
    debitCashAmount: Number(inputs.cash) || 0,
    traditionalMarketAmount: Number(inputs.market) || 0,
    publicTransportAmount: Number(inputs.transport) || 0,
});

const inputsFromRequest = (req: YearEndSettlementRequest): YearEndInputs => ({
    salary: String(req.totalSalary ?? 0),
    card: String(req.creditCardAmount ?? 0),
    cash: String(req.debitCashAmount ?? 0),
    market: String(req.traditionalMarketAmount ?? 0),
    transport: String(req.publicTransportAmount ?? 0),
});

const YearEndManualSimulator = () => {
    const [inputs, setInputs] = useState<YearEndInputs>(emptyInputs);
    const [result, setResult] = useState<YearEndSettlementResponse | null>(null);

    const handleSimulate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const data = await simulateYearEnd(requestFromInputs(inputs));
            setResult(data);
        } catch (error) {
            alert('Simulation failed');
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <YearEndInputForm
                inputs={inputs}
                onChange={setInputs}
                onSubmit={handleSimulate}
                title="연말정산 시뮬레이터"
                subtitle={null}
            />
            <YearEndResultPanel result={result} />
        </div>
    );
};

const YearEndAutoSimulator = () => {
    const currentYear = new Date().getFullYear();
    const [year, setYear] = useState<number>(currentYear);
    const [inputs, setInputs] = useState<YearEndInputs>(emptyInputs);
    const [result, setResult] = useState<YearEndSettlementResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [autoLoaded, setAutoLoaded] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleAutoLoad = async () => {
        setLoading(true);
        setError(null);
        setResult(null);
        try {
            const req = await getAutoYearEndSettlement(year);
            setInputs(inputsFromRequest(req));
            setAutoLoaded(true);
        } catch (e: any) {
            setError(e?.response?.data?.message || '자동 산출에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleSimulate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const data = await simulateYearEnd(requestFromInputs(inputs));
            setResult(data);
        } catch (e) {
            alert('Simulation failed');
        }
    };

    const yearOptions = [currentYear - 2, currentYear - 1, currentYear, currentYear + 1];
    const sumAll = (Number(inputs.salary) + Number(inputs.card) + Number(inputs.cash) + Number(inputs.market) + Number(inputs.transport));

    return (
        <div className="space-y-6">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 mb-1 flex items-center gap-2">
                            <Database size={18} className="text-green-600" /> 내 거래에서 자동 산출
                        </h2>
                        <p className="text-sm text-slate-500">선택한 연도의 <strong>확정된</strong> 거래만 집계됩니다. (미확정 거래는 Budget 페이지에서 확정해주세요)</p>
                    </div>
                    <div className="flex gap-2">
                        <select
                            value={year}
                            onChange={e => setYear(Number(e.target.value))}
                            className="border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-medium focus:ring-2 focus:ring-green-500 outline-none"
                        >
                            {yearOptions.map(y => <option key={y} value={y}>{y}년</option>)}
                        </select>
                        <button
                            onClick={handleAutoLoad}
                            disabled={loading}
                            className="bg-green-600 hover:bg-green-700 disabled:bg-slate-300 text-white font-semibold px-5 py-2.5 rounded-xl flex items-center gap-2 transition"
                        >
                            {loading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
                            자동 산출
                        </button>
                    </div>
                </div>

                {autoLoaded && !loading && (
                    <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-3 flex gap-2 items-start">
                        <Info size={16} className="text-amber-600 flex-shrink-0 mt-0.5" />
                        <p className="text-sm text-amber-900">
                            자동 산출된 값입니다. 총급여는 <strong>'월급'</strong> 카테고리 합계로 계산되며,
                            전통시장/대중교통은 Settings에서 카테고리별로 매핑된 거래만 합산됩니다.
                            <strong> 필요시 보정 후 계산을 실행하세요.</strong>
                        </p>
                    </div>
                )}

                {error && (
                    <div className="mt-4 bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-900">{error}</div>
                )}

                {autoLoaded && sumAll === 0 && !loading && (
                    <div className="mt-4 text-sm text-slate-500">
                        해당 연도의 확정 거래가 없거나 분류된 거래가 없습니다. 값을 직접 입력해 계산해보세요.
                    </div>
                )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
                <YearEndInputForm
                    inputs={inputs}
                    onChange={setInputs}
                    onSubmit={handleSimulate}
                    title="연말정산 시뮬레이터"
                    subtitle={autoLoaded ? `${year}년 자동 산출값 (수정 가능)` : null}
                />
                <YearEndResultPanel result={result} />
            </div>
        </div>
    );
};

const YearEndInputForm = ({
    inputs, onChange, onSubmit, title, subtitle,
}: {
    inputs: YearEndInputs;
    onChange: (i: YearEndInputs) => void;
    onSubmit: (e: React.FormEvent) => void;
    title: string;
    subtitle: string | null;
}) => (
    <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
        <div className="flex items-center gap-3 mb-6">
            <div className="p-3 bg-green-50 text-green-600 rounded-xl">
                <DollarSign size={24} />
            </div>
            <div>
                <h2 className="text-xl font-bold text-slate-800">{title}</h2>
                {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
            </div>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
            <NumberField label="총급여 (연봉)" value={inputs.salary} onChange={v => onChange({...inputs, salary: v})} placeholder="예: 50000000" required />
            <NumberField label="신용카드 사용액 (15%)" value={inputs.card} onChange={v => onChange({...inputs, card: v})} placeholder="0" required />
            <NumberField label="체크카드/현금영수증 (30%)" value={inputs.cash} onChange={v => onChange({...inputs, cash: v})} placeholder="0" required />
            <NumberField label="전통시장 사용액 (40%)" value={inputs.market} onChange={v => onChange({...inputs, market: v})} placeholder="0 (선택)" />
            <NumberField label="대중교통 사용액 (40%)" value={inputs.transport} onChange={v => onChange({...inputs, transport: v})} placeholder="0 (선택)" />
            <button type="submit" className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold hover:bg-slate-800 shadow-lg shadow-slate-900/20 transition-all mt-4">
                공제액 계산
            </button>
        </form>
    </div>
);

const NumberField = ({ label, value, onChange, placeholder, required }: { label: string; value: string; onChange: (v: string) => void; placeholder?: string; required?: boolean }) => (
    <div>
        <label className="block text-sm font-bold text-slate-700 mb-2">{label}</label>
        <input
            type="number"
            required={required}
            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition-all"
            value={value}
            onChange={e => onChange(e.target.value)}
            placeholder={placeholder}
        />
    </div>
);

const YearEndResultPanel = ({ result }: { result: YearEndSettlementResponse | null }) => {
    if (!result) {
        return (
            <div className="hidden lg:flex flex-col items-center justify-center h-full bg-slate-50 rounded-3xl border-2 border-dashed border-slate-200 p-12 text-center text-slate-400">
                <CheckCircle2 size={48} className="mb-4 opacity-50"/>
                <p className="font-medium">급여와 사용액을 입력하면 예상 공제액을 계산합니다.</p>
            </div>
        );
    }
    return (
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
                    <p className="text-sm text-slate-700 leading-relaxed">{result.guideMessage}</p>
                </div>
            </div>
        </div>
    );
};

export default TaxPage;
