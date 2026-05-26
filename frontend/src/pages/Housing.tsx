import React, { useState, useEffect, useMemo } from 'react';
import {
    calculateAcquisitionCost, calculateLoanCost, calculateLoanRepayment,
    getLoanProducts, getHousingRegions, getApartmentDeals
} from '../api/services';
import type {
    AcquisitionCostRequest, AcquisitionCostResponse,
    LoanCostRequest, LoanCostResponse,
    LoanRepaymentRequest, LoanRepaymentResponse,
    LoanProductInfo, LoanProductCode,
    ApartmentDealsResponse, RegionTree,
    HouseCount, RepaymentType
} from '../types';
import { formatCurrency, cn, formatExpr, evaluateExpr } from '../utils';
import {
    Home, Receipt, Banknote, Building2, PieChart as PieIcon,
    Calculator, MapPin, TrendingDown, Info
} from 'lucide-react';
import {
    PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
    LineChart, Line, XAxis, YAxis, CartesianGrid,
    BarChart, Bar
} from 'recharts';

type TabKey = 'ACQUISITION' | 'LOAN_COST' | 'REPAYMENT' | 'MARKET';

const TABS: { key: TabKey; label: string; icon: React.ComponentType<{ size?: number }> }[] = [
    { key: 'ACQUISITION', label: '취득·부대비용', icon: Receipt },
    { key: 'LOAN_COST', label: '대출 부대비용', icon: Banknote },
    { key: 'REPAYMENT', label: '대출 상환 계산', icon: Calculator },
    { key: 'MARKET', label: '수도권 아파트', icon: Building2 },
];

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6'];

const HousingPage = () => {
    const [activeTab, setActiveTab] = useState<TabKey>('ACQUISITION');

    return (
        <div className="space-y-8 max-w-7xl mx-auto">
            <div>
                <h1 className="text-3xl font-bold text-slate-900 tracking-tight flex items-center gap-3">
                    <Home className="text-blue-600" /> 주택 구매 비용 계산기
                </h1>
                <p className="text-slate-500 mt-2">취득세·부대비용, 대출 상환, 수도권 실거래가까지 한 번에.</p>
            </div>

            <div className="bg-white p-1.5 rounded-2xl shadow-sm border border-slate-200 inline-flex flex-wrap gap-1">
                {TABS.map(t => (
                    <button
                        key={t.key}
                        onClick={() => setActiveTab(t.key)}
                        className={cn(
                            "px-5 py-2.5 rounded-xl text-sm font-semibold transition-all flex items-center gap-2",
                            activeTab === t.key
                                ? "bg-blue-600 text-white shadow-md shadow-blue-600/20"
                                : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
                        )}
                    >
                        <t.icon size={16} /> {t.label}
                    </button>
                ))}
            </div>

            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                {activeTab === 'ACQUISITION' && <AcquisitionTab />}
                {activeTab === 'LOAN_COST' && <LoanCostTab />}
                {activeTab === 'REPAYMENT' && <RepaymentTab />}
                {activeTab === 'MARKET' && <MarketTab />}
            </div>
        </div>
    );
};

// ───────────────── 공통 유틸 컴포넌트 ─────────────────

