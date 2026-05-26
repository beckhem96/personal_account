import { useState, useMemo } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { TrendingUp, Info, RefreshCcw } from 'lucide-react';
import { formatCurrency, cn } from '../utils';

// ETF 기본 데이터
const ETF_DATA: Record<string, ETFInfo> = {
    QQQ: {
        name: 'QQQ',
        type: '성장주 (Tech)',
        cagr: 17.5,
        dividend: 0.6,
        color: '#3b82f6',
        description: 'Nasdaq-100 추종 ETF. 높은 성장성, 낮은 배당. 기술주 중심.'
    },
    VOO: {
        name: 'VOO',
        type: '시장지수 (S&P500)',
        cagr: 12.5,
        dividend: 1.5,
        color: '#10b981',
        description: 'S&P500 추종 ETF. 안정적 성장, 시장 평균 수익률.'
    },
    SCHD: {
        name: 'SCHD',
        type: '배당성장',
        cagr: 11.5,
        dividend: 3.5,
        color: '#f59e0b',
        description: '배당 성장주 ETF. 성장과 배당의 균형.'
    },
    JEPI: {
        name: 'JEPI',
        type: '고배당 (Covered Call)',
        cagr: 6.0,
        dividend: 7.5,
        color: '#ef4444',
        description: 'Covered Call 전략 ETF. 낮은 변동성, 높은 월배당.'
    },
    JEPQ: {
        name: 'JEPQ',
        type: '고배당 (Tech)',
        cagr: 9.0,
        dividend: 9.5,
        color: '#8b5cf6',
        description: '나스닥 기반 Covered Call ETF. 기술주 노출 + 고배당.'
    }
};

interface ETFInfo {
    name: string;
    type: string;
    cagr: number;
    dividend: number;
    color: string;
    description: string;
}

interface SimulationResult {
    year: number;
    [key: string]: number; // 각 ETF 자산
}

interface YearlyDetail {
    ticker: string;
    totalAsset: number;
    totalContribution: number;
    totalDividend: number;
    gain: number;
    gainPercent: number;
}

