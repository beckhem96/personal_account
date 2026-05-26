import { useState, useEffect, useCallback, useRef } from 'react';
import { Search, Plus, RefreshCw, Trash2, Edit3, TrendingUp, TrendingDown, BarChart3, X, DollarSign, AlertCircle, Newspaper, ChevronDown, ChevronUp, ExternalLink } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../utils';
import type { MyStock, MyStockRequest, SymbolSearchResult, StockAnalysis, MarketOutlookResponse } from '../types';
import {
    getMyStocks, addMyStock, updateMyStock, deleteMyStock,
    searchSymbol, syncStockPrice, syncAllStockPrices, analyzeStock,
    getMarketOutlook
} from '../api/services';

const formatUsd = (value: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
};

const extractErrorMessage = (e: unknown): string => {
    if (e && typeof e === 'object' && 'response' in e) {
        const resp = (e as { response?: { data?: { error?: string } } }).response;
        if (resp?.data?.error) return resp.data.error;
    }
    if (e instanceof Error) return e.message;
    return '알 수 없는 오류가 발생했습니다.';
};

const StockAnalysisPage = () => {
    const [stocks, setStocks] = useState<MyStock[]>([]);
    const [loading, setLoading] = useState(true);
    const [syncingAll, setSyncingAll] = useState(false);
    const [syncingId, setSyncingId] = useState<number | null>(null);

    // 검색
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<SymbolSearchResult[]>([]);
    const [searching, setSearching] = useState(false);
    const [showSearchDropdown, setShowSearchDropdown] = useState(false);
    const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const searchContainerRef = useRef<HTMLDivElement>(null);

    // 종목 추가/수정 모달
    const [showAddModal, setShowAddModal] = useState(false);
    const [editingStock, setEditingStock] = useState<MyStock | null>(null);
    const [formData, setFormData] = useState<MyStockRequest>({
        ticker: '', companyName: '', purchasePrice: 0, quantity: 0
    });

    // 에러 알림
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // 분석
    const [analysisResult, setAnalysisResult] = useState<StockAnalysis | null>(null);
    const [analyzingId, setAnalyzingId] = useState<number | null>(null);
    const [showAnalysisPanel, setShowAnalysisPanel] = useState(false);

    // 시장 전망
    const [marketOutlook, setMarketOutlook] = useState<MarketOutlookResponse | null>(null);
    const [loadingOutlook, setLoadingOutlook] = useState(false);
    const [showOutlookPanel, setShowOutlookPanel] = useState(false);
    const [showOutlookSources, setShowOutlookSources] = useState(false);

    const fetchStocks = useCallback(async () => {
        try {
            setLoading(true);
            const data = await getMyStocks();
            setStocks(data);
        } catch (e) {
            console.error('종목 조회 실패:', e);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchStocks(); }, [fetchStocks]);

    // 클릭 외부 감지
    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (searchContainerRef.current && !searchContainerRef.current.contains(e.target as Node)) {
                setShowSearchDropdown(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    // 검색 디바운스
    const handleSearchChange = (value: string) => {
        setSearchQuery(value);
        if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
        if (value.trim().length < 1) {
            setSearchResults([]);
            setShowSearchDropdown(false);
            return;
        }
        searchTimeoutRef.current = setTimeout(async () => {
            try {
                setSearching(true);
                const results = await searchSymbol(value.trim());
                setSearchResults(results);
                setShowSearchDropdown(true);
            } catch (e) {
                console.error('검색 실패:', e);
            } finally {
                setSearching(false);
            }
        }, 500);
    };

    const handleSelectSymbol = (result: SymbolSearchResult) => {
        setFormData({ ticker: result.symbol, companyName: result.name, purchasePrice: 0, quantity: 0 });
        setEditingStock(null);
        setShowAddModal(true);
        setShowSearchDropdown(false);
        setSearchQuery('');
    };

    const handleSubmitStock = async () => {
        try {
            if (editingStock) {
                await updateMyStock(editingStock.id, formData);
            } else {
                await addMyStock(formData);
            }
            setShowAddModal(false);
            setEditingStock(null);
            await fetchStocks();
        } catch (e) {
            console.error('종목 저장 실패:', e);
        }
    };

    const handleEditStock = (stock: MyStock) => {
        setEditingStock(stock);
        setFormData({
            ticker: stock.ticker,
            companyName: stock.companyName,
            purchasePrice: stock.purchasePrice,
            quantity: stock.quantity
        });
        setShowAddModal(true);
    };

    const handleDeleteStock = async (id: number) => {
        if (!confirm('이 종목을 삭제하시겠습니까?')) return;
        try {
            await deleteMyStock(id);
            await fetchStocks();
        } catch (e) {
            console.error('종목 삭제 실패:', e);
        }
    };

    const handleSyncPrice = async (id: number) => {
        try {
            setSyncingId(id);
            setErrorMessage(null);
            await syncStockPrice(id);
            await fetchStocks();
        } catch (e) {
            console.error('가격 동기화 실패:', e);
            setErrorMessage(extractErrorMessage(e));
        } finally {
            setSyncingId(null);
        }
    };

    const handleSyncAll = async () => {
        try {
            setSyncingAll(true);
            setErrorMessage(null);
            await syncAllStockPrices();
            await fetchStocks();
        } catch (e) {
            console.error('전체 동기화 실패:', e);
        } finally {
            setSyncingAll(false);
        }
    };

    const handleAnalyze = async (id: number) => {
        try {
            setAnalyzingId(id);
            setShowAnalysisPanel(true);
            setAnalysisResult(null);
            const result = await analyzeStock(id);
            setAnalysisResult(result);
        } catch (e) {
            console.error('분석 실패:', e);
        } finally {
            setAnalyzingId(null);
        }
    };

    const handleMarketOutlook = async () => {
        try {
            setLoadingOutlook(true);
            setShowOutlookPanel(true);
            setMarketOutlook(null);
            const data = await getMarketOutlook();
            setMarketOutlook(data);
        } catch (e) {
            console.error('시장 전망 조회 실패:', e);
            setErrorMessage(extractErrorMessage(e));
        } finally {
            setLoadingOutlook(false);
        }
    };

    // 포트폴리오 요약 계산
    const totalInvested = stocks.reduce((sum, s) => sum + s.purchasePrice * s.quantity, 0);
    const totalValuation = stocks.reduce((sum, s) => sum + (s.valuation ?? s.purchasePrice * s.quantity), 0);
    const totalReturn = totalInvested > 0 ? ((totalValuation - totalInvested) / totalInvested) * 100 : 0;

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">미국 주식 포트폴리오</h1>
                    <p className="text-sm text-slate-500 mt-1">보유 종목 관리 및 AI 분석</p>
                </div>
                <button
                    onClick={handleSyncAll}
                    disabled={syncingAll || stocks.length === 0}
                    className={cn(
                        "flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                        syncingAll
                            ? "bg-slate-200 text-slate-400 cursor-not-allowed"
                            : "bg-blue-600 text-white hover:bg-blue-700"
                    )}
                >
                    <RefreshCw size={16} className={cn(syncingAll && "animate-spin")} />
                    {syncingAll ? '동기화 중...' : '전체 가격 동기화'}
                </button>
            </div>

            {/* 포트폴리오 요약 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                            <DollarSign size={20} className="text-blue-600" />
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 font-medium">총 투자금</p>
                            <p className="text-lg font-bold text-slate-800">{formatUsd(totalInvested)}</p>
                        </div>
                    </div>
                </div>
                <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                            <BarChart3 size={20} className="text-green-600" />
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 font-medium">총 평가금</p>
                            <p className="text-lg font-bold text-slate-800">{formatUsd(totalValuation)}</p>
                        </div>
                    </div>
                </div>
                <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
                    <div className="flex items-center gap-3">
                        <div className={cn(
                            "w-10 h-10 rounded-lg flex items-center justify-center",
                            totalReturn >= 0 ? "bg-green-100" : "bg-red-100"
                        )}>
                            {totalReturn >= 0
                                ? <TrendingUp size={20} className="text-green-600" />
                                : <TrendingDown size={20} className="text-red-600" />}
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 font-medium">총 수익률</p>
                            <p className={cn(
                                "text-lg font-bold",
                                totalReturn >= 0 ? "text-green-600" : "text-red-600"
                            )}>
                                {totalReturn >= 0 ? '+' : ''}{totalReturn.toFixed(2)}%
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* 오늘의 전망은? */}
            <div className="flex items-center gap-3">
                <button
                    onClick={handleMarketOutlook}
                    disabled={loadingOutlook}
                    className={cn(
                        "flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-medium transition-colors",
                        loadingOutlook
                            ? "bg-slate-200 text-slate-400 cursor-not-allowed"
                            : "bg-purple-600 text-white hover:bg-purple-700"
                    )}
                >
                    <Newspaper size={16} />
                    {loadingOutlook ? '분석 중...' : '오늘의 전망은?'}
                </button>
                {marketOutlook && !loadingOutlook && (
                    <span className="text-xs text-slate-400">
                        생성: {new Date(marketOutlook.generatedAt).toLocaleString('ko-KR')}
                    </span>
                )}
            </div>

            {/* 시장 전망 리포트 */}
            {showOutlookPanel && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200">
                        <div className="flex items-center gap-2">
                            <Newspaper size={18} className="text-purple-600" />
                            <h2 className="text-sm font-semibold text-slate-700">오늘의 시장 전망</h2>
                        </div>
                        <button onClick={() => setShowOutlookPanel(false)} className="text-slate-400 hover:text-slate-600">
                            <X size={18} />
                        </button>
                    </div>
                    <div className="p-5">
                        {loadingOutlook ? (
                            <div className="flex items-center justify-center py-12 text-slate-400">
                                <RefreshCw size={20} className="animate-spin mr-2" /> CNBC 뉴스 수집 및 AI 분석 중... (최대 1분 소요)
                            </div>
                        ) : marketOutlook ? (
                            <div className="space-y-4">
                                <div className="prose prose-sm prose-slate max-w-none bg-slate-50 rounded-lg p-5">
                                    <ReactMarkdown>{marketOutlook.report}</ReactMarkdown>
                                </div>

                                {/* 참고 기사 접기/펼치기 */}
                                {marketOutlook.sources.length > 0 && (
                                    <div>
                                        <button
                                            onClick={() => setShowOutlookSources(!showOutlookSources)}
                                            className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 transition-colors"
                                        >
                                            {showOutlookSources ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                            참고 기사 ({marketOutlook.sources.length}건)
                                        </button>
                                        {showOutlookSources && (
                                            <div className="mt-2 space-y-1.5">
                                                {marketOutlook.sources.map((article, idx) => (
                                                    <a
                                                        key={idx}
                                                        href={article.link}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="flex items-start gap-2 bg-slate-50 rounded-lg px-4 py-2.5 hover:bg-slate-100 transition-colors group"
                                                    >
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-2">
                                                                <span className="text-xs bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded font-medium whitespace-nowrap">
                                                                    {article.category}
                                                                </span>
                                                                <span className="text-sm text-slate-700 truncate">{article.title}</span>
                                                            </div>
                                                            {article.pubDate && (
                                                                <p className="text-xs text-slate-400 mt-0.5">{article.pubDate}</p>
                                                            )}
                                                        </div>
                                                        <ExternalLink size={14} className="text-slate-300 group-hover:text-slate-500 flex-shrink-0 mt-1" />
                                                    </a>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        ) : null}
                    </div>
                </div>
            )}

            {/* 에러 알림 */}
            {errorMessage && (
                <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl px-5 py-4">
                    <AlertCircle size={18} className="text-red-500 flex-shrink-0" />
                    <p className="text-sm text-red-700 flex-1">{errorMessage}</p>
                    <button onClick={() => setErrorMessage(null)} className="text-red-400 hover:text-red-600">
                        <X size={16} />
                    </button>
                </div>
            )}

            {/* 종목 검색 */}
            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
                <h2 className="text-sm font-semibold text-slate-700 mb-3">종목 검색 및 추가</h2>
                <div ref={searchContainerRef} className="relative">
                    <div className="flex items-center gap-2">
                        <div className="relative flex-1">
                            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => handleSearchChange(e.target.value)}
                                placeholder="티커 또는 회사명 검색 (예: AAPL, Microsoft)"
                                className="w-full pl-9 pr-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            />
                            {searching && (
                                <RefreshCw size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 animate-spin" />
                            )}
                        </div>
                    </div>

                    {showSearchDropdown && searchResults.length > 0 && (
                        <div className="absolute z-20 top-full mt-1 w-full bg-white border border-slate-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                            {searchResults.map((result, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => handleSelectSymbol(result)}
                                    className="w-full flex items-center justify-between px-4 py-3 hover:bg-slate-50 transition-colors text-left border-b border-slate-100 last:border-b-0"
                                >
                                    <div>
                                        <span className="font-semibold text-sm text-slate-800">{result.symbol}</span>
                                        <span className="text-xs text-slate-500 ml-2">{result.name}</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <span className="text-xs text-slate-400">{result.region}</span>
                                        <span className="text-xs bg-slate-100 px-2 py-0.5 rounded">{result.currency}</span>
                                        <Plus size={14} className="text-blue-500" />
                                    </div>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* 보유 종목 테이블 */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="px-5 py-4 border-b border-slate-200">
                    <h2 className="text-sm font-semibold text-slate-700">보유 종목 ({stocks.length})</h2>
                </div>
                {loading ? (
                    <div className="flex items-center justify-center py-12 text-slate-400">
                        <RefreshCw size={20} className="animate-spin mr-2" /> 불러오는 중...
                    </div>
                ) : stocks.length === 0 ? (
                    <div className="text-center py-12 text-slate-400">
                        <BarChart3 size={40} className="mx-auto mb-3 opacity-50" />
                        <p className="text-sm">보유 종목이 없습니다. 위에서 종목을 검색하여 추가해보세요.</p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-slate-50 text-slate-500 text-xs uppercase">
                                    <th className="text-left px-5 py-3 font-medium">종목</th>
                                    <th className="text-right px-5 py-3 font-medium">평단가</th>
                                    <th className="text-right px-5 py-3 font-medium">수량</th>
                                    <th className="text-right px-5 py-3 font-medium">현재가</th>
                                    <th className="text-right px-5 py-3 font-medium">평가금액</th>
                                    <th className="text-right px-5 py-3 font-medium">수익률</th>
                                    <th className="text-center px-5 py-3 font-medium">액션</th>
                                </tr>
                            </thead>
                            <tbody>
                                {stocks.map((stock) => (
                                    <tr key={stock.id} className="border-t border-slate-100 hover:bg-slate-50 transition-colors">
                                        <td className="px-5 py-3">
                                            <div>
                                                <span className="font-semibold text-slate-800">{stock.ticker}</span>
                                                <p className="text-xs text-slate-400 truncate max-w-[200px]">{stock.companyName}</p>
                                            </div>
                                        </td>
                                        <td className="text-right px-5 py-3 text-slate-700">{formatUsd(stock.purchasePrice)}</td>
                                        <td className="text-right px-5 py-3 text-slate-700">{stock.quantity}</td>
                                        <td className="text-right px-5 py-3 text-slate-700">
                                            {stock.currentPrice != null ? formatUsd(stock.currentPrice) : '-'}
                                        </td>
                                        <td className="text-right px-5 py-3 font-medium text-slate-800">
                                            {stock.valuation != null ? formatUsd(stock.valuation) : '-'}
                                        </td>
                                        <td className="text-right px-5 py-3">
                                            {stock.returnRate != null ? (
                                                <span className={cn(
                                                    "font-semibold",
                                                    stock.returnRate >= 0 ? "text-green-600" : "text-red-600"
                                                )}>
                                                    {stock.returnRate >= 0 ? '+' : ''}{stock.returnRate.toFixed(2)}%
                                                </span>
                                            ) : '-'}
                                        </td>
                                        <td className="px-5 py-3">
                                            <div className="flex items-center justify-center gap-1">
                                                <button
                                                    onClick={() => handleSyncPrice(stock.id)}
                                                    disabled={syncingId === stock.id}
                                                    title="가격 동기화"
                                                    className="p-1.5 rounded-lg hover:bg-blue-50 text-blue-500 transition-colors disabled:opacity-50"
                                                >
                                                    <RefreshCw size={14} className={cn(syncingId === stock.id && "animate-spin")} />
                                                </button>
                                                <button
                                                    onClick={() => handleAnalyze(stock.id)}
                                                    disabled={analyzingId === stock.id}
                                                    title="AI 분석"
                                                    className="p-1.5 rounded-lg hover:bg-purple-50 text-purple-500 transition-colors disabled:opacity-50"
                                                >
                                                    <BarChart3 size={14} />
                                                </button>
                                                <button
                                                    onClick={() => handleEditStock(stock)}
                                                    title="수정"
                                                    className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 transition-colors"
                                                >
                                                    <Edit3 size={14} />
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteStock(stock.id)}
                                                    title="삭제"
                                                    className="p-1.5 rounded-lg hover:bg-red-50 text-red-400 transition-colors"
                                                >
                                                    <Trash2 size={14} />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* 분석 결과 패널 */}
            {showAnalysisPanel && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200">
                        <h2 className="text-sm font-semibold text-slate-700">
                            {analysisResult ? `${analysisResult.ticker} AI 분석 리포트` : 'AI 분석 중...'}
                        </h2>
                        <button onClick={() => setShowAnalysisPanel(false)} className="text-slate-400 hover:text-slate-600">
                            <X size={18} />
                        </button>
                    </div>
                    <div className="p-5">
                        {analyzingId ? (
                            <div className="flex items-center justify-center py-12 text-slate-400">
                                <RefreshCw size={20} className="animate-spin mr-2" /> 기술적 지표 수집 및 AI 분석 중... (최대 1분 소요)
                            </div>
                        ) : analysisResult ? (
                            <div className="space-y-6">
                                {/* 기술적 지표 */}
                                {Object.keys(analysisResult.indicators).length > 0 && (
                                    <div>
                                        <h3 className="text-xs font-semibold text-slate-500 uppercase mb-2">기술적 지표</h3>
                                        <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                                            {Object.entries(analysisResult.indicators).map(([key, value]) => (
                                                <div key={key} className="bg-slate-50 rounded-lg px-3 py-2">
                                                    <p className="text-xs text-slate-400">{key}</p>
                                                    <p className="text-sm font-medium text-slate-700">{value}</p>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* 뉴스 */}
                                {analysisResult.news.length > 0 && (
                                    <div>
                                        <h3 className="text-xs font-semibold text-slate-500 uppercase mb-2">관련 뉴스</h3>
                                        <div className="space-y-2">
                                            {analysisResult.news.map((item, idx) => (
                                                <a
                                                    key={idx}
                                                    href={item.url}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="block bg-slate-50 rounded-lg px-4 py-3 hover:bg-slate-100 transition-colors"
                                                >
                                                    <div className="flex items-start justify-between gap-2">
                                                        <p className="text-sm text-slate-700 font-medium">{item.title}</p>
                                                        <span className={cn(
                                                            "text-xs px-2 py-0.5 rounded-full whitespace-nowrap font-medium",
                                                            item.sentiment === 'Bullish' ? "bg-green-100 text-green-700" :
                                                            item.sentiment === 'Bearish' ? "bg-red-100 text-red-700" :
                                                            "bg-slate-200 text-slate-600"
                                                        )}>
                                                            {item.sentiment}
                                                        </span>
                                                    </div>
                                                    {item.summary && (
                                                        <p className="text-xs text-slate-500 mt-1 line-clamp-2">{item.summary}</p>
                                                    )}
                                                </a>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* AI 리포트 */}
                                <div>
                                    <h3 className="text-xs font-semibold text-slate-500 uppercase mb-2">AI 분석 리포트</h3>
                                    <div className="prose prose-sm prose-slate max-w-none bg-slate-50 rounded-lg p-5">
                                        <ReactMarkdown>{analysisResult.report}</ReactMarkdown>
                                    </div>
                                </div>
                            </div>
                        ) : null}
                    </div>
                </div>
            )}

            {/* 종목 추가/수정 모달 */}
            {showAddModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center">
                    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowAddModal(false)} />
                    <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6 mx-4">
                        <div className="flex items-center justify-between mb-5">
                            <h2 className="text-lg font-bold text-slate-800">
                                {editingStock ? '종목 수정' : '종목 추가'}
                            </h2>
                            <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                                <X size={20} />
                            </button>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-xs font-medium text-slate-600 mb-1">티커</label>
                                <input
                                    type="text"
                                    value={formData.ticker}
                                    disabled={!!editingStock}
                                    onChange={(e) => setFormData({ ...formData, ticker: e.target.value.toUpperCase() })}
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm disabled:bg-slate-100"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-600 mb-1">회사명</label>
                                <input
                                    type="text"
                                    value={formData.companyName}
                                    disabled={!!editingStock}
                                    onChange={(e) => setFormData({ ...formData, companyName: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm disabled:bg-slate-100"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-600 mb-1">평단가 (USD)</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    value={formData.purchasePrice || ''}
                                    onChange={(e) => setFormData({ ...formData, purchasePrice: parseFloat(e.target.value) || 0 })}
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-600 mb-1">보유 수량</label>
                                <input
                                    type="number"
                                    value={formData.quantity || ''}
                                    onChange={(e) => setFormData({ ...formData, quantity: parseInt(e.target.value) || 0 })}
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm"
                                />
                            </div>
                        </div>
                        <div className="flex gap-3 mt-6">
                            <button
                                onClick={() => setShowAddModal(false)}
                                className="flex-1 px-4 py-2.5 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50"
                            >
                                취소
                            </button>
                            <button
                                onClick={handleSubmitStock}
                                disabled={!formData.ticker || formData.purchasePrice <= 0 || formData.quantity <= 0}
                                className="flex-1 px-4 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:bg-slate-200 disabled:text-slate-400"
                            >
                                {editingStock ? '수정' : '추가'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default StockAnalysisPage;