const MoneyInput = ({ label, value, onChange, placeholder, suffix }: {
    label: string;
    value: string;
    onChange: (raw: string, num: number | null) => void;
    placeholder?: string;
    suffix?: string;
}) => {
    const num = evaluateExpr(value);
    return (
        <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">{label}</label>
            <div className="relative">
                <input
                    type="text"
                    inputMode="decimal"
                    value={value}
                    onChange={e => {
                        const formatted = formatExpr(e.target.value);
                        onChange(formatted, evaluateExpr(formatted));
                    }}
                    placeholder={placeholder}
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                />
                {suffix && <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400">{suffix}</span>}
            </div>
            {num !== null && /[+\-*/]/.test(value.replace(/^-/, '')) && (
                <p className="text-xs text-blue-600 font-medium mt-1">= {formatCurrency(num)}</p>
            )}
        </div>
    );
};

const ResultRow = ({ label, value, highlight }: { label: string; value: React.ReactNode; highlight?: boolean }) => (
    <div className={cn(
        "flex justify-between items-center py-3 border-b border-slate-100 last:border-b-0",
        highlight && "bg-blue-50 px-4 -mx-4 rounded-xl font-bold text-blue-700 border-blue-100"
    )}>
        <span className="text-slate-600 text-sm">{label}</span>
        <span className={cn("text-slate-900 font-semibold", highlight && "text-blue-700 text-base")}>{value}</span>
    </div>
);

// ───────────────── Tab 1: 취득·부대비용 ─────────────────

const AcquisitionTab = () => {
    const [salePrice, setSalePrice] = useState('600,000,000');
    const [exclusiveArea, setExclusiveArea] = useState('84.96');
    const [houseCount, setHouseCount] = useState<HouseCount>('SINGLE');
    const [isRegulated, setIsRegulated] = useState(false);
    const [isFirstTime, setIsFirstTime] = useState(false);
    const [bondDiscount, setBondDiscount] = useState('8');
    const [result, setResult] = useState<AcquisitionCostResponse | null>(null);
    const [loading, setLoading] = useState(false);

    const handleCalculate = async () => {
        const price = evaluateExpr(salePrice);
        if (!price) return alert('매매가를 입력하세요.');
        const area = Number(exclusiveArea);
        if (!area) return alert('전용면적을 입력하세요.');

        const req: AcquisitionCostRequest = {
            salePrice: price,
            houseCount,
            isRegulatedArea: isRegulated,
            isFirstTime,
            exclusiveAreaSqm: area,
            standardMarketPrice: null,
            bondDiscountRate: Number(bondDiscount) || null,
        };
        setLoading(true);
        try {
            setResult(await calculateAcquisitionCost(req));
        } catch (e) {
            alert('계산 실패');
        } finally {
            setLoading(false);
        }
    };

    const chartData = useMemo(() => {
        if (!result) return [];
        return Object.entries(result.breakdown)
            .filter(([, v]) => v > 0)
            .map(([name, value]) => ({ name, value }));
    }, [result]);

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-5">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2"><Receipt /> 입력</h2>
                <MoneyInput label="매매가" value={salePrice} placeholder="600,000,000" suffix="원"
                    onChange={raw => setSalePrice(raw)} />
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">전용면적 (㎡)</label>
                    <input type="number" step="0.01" value={exclusiveArea} onChange={e => setExclusiveArea(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                    <p className="text-xs text-slate-400 mt-1">85㎡ 초과 시 농어촌특별세 부과</p>
                </div>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">보유 주택수 (취득 후)</label>
                    <select value={houseCount} onChange={e => setHouseCount(e.target.value as HouseCount)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                        <option value="SINGLE">1주택</option>
                        <option value="TWO">2주택</option>
                        <option value="THREE_OR_MORE">3주택 이상</option>
                    </select>
                </div>
                <div className="grid grid-cols-2 gap-3">
                    <label className="flex items-center gap-2 cursor-pointer p-3 border border-slate-200 rounded-xl hover:bg-slate-50">
                        <input type="checkbox" checked={isRegulated} onChange={e => setIsRegulated(e.target.checked)}
                            className="w-4 h-4 text-blue-600 rounded" />
                        <span className="text-sm font-medium text-slate-700">조정대상지역</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer p-3 border border-slate-200 rounded-xl hover:bg-slate-50">
                        <input type="checkbox" checked={isFirstTime} onChange={e => setIsFirstTime(e.target.checked)}
                            className="w-4 h-4 text-blue-600 rounded" />
                        <span className="text-sm font-medium text-slate-700">생애최초</span>
                    </label>
                </div>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">국민주택채권 할인율 (%)</label>
                    <input type="number" step="0.1" value={bondDiscount} onChange={e => setBondDiscount(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                    <p className="text-xs text-slate-400 mt-1">매입한 채권을 즉시 매각 시의 손실률 (당일 시세 기준)</p>
                </div>
                <button onClick={handleCalculate} disabled={loading}
                    className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition disabled:opacity-50">
                    {loading ? '계산 중...' : '계산하기'}
                </button>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2 mb-4"><PieIcon /> 부대비용 분해</h2>
                {!result ? (
                    <div className="text-center py-12 text-slate-400">
                        <Info size={36} className="mx-auto mb-3 opacity-30" />
                        매매가를 입력하고 계산하기를 누르세요.
                    </div>
                ) : (
                    <>
                        <div className="h-64">
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie data={chartData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={false}>
                                        {chartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                                    </Pie>
                                    <Tooltip formatter={(v, name) => [formatCurrency(v as number), name as string]} />
                                    <Legend wrapperStyle={{ fontSize: '12px' }} />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                        <div className="mt-6 space-y-0">
                            <ResultRow label="취득세" value={formatCurrency(result.acquisitionTax)} />
                            <ResultRow label="지방교육세" value={formatCurrency(result.localEducationTax)} />
                            <ResultRow label="농어촌특별세" value={formatCurrency(result.ruralSpecialTax)} />
                            <ResultRow label="중개수수료" value={formatCurrency(result.brokerFee)} />
                            <ResultRow label="법무사 보수" value={formatCurrency(result.judicialFee)} />
                            <ResultRow label="인지세" value={formatCurrency(result.stampDuty)} />
                            <ResultRow label="국민주택채권 할인손실" value={formatCurrency(result.nationalHousingBondLoss)} />
                            <ResultRow label="총 부대비용" value={formatCurrency(result.totalCost)} highlight />
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

// ───────────────── Tab 2: 대출 부대비용 ─────────────────

const LoanCostTab = () => {
    const [loanAmount, setLoanAmount] = useState('300,000,000');
    const [includeAppraisal, setIncludeAppraisal] = useState(true);
    const [result, setResult] = useState<LoanCostResponse | null>(null);

    const handleCalculate = async () => {
        const amount = evaluateExpr(loanAmount);
        if (!amount) return alert('대출액을 입력하세요.');
        const req: LoanCostRequest = { loanAmount: amount, includeAppraisalFee: includeAppraisal };
        try {
            setResult(await calculateLoanCost(req));
        } catch {
            alert('계산 실패');
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-5">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2"><Banknote /> 입력</h2>
                <MoneyInput label="대출 실행 금액" value={loanAmount} suffix="원"
                    onChange={raw => setLoanAmount(raw)} />
                <label className="flex items-center gap-2 cursor-pointer p-3 border border-slate-200 rounded-xl hover:bg-slate-50">
                    <input type="checkbox" checked={includeAppraisal} onChange={e => setIncludeAppraisal(e.target.checked)}
                        className="w-4 h-4 text-blue-600 rounded" />
                    <span className="text-sm font-medium text-slate-700">감정평가수수료 포함 (약 30만원)</span>
                </label>
                <button onClick={handleCalculate}
                    className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition">
                    계산하기
                </button>
                <div className="bg-amber-50 border border-amber-100 p-4 rounded-xl text-xs text-amber-800">
                    <Info size={14} className="inline mr-1" />
                    근저당설정비는 채권최고액(대출액의 120%)의 0.24%로 계산됩니다.
                </div>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
                <h2 className="text-xl font-bold text-slate-800 mb-4">결과</h2>
                {!result ? (
                    <div className="text-center py-12 text-slate-400">대출액을 입력하고 계산하세요.</div>
                ) : (
                    <div className="space-y-0">
                        <ResultRow label="근저당설정비" value={formatCurrency(result.mortgageRegistrationCost)} />
                        <ResultRow label="인지세 (약정서)" value={formatCurrency(result.stampDuty)} />
                        <ResultRow label="감정평가수수료" value={formatCurrency(result.appraisalFee)} />
                        <ResultRow label="총 대출 부대비용" value={formatCurrency(result.totalCost)} highlight />
                    </div>
                )}
            </div>
        </div>
    );
};

// ───────────────── Tab 3: 대출 상환 계산 ─────────────────

const RepaymentTab = () => {
    const [principal, setPrincipal] = useState('300,000,000');
    const [rate, setRate] = useState('4.0');
    const [years, setYears] = useState('30');
    const [repaymentType, setRepaymentType] = useState<RepaymentType>('PRINCIPAL_INTEREST');
    const [graceMonths, setGraceMonths] = useState('0');
    const [productCode, setProductCode] = useState<LoanProductCode | ''>('');
    const [products, setProducts] = useState<LoanProductInfo[]>([]);
    const [result, setResult] = useState<LoanRepaymentResponse | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        getLoanProducts().then(setProducts).catch(() => { });
    }, []);

    const selectedProduct = products.find(p => p.code === productCode);

    const handleCalculate = async () => {
        const p = evaluateExpr(principal);
        if (!p) return alert('대출 원금을 입력하세요.');
        const req: LoanRepaymentRequest = {
            principal: p,
            annualRatePercent: Number(rate),
            termMonths: Number(years) * 12,
            repaymentType,
            gracePeriodMonths: Number(graceMonths) || 0,
            productCode: productCode || null,
        };
        setLoading(true);
        try {
            setResult(await calculateLoanRepayment(req));
        } catch {
            alert('계산 실패');
        } finally {
            setLoading(false);
        }
    };

    const chartData = useMemo(() => {
        if (!result) return [];
        // 표시 부하 절감: 12개월 단위로 샘플링 (마지막 회차 포함)
        const sampled = result.schedule.filter((_, i, arr) => i % 12 === 0 || i === arr.length - 1);
        let cumulativePrincipal = 0;
        let cumulativeInterest = 0;
        return sampled.map(r => {
            cumulativePrincipal += r.principal;
            cumulativeInterest += r.interest;
            return {
                month: r.month,
                잔액: r.remainingBalance,
                누적원금: cumulativePrincipal,
                누적이자: cumulativeInterest,
            };
        });
    }, [result]);

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-5">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2"><Calculator /> 입력</h2>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">대출 상품군</label>
                    <select value={productCode} onChange={e => setProductCode(e.target.value as LoanProductCode | '')}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                        <option value="">선택 안 함</option>
                        {products.map(p => <option key={p.code} value={p.code}>{p.name}</option>)}
                    </select>
                    {selectedProduct && (
                        <div className="mt-3 bg-blue-50 border border-blue-100 p-4 rounded-xl text-xs text-blue-800 space-y-1">
                            <p className="font-semibold">{selectedProduct.description}</p>
                            <p>최대 한도: {formatCurrency(selectedProduct.maxLoanAmount)} · 참고 금리: {selectedProduct.referenceRate}%</p>
                            <ul className="list-disc list-inside space-y-0.5">
                                {selectedProduct.eligibility.map((e, i) => <li key={i}>{e}</li>)}
                            </ul>
                        </div>
                    )}
                </div>
                <MoneyInput label="대출 원금" value={principal} suffix="원"
                    onChange={raw => setPrincipal(raw)} />
                <div className="grid grid-cols-2 gap-3">
                    <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1.5">연 금리 (%)</label>
                        <input type="number" step="0.01" value={rate} onChange={e => setRate(e.target.value)}
                            className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1.5">대출 기간 (년)</label>
                        <input type="number" value={years} onChange={e => setYears(e.target.value)}
                            className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                    </div>
                </div>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">상환 방식</label>
                    <select value={repaymentType} onChange={e => setRepaymentType(e.target.value as RepaymentType)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                        <option value="PRINCIPAL_INTEREST">원리금균등분할상환</option>
                        <option value="PRINCIPAL_ONLY">원금균등분할상환</option>
                        <option value="BULLET">만기일시상환</option>
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">거치기간 (개월)</label>
                    <input type="number" value={graceMonths} onChange={e => setGraceMonths(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                    <p className="text-xs text-slate-400 mt-1">거치 동안은 이자만 납부</p>
                </div>
                <button onClick={handleCalculate} disabled={loading}
                    className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition disabled:opacity-50">
                    {loading ? '계산 중...' : '상환 스케줄 계산'}
                </button>
            </div>

            <div className="space-y-6">
                {!result ? (
                    <div className="bg-white p-12 rounded-3xl shadow-sm border border-slate-100 text-center text-slate-400">
                        대출 조건을 입력하고 계산하세요.
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                                <p className="text-xs text-slate-500 mb-1">첫 회차 납입액</p>
                                <p className="text-xl font-bold text-blue-600">{formatCurrency(result.firstMonthPayment)}</p>
                            </div>
                            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                                <p className="text-xs text-slate-500 mb-1">마지막 회차 납입액</p>
                                <p className="text-xl font-bold text-slate-700">{formatCurrency(result.lastMonthPayment)}</p>
                            </div>
                            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                                <p className="text-xs text-slate-500 mb-1">총 이자</p>
                                <p className="text-xl font-bold text-red-500">{formatCurrency(result.totalInterest)}</p>
                            </div>
                            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                                <p className="text-xs text-slate-500 mb-1">총 납입액</p>
                                <p className="text-xl font-bold text-slate-900">{formatCurrency(result.totalPayment)}</p>
                            </div>
                        </div>

                        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                            <h3 className="text-sm font-bold text-slate-700 mb-3 flex items-center gap-2"><TrendingDown size={16} /> 상환 곡선 (연 단위)</h3>
                            <div className="h-72">
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="month" tickFormatter={(m: number) => `${Math.floor(m / 12)}년`} tick={{ fontSize: 11 }} />
                                        <YAxis tickFormatter={(v: number) => `${(v / 100000000).toFixed(1)}억`} tick={{ fontSize: 11 }} />
                                        <Tooltip formatter={(v, name) => [formatCurrency(v as number), name as string]} labelFormatter={(m) => `${m}회차`} />
                                        <Legend wrapperStyle={{ fontSize: '12px' }} />
                                        <Line type="monotone" dataKey="잔액" stroke="#3b82f6" strokeWidth={2} dot={false} />
                                        <Line type="monotone" dataKey="누적원금" stroke="#10b981" strokeWidth={2} dot={false} />
                                        <Line type="monotone" dataKey="누적이자" stroke="#ef4444" strokeWidth={2} dot={false} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                            <h3 className="text-sm font-bold text-slate-700 p-4 border-b border-slate-100">회차별 상환 스케줄</h3>
                            <div className="max-h-80 overflow-y-auto">
                                <table className="w-full text-xs">
                                    <thead className="bg-slate-50 text-slate-500 sticky top-0">
                                        <tr>
                                            <th className="px-3 py-2 text-left">회차</th>
                                            <th className="px-3 py-2 text-right">납입액</th>
                                            <th className="px-3 py-2 text-right">원금</th>
                                            <th className="px-3 py-2 text-right">이자</th>
                                            <th className="px-3 py-2 text-right">잔액</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {result.schedule.map(r => (
                                            <tr key={r.month} className="hover:bg-slate-50">
                                                <td className="px-3 py-1.5 text-slate-600">{r.month}</td>
                                                <td className="px-3 py-1.5 text-right font-medium">{formatCurrency(r.payment)}</td>
                                                <td className="px-3 py-1.5 text-right text-green-600">{formatCurrency(r.principal)}</td>
                                                <td className="px-3 py-1.5 text-right text-red-500">{formatCurrency(r.interest)}</td>
                                                <td className="px-3 py-1.5 text-right text-slate-500">{formatCurrency(r.remainingBalance)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

// ───────────────── Tab 4: 수도권 아파트 ─────────────────

const MarketTab = () => {
    const [regions, setRegions] = useState<RegionTree[]>([]);
    const [selectedRegion, setSelectedRegion] = useState<string>('SEOUL');
    const [lawdCd, setLawdCd] = useState<string>('');
    const today = new Date();
    const defaultYm = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}`;
    const [dealYm, setDealYm] = useState(defaultYm);
    const [minPrice, setMinPrice] = useState('');
    const [maxPrice, setMaxPrice] = useState('');
    const [minArea, setMinArea] = useState('');
    const [result, setResult] = useState<ApartmentDealsResponse | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        getHousingRegions().then(rs => {
            setRegions(rs);
            const first = rs.find(r => r.region === 'SEOUL')?.districts[0];
            if (first) setLawdCd(first.code);
        }).catch(() => { });
    }, []);

    const currentDistricts = regions.find(r => r.region === selectedRegion)?.districts || [];

    const handleSearch = async () => {
        if (!lawdCd) return alert('시군구를 선택하세요.');
        setLoading(true);
        try {
            const data = await getApartmentDeals({
                lawdCd,
                dealYearMonth: dealYm,
                minPrice: evaluateExpr(minPrice) || undefined,
                maxPrice: evaluateExpr(maxPrice) || undefined,
                minArea: Number(minArea) || undefined,
            });
            setResult(data);
        } catch {
            alert('조회 실패. MOLIT API 키가 설정됐는지 확인하세요.');
        } finally {
            setLoading(false);
        }
    };

    const barChartData = useMemo(() => {
        if (!result) return [];
        return [
            { name: '평균 거래가', 금액: result.averagePrice },
            { name: '평균 ㎡당가', 금액: result.averagePricePerSqm },
        ];
    }, [result]);

    return (
        <div className="space-y-6">
            <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-100">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2 mb-5"><MapPin /> 조회 조건</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">권역</label>
                        <select value={selectedRegion} onChange={e => {
                            setSelectedRegion(e.target.value);
                            const first = regions.find(r => r.region === e.target.value)?.districts[0];
                            if (first) setLawdCd(first.code);
                        }} className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm">
                            {regions.map(r => <option key={r.region} value={r.region}>{r.regionLabel}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">시군구</label>
                        <select value={lawdCd} onChange={e => setLawdCd(e.target.value)}
                            className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm">
                            {currentDistricts.map(d => <option key={d.code} value={d.code}>{d.name}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">거래월 (yyyyMM)</label>
                        <input type="text" value={dealYm} onChange={e => setDealYm(e.target.value)} maxLength={6}
                            className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm" />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">최소가 (원)</label>
                        <input type="text" value={minPrice} onChange={e => setMinPrice(formatExpr(e.target.value))} placeholder="예: 500,000,000"
                            className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm" />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">최대가 (원)</label>
                        <input type="text" value={maxPrice} onChange={e => setMaxPrice(formatExpr(e.target.value))} placeholder="예: 1,000,000,000"
                            className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm" />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">최소 전용면적 (㎡)</label>
                        <input type="number" value={minArea} onChange={e => setMinArea(e.target.value)}
                            className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm" />
                    </div>
                </div>
                <button onClick={handleSearch} disabled={loading}
                    className="mt-5 bg-blue-600 text-white px-6 py-2.5 rounded-xl font-semibold hover:bg-blue-700 transition disabled:opacity-50">
                    {loading ? '조회 중...' : '실거래 조회'}
                </button>
            </div>

            {result && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
                        <h3 className="text-sm font-bold text-slate-700 mb-3">권역 평균</h3>
                        <div className="space-y-2">
                            <ResultRow label="평균 거래가" value={formatCurrency(result.averagePrice)} />
                            <ResultRow label="평균 ㎡당 가격" value={formatCurrency(result.averagePricePerSqm)} />
                            <ResultRow label="전체 거래 건수" value={`${result.totalDeals}건`} />
                            <ResultRow label="필터 적용 후" value={`${result.filteredDeals}건`} />
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 lg:col-span-2">
                        <h3 className="text-sm font-bold text-slate-700 mb-3">평균 시각화</h3>
                        <div className="h-48">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={barChartData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                    <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                                    <YAxis tickFormatter={(v: number) => `${(v / 100000000).toFixed(1)}억`} tick={{ fontSize: 11 }} />
                                    <Tooltip formatter={(v, name) => [formatCurrency(v as number), name as string]} />
                                    <Bar dataKey="금액" fill="#3b82f6" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>
            )}

            {result && result.deals.length > 0 && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                    <h3 className="text-sm font-bold text-slate-700 p-5 border-b border-slate-100">거래 목록 ({result.filteredDeals}건)</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-5">
                        {result.deals.slice(0, 60).map((d, i) => (
                            <div key={i} className="border border-slate-100 rounded-xl p-4 hover:shadow-md transition">
                                <div className="flex justify-between items-start mb-2">
                                    <h4 className="font-bold text-slate-800 text-sm">{d.apartmentName}</h4>
                                    <span className="text-xs text-slate-400">{d.dong}</span>
                                </div>
                                <p className="text-lg font-bold text-blue-600">{formatCurrency(d.dealAmount)}</p>
                                <div className="flex justify-between text-xs text-slate-500 mt-2">
                                    <span>{d.exclusiveArea}㎡ · {d.floor}층</span>
                                    <span>{d.buildYear}년 · {d.dealDate}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {result && result.deals.length === 0 && (
                <div className="bg-white p-12 rounded-2xl shadow-sm border border-slate-100 text-center text-slate-400">
                    조건에 맞는 거래가 없습니다. MOLIT API 키가 미설정이면 빈 결과가 반환됩니다.
                </div>
            )}
        </div>
    );
};

export default HousingPage;