const InvestmentPage = () => {
    // 설정 상태
    const [selectedETFs, setSelectedETFs] = useState<string[]>(['QQQ', 'VOO']);
    const [years, setYears] = useState(10);
    const [monthlyInvestment, setMonthlyInvestment] = useState(1000000);
    const [yearlyIncrease, setYearlyIncrease] = useState(100000);
    const [maxMonthlyInvestment, setMaxMonthlyInvestment] = useState(2000000);
    const [useMaxLimit, setUseMaxLimit] = useState(false);
    const [customRateMode, setCustomRateMode] = useState(false);
    const [customRates, setCustomRates] = useState<Record<string, { cagr: number; dividend: number }>>({});
    const [dripEnabled, setDripEnabled] = useState<Record<string, boolean>>({
        QQQ: true,
        VOO: true,
        SCHD: true,
        JEPI: true,
        JEPQ: true
    });

    // Tooltip 상태
    const [hoveredETF, setHoveredETF] = useState<string | null>(null);

    // ETF 선택 토글
    const toggleETF = (ticker: string) => {
        setSelectedETFs(prev =>
            prev.includes(ticker)
                ? prev.filter(t => t !== ticker)
                : [...prev, ticker]
        );
    };

    // DRIP 토글
    const toggleDrip = (ticker: string) => {
        setDripEnabled(prev => ({
            ...prev,
            [ticker]: !prev[ticker]
        }));
    };

    // 수익률 가져오기 (커스텀 or 기본값)
    const getRate = (ticker: string) => {
        if (customRateMode && customRates[ticker]) {
            return customRates[ticker];
        }
        return { cagr: ETF_DATA[ticker].cagr, dividend: ETF_DATA[ticker].dividend };
    };

    // 시뮬레이션 계산
    const calculateSimulation = (
        _ticker: string,
        years: number,
        monthlyInvestment: number,
        yearlyIncrease: number,
        cagr: number,
        dividend: number,
        drip: boolean,
        maxMonthly: number | null
    ): { yearlyData: number[]; totalDividend: number } => {
        const yearlyData: number[] = [];
        let totalAsset = 0;
        let totalDividend = 0;
        let currentMonthly = monthlyInvestment;

        for (let month = 1; month <= years * 12; month++) {
            // 1. 자산 성장 (월별)
            totalAsset *= (1 + cagr / 100 / 12);

            // 2. 배당금 계산
            const monthlyDividend = totalAsset * (dividend / 100 / 12);
            totalDividend += monthlyDividend;

            // 3. 배당 재투자 여부
            if (drip) {
                totalAsset += monthlyDividend;
            }

            // 4. 월 적립금 추가
            totalAsset += currentMonthly;

            // 5. 매년 적립금 증액 (상한선 적용)
            if (month % 12 === 0) {
                yearlyData.push(totalAsset);
                const nextMonthly = currentMonthly + yearlyIncrease;
                currentMonthly = maxMonthly !== null ? Math.min(nextMonthly, maxMonthly) : nextMonthly;
            }
        }

        return { yearlyData, totalDividend };
    };

    // 차트 데이터 생성
    const chartData = useMemo(() => {
        const data: SimulationResult[] = [];

        // 0년차 (시작점)
        const startPoint: SimulationResult = { year: 0 };
        selectedETFs.forEach(ticker => {
            startPoint[ticker] = 0;
        });
        data.push(startPoint);

        // 각 ETF 시뮬레이션 실행
        const etfResults: Record<string, number[]> = {};
        const maxLimit = useMaxLimit ? maxMonthlyInvestment : null;
        selectedETFs.forEach(ticker => {
            const rate = getRate(ticker);
            const { yearlyData } = calculateSimulation(
                ticker,
                years,
                monthlyInvestment,
                yearlyIncrease,
                rate.cagr,
                rate.dividend,
                dripEnabled[ticker],
                maxLimit
            );
            etfResults[ticker] = yearlyData;
        });

        // 년차별 데이터 구성
        for (let y = 1; y <= years; y++) {
            const point: SimulationResult = { year: y };
            selectedETFs.forEach(ticker => {
                point[ticker] = etfResults[ticker][y - 1];
            });
            data.push(point);
        }

        return data;
    }, [selectedETFs, years, monthlyInvestment, yearlyIncrease, useMaxLimit, maxMonthlyInvestment, customRateMode, customRates, dripEnabled]);

    // 결과 요약 계산
    const summaryData = useMemo((): YearlyDetail[] => {
        const maxLimit = useMaxLimit ? maxMonthlyInvestment : null;
        return selectedETFs.map(ticker => {
            const rate = getRate(ticker);
            const { yearlyData, totalDividend } = calculateSimulation(
                ticker,
                years,
                monthlyInvestment,
                yearlyIncrease,
                rate.cagr,
                rate.dividend,
                dripEnabled[ticker],
                maxLimit
            );

            const totalAsset = yearlyData[yearlyData.length - 1] || 0;

            // 총 원금 계산 (상한선 적용)
            let totalContribution = 0;
            let currentMonthly = monthlyInvestment;
            for (let y = 0; y < years; y++) {
                totalContribution += currentMonthly * 12;
                const nextMonthly = currentMonthly + yearlyIncrease;
                currentMonthly = maxLimit !== null ? Math.min(nextMonthly, maxLimit) : nextMonthly;
            }

            const gain = totalAsset - totalContribution;
            const gainPercent = totalContribution > 0 ? (gain / totalContribution) * 100 : 0;

            return {
                ticker,
                totalAsset,
                totalContribution,
                totalDividend,
                gain,
                gainPercent
            };
        }).sort((a, b) => b.totalAsset - a.totalAsset);
    }, [selectedETFs, years, monthlyInvestment, yearlyIncrease, useMaxLimit, maxMonthlyInvestment, customRateMode, customRates, dripEnabled]);

    // 커스텀 수익률 업데이트
    const updateCustomRate = (ticker: string, field: 'cagr' | 'dividend', value: number) => {
        setCustomRates(prev => ({
            ...prev,
            [ticker]: {
                cagr: field === 'cagr' ? value : (prev[ticker]?.cagr ?? ETF_DATA[ticker].cagr),
                dividend: field === 'dividend' ? value : (prev[ticker]?.dividend ?? ETF_DATA[ticker].dividend)
            }
        }));
    };

    // 설정 초기화
    const resetSettings = () => {
        setSelectedETFs(['QQQ', 'VOO']);
        setYears(10);
        setMonthlyInvestment(1000000);
        setYearlyIncrease(100000);
        setMaxMonthlyInvestment(2000000);
        setUseMaxLimit(false);
        setCustomRateMode(false);
        setCustomRates({});
        setDripEnabled({
            QQQ: true,
            VOO: true,
            SCHD: true,
            JEPI: true,
            JEPQ: true
        });
    };

    // 숫자 포맷팅 (입력용)
    const formatNumberInput = (value: number) => {
        return value.toLocaleString('ko-KR');
    };

    // 입력 파싱
    const parseNumberInput = (value: string) => {
        return Number(value.replace(/,/g, '')) || 0;
    };

    return (
        <div className="space-y-8">
            {/* Header */}
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight flex items-center gap-3">
                        <TrendingUp className="text-blue-500" />
                        투자 시뮬레이션
                    </h1>
                    <p className="text-slate-500 mt-1">ETF 장기 투자 수익률을 비교해보세요</p>
                </div>
                <button
                    onClick={resetSettings}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-600 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors"
                >
                    <RefreshCcw size={16} />
                    설정 초기화
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 설정 패널 (좌측) */}
                <div className="lg:col-span-1 space-y-6">
                    {/* 종목 선택 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">종목 선택</h3>
                        <div className="space-y-3">
                            {Object.entries(ETF_DATA).map(([ticker, info]) => (
                                <div
                                    key={ticker}
                                    className="relative"
                                    onMouseEnter={() => setHoveredETF(ticker)}
                                    onMouseLeave={() => setHoveredETF(null)}
                                >
                                    <label className={cn(
                                        "flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all",
                                        selectedETFs.includes(ticker)
                                            ? "bg-blue-50 border-2 border-blue-200"
                                            : "bg-slate-50 border-2 border-transparent hover:bg-slate-100"
                                    )}>
                                        <div className="flex items-center gap-3">
                                            <input
                                                type="checkbox"
                                                checked={selectedETFs.includes(ticker)}
                                                onChange={() => toggleETF(ticker)}
                                                className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
                                            />
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-bold text-slate-800">{ticker}</span>
                                                    <div
                                                        className="w-3 h-3 rounded-full"
                                                        style={{ backgroundColor: info.color }}
                                                    />
                                                </div>
                                                <span className="text-xs text-slate-500">{info.type}</span>
                                            </div>
                                        </div>
                                        <Info size={16} className="text-slate-400" />
                                    </label>

                                    {/* Tooltip */}
                                    {hoveredETF === ticker && (
                                        <div className="absolute z-10 left-full ml-2 top-0 w-64 p-3 bg-slate-800 text-white text-sm rounded-xl shadow-xl animate-in fade-in slide-in-from-left-2 duration-200">
                                            <p className="font-semibold mb-1">{ticker} - {info.type}</p>
                                            <p className="text-slate-300 text-xs leading-relaxed">{info.description}</p>
                                            <div className="mt-2 pt-2 border-t border-slate-700 grid grid-cols-2 gap-2 text-xs">
                                                <div>
                                                    <span className="text-slate-400">CAGR</span>
                                                    <span className="ml-1 text-green-400 font-medium">{info.cagr}%</span>
                                                </div>
                                                <div>
                                                    <span className="text-slate-400">배당률</span>
                                                    <span className="ml-1 text-amber-400 font-medium">{info.dividend}%</span>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 투자 설정 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">투자 설정</h3>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    투자 기간
                                </label>
                                <div className="flex items-center gap-3">
                                    <input
                                        type="range"
                                        min="1"
                                        max="30"
                                        value={years}
                                        onChange={(e) => setYears(Number(e.target.value))}
                                        className="flex-1 h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                                    />
                                    <span className="w-16 text-center font-bold text-slate-800">{years}년</span>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    월 적립금
                                </label>
                                <div className="relative">
                                    <input
                                        type="text"
                                        value={formatNumberInput(monthlyInvestment)}
                                        onChange={(e) => setMonthlyInvestment(parseNumberInput(e.target.value))}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                                    />
                                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 text-sm">원</span>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    매년 증액
                                </label>
                                <div className="relative">
                                    <input
                                        type="text"
                                        value={formatNumberInput(yearlyIncrease)}
                                        onChange={(e) => setYearlyIncrease(parseNumberInput(e.target.value))}
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                                    />
                                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 text-sm">원/년</span>
                                </div>
                            </div>

                            {/* 최대 월 적립금 */}
                            <div className="pt-2 border-t border-slate-100">
                                <div className="flex items-center justify-between mb-2">
                                    <label className="text-sm font-medium text-slate-700">
                                        적립금 상한 설정
                                    </label>
                                    <button
                                        onClick={() => setUseMaxLimit(!useMaxLimit)}
                                        className={cn(
                                            "relative w-11 h-6 rounded-full transition-colors",
                                            useMaxLimit ? "bg-blue-500" : "bg-slate-300"
                                        )}
                                    >
                                        <div
                                            className={cn(
                                                "absolute w-4 h-4 bg-white rounded-full top-1 transition-transform shadow",
                                                useMaxLimit ? "translate-x-6" : "translate-x-1"
                                            )}
                                        />
                                    </button>
                                </div>
                                {useMaxLimit && (
                                    <div className="relative animate-in slide-in-from-top-1 duration-200">
                                        <input
                                            type="text"
                                            value={formatNumberInput(maxMonthlyInvestment)}
                                            onChange={(e) => setMaxMonthlyInvestment(parseNumberInput(e.target.value))}
                                            className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                                        />
                                        <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 text-sm">원</span>
                                    </div>
                                )}
                                {useMaxLimit && (
                                    <p className="text-xs text-slate-400 mt-2">
                                        증액 후 월 적립금이 {formatCurrency(maxMonthlyInvestment)}을 초과하지 않습니다.
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* 수익률 설정 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider">수익률 설정</h3>
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={customRateMode}
                                    onChange={(e) => setCustomRateMode(e.target.checked)}
                                    className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
                                />
                                <span className="text-xs text-slate-600">직접 입력</span>
                            </label>
                        </div>

                        {customRateMode ? (
                            <div className="space-y-3">
                                {selectedETFs.map(ticker => (
                                    <div key={ticker} className="p-3 bg-slate-50 rounded-xl">
                                        <div className="flex items-center gap-2 mb-2">
                                            <div
                                                className="w-3 h-3 rounded-full"
                                                style={{ backgroundColor: ETF_DATA[ticker].color }}
                                            />
                                            <span className="font-semibold text-slate-800 text-sm">{ticker}</span>
                                        </div>
                                        <div className="grid grid-cols-2 gap-2">
                                            <div>
                                                <label className="text-xs text-slate-500">CAGR (%)</label>
                                                <input
                                                    type="number"
                                                    step="0.1"
                                                    value={customRates[ticker]?.cagr ?? ETF_DATA[ticker].cagr}
                                                    onChange={(e) => updateCustomRate(ticker, 'cagr', Number(e.target.value))}
                                                    className="w-full border border-slate-200 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                                                />
                                            </div>
                                            <div>
                                                <label className="text-xs text-slate-500">배당률 (%)</label>
                                                <input
                                                    type="number"
                                                    step="0.1"
                                                    value={customRates[ticker]?.dividend ?? ETF_DATA[ticker].dividend}
                                                    onChange={(e) => updateCustomRate(ticker, 'dividend', Number(e.target.value))}
                                                    className="w-full border border-slate-200 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                                                />
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">
                                기본 수익률을 사용 중입니다. 직접 입력을 활성화하여 수정하세요.
                            </p>
                        )}
                    </div>

                    {/* 배당 재투자 (DRIP) */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">배당 재투자 (DRIP)</h3>
                        <div className="space-y-2">
                            {selectedETFs.map(ticker => (
                                <div
                                    key={ticker}
                                    className="flex items-center justify-between p-2 rounded-lg hover:bg-slate-50 transition-colors"
                                >
                                    <div className="flex items-center gap-2">
                                        <div
                                            className="w-3 h-3 rounded-full"
                                            style={{ backgroundColor: ETF_DATA[ticker].color }}
                                        />
                                        <span className="font-medium text-slate-700 text-sm">{ticker}</span>
                                        <span className="text-xs text-slate-400">
                                            ({getRate(ticker).dividend}%)
                                        </span>
                                    </div>
                                    <button
                                        onClick={() => toggleDrip(ticker)}
                                        className={cn(
                                            "relative w-11 h-6 rounded-full transition-colors",
                                            dripEnabled[ticker] ? "bg-green-500" : "bg-slate-300"
                                        )}
                                    >
                                        <div
                                            className={cn(
                                                "absolute w-4 h-4 bg-white rounded-full top-1 transition-transform shadow",
                                                dripEnabled[ticker] ? "translate-x-6" : "translate-x-1"
                                            )}
                                        />
                                    </button>
                                </div>
                            ))}
                        </div>
                        <p className="text-xs text-slate-400 mt-3">
                            DRIP ON: 배당금 재투자 / OFF: 배당금 인출
                        </p>
                    </div>
                </div>

                {/* 차트 및 결과 (우측) */}
                <div className="lg:col-span-2 space-y-6">
                    {/* 차트 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <h3 className="text-lg font-bold text-slate-800 mb-4">자산 추이</h3>
                        {selectedETFs.length === 0 ? (
                            <div className="h-80 flex items-center justify-center text-slate-400">
                                종목을 선택해주세요
                            </div>
                        ) : (
                            <div className="h-80">
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={chartData}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="year"
                                            tickFormatter={(value) => `${value}년`}
                                            stroke="#94a3b8"
                                            fontSize={12}
                                        />
                                        <YAxis
                                            tickFormatter={(value) => {
                                                if (value >= 100000000) {
                                                    return `${(value / 100000000).toFixed(1)}억`;
                                                } else if (value >= 10000000) {
                                                    return `${(value / 10000000).toFixed(0)}천만`;
                                                } else if (value >= 10000) {
                                                    return `${(value / 10000).toFixed(0)}만`;
                                                }
                                                return value.toString();
                                            }}
                                            stroke="#94a3b8"
                                            fontSize={12}
                                        />
                                        <Tooltip
                                            formatter={(value, name) => [
                                                formatCurrency(value as number),
                                                name as string
                                            ]}
                                            labelFormatter={(label) => `${label}년차`}
                                            contentStyle={{
                                                backgroundColor: '#1e293b',
                                                border: 'none',
                                                borderRadius: '12px',
                                                color: 'white'
                                            }}
                                        />
                                        <Legend />
                                        {selectedETFs.map(ticker => (
                                            <Line
                                                key={ticker}
                                                type="monotone"
                                                dataKey={ticker}
                                                stroke={ETF_DATA[ticker].color}
                                                strokeWidth={3}
                                                dot={false}
                                                activeDot={{ r: 6 }}
                                            />
                                        ))}
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        )}
                    </div>

                    {/* 결과 요약 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
                        <h3 className="text-lg font-bold text-slate-800 mb-4">
                            {years}년 뒤 예상 결과
                        </h3>

                        {summaryData.length === 0 ? (
                            <div className="text-center text-slate-400 py-8">
                                종목을 선택해주세요
                            </div>
                        ) : (
                            <>
                                {/* 1등 하이라이트 */}
                                {summaryData.length > 0 && (
                                    <div className="mb-6 p-4 bg-gradient-to-r from-amber-50 to-yellow-50 border border-amber-200 rounded-xl">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-amber-500 text-white rounded-full flex items-center justify-center font-bold text-lg">
                                                1
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-bold text-slate-800 text-lg">
                                                        {summaryData[0].ticker}
                                                    </span>
                                                    <div
                                                        className="w-3 h-3 rounded-full"
                                                        style={{ backgroundColor: ETF_DATA[summaryData[0].ticker].color }}
                                                    />
                                                </div>
                                                <p className="text-sm text-slate-600">
                                                    예상 자산 <span className="font-bold text-blue-600">{formatCurrency(summaryData[0].totalAsset)}</span>
                                                    {' '}(원금 대비 <span className="font-bold text-green-600">+{summaryData[0].gainPercent.toFixed(1)}%</span>)
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {/* 상세 테이블 */}
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-slate-200">
                                                <th className="text-left py-3 px-4 font-semibold text-slate-500">종목</th>
                                                <th className="text-right py-3 px-4 font-semibold text-slate-500">총 자산</th>
                                                <th className="text-right py-3 px-4 font-semibold text-slate-500">총 원금</th>
                                                <th className="text-right py-3 px-4 font-semibold text-slate-500">수익</th>
                                                <th className="text-right py-3 px-4 font-semibold text-slate-500">수익률</th>
                                                <th className="text-center py-3 px-4 font-semibold text-slate-500">DRIP</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {summaryData.map((item, index) => (
                                                <tr
                                                    key={item.ticker}
                                                    className={cn(
                                                        "border-b border-slate-100 transition-colors",
                                                        index === 0 && "bg-amber-50/50"
                                                    )}
                                                >
                                                    <td className="py-4 px-4">
                                                        <div className="flex items-center gap-2">
                                                            <div
                                                                className="w-3 h-3 rounded-full"
                                                                style={{ backgroundColor: ETF_DATA[item.ticker].color }}
                                                            />
                                                            <span className="font-bold text-slate-800">{item.ticker}</span>
                                                        </div>
                                                    </td>
                                                    <td className="py-4 px-4 text-right font-bold text-blue-600">
                                                        {formatCurrency(item.totalAsset)}
                                                    </td>
                                                    <td className="py-4 px-4 text-right text-slate-600">
                                                        {formatCurrency(item.totalContribution)}
                                                    </td>
                                                    <td className="py-4 px-4 text-right font-medium text-green-600">
                                                        +{formatCurrency(item.gain)}
                                                    </td>
                                                    <td className="py-4 px-4 text-right font-medium text-green-600">
                                                        +{item.gainPercent.toFixed(1)}%
                                                    </td>
                                                    <td className="py-4 px-4 text-center">
                                                        <span className={cn(
                                                            "px-2 py-1 rounded-full text-xs font-medium",
                                                            dripEnabled[item.ticker]
                                                                ? "bg-green-100 text-green-700"
                                                                : "bg-slate-100 text-slate-500"
                                                        )}>
                                                            {dripEnabled[item.ticker] ? 'ON' : 'OFF'}
                                                        </span>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                {/* 투자 요약 정보 */}
                                <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div className="p-4 bg-slate-50 rounded-xl">
                                        <p className="text-xs text-slate-500 mb-1">투자 기간</p>
                                        <p className="text-lg font-bold text-slate-800">{years}년</p>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-xl">
                                        <p className="text-xs text-slate-500 mb-1">시작 월적립금</p>
                                        <p className="text-lg font-bold text-slate-800">{formatCurrency(monthlyInvestment)}</p>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-xl">
                                        <p className="text-xs text-slate-500 mb-1">매년 증액</p>
                                        <p className="text-lg font-bold text-slate-800">{formatCurrency(yearlyIncrease)}</p>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-xl">
                                        <p className="text-xs text-slate-500 mb-1">총 원금</p>
                                        <p className="text-lg font-bold text-slate-800">
                                            {summaryData.length > 0 ? formatCurrency(summaryData[0].totalContribution) : '-'}
                                        </p>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InvestmentPage;
