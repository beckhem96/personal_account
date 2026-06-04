import React, { useEffect, useMemo, useState } from 'react';
import { Bell, RefreshCw, AlertCircle, Loader2, MapPin, Calendar, ExternalLink, Info, Building2 } from 'lucide-react';
import { getTodaySubscriptions } from '../api/services';
import type { SubscriptionItem, SubscriptionRank, SubscriptionsResponse } from '../types';

const REGION_OPTIONS: Array<{ key: string; label: string; predicate: (it: SubscriptionItem) => boolean }> = [
    { key: 'SEOUL', label: '서울', predicate: it => (it.regionLabel ?? '').includes('서울') },
    { key: 'UIJEONGBU', label: '의정부', predicate: it => (it.address ?? '').includes('의정부') },
    { key: 'NAMYANGJU', label: '남양주', predicate: it => (it.address ?? '').includes('남양주') },
    { key: 'HANAM', label: '하남', predicate: it => (it.address ?? '').includes('하남') },
    { key: 'GURI', label: '구리', predicate: it => (it.address ?? '').includes('구리') },
];

const RANK_LABEL: Record<SubscriptionRank, string> = {
    FIRST: '1순위',
    SECOND: '2순위',
    REMAINDER: '무순위',
};

const RANK_COLOR: Record<SubscriptionRank, string> = {
    FIRST: 'bg-blue-100 text-blue-800 border-blue-200',
    SECOND: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    REMAINDER: 'bg-amber-100 text-amber-800 border-amber-200',
};

const SubscriptionsPage = () => {
    const [data, setData] = useState<SubscriptionsResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [enabledRegions, setEnabledRegions] = useState<Set<string>>(new Set(REGION_OPTIONS.map(r => r.key)));
    const [enabledRanks, setEnabledRanks] = useState<Set<SubscriptionRank>>(new Set(['FIRST', 'SECOND', 'REMAINDER']));
    const [lastFetched, setLastFetched] = useState<Date | null>(null);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await getTodaySubscriptions();
            setData(res);
            setLastFetched(new Date());
        } catch (e: any) {
            setError(e?.response?.data?.message || e?.message || '청약 정보를 불러올 수 없습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const filteredFirst = useMemo(() => filterByRegion(data?.firstRank ?? [], enabledRegions), [data, enabledRegions]);
    const filteredSecond = useMemo(() => filterByRegion(data?.secondRank ?? [], enabledRegions), [data, enabledRegions]);
    const filteredRemainder = useMemo(() => filterByRegion(data?.remainder ?? [], enabledRegions), [data, enabledRegions]);

    const toggleRegion = (key: string) => {
        const next = new Set(enabledRegions);
        if (next.has(key)) next.delete(key); else next.add(key);
        setEnabledRegions(next);
    };

    const toggleRank = (rank: SubscriptionRank) => {
        const next = new Set(enabledRanks);
        if (next.has(rank)) next.delete(rank); else next.add(rank);
        setEnabledRanks(next);
    };

    const sections: Array<{ rank: SubscriptionRank; items: SubscriptionItem[] }> = ([
        { rank: 'FIRST' as SubscriptionRank, items: filteredFirst },
        { rank: 'SECOND' as SubscriptionRank, items: filteredSecond },
        { rank: 'REMAINDER' as SubscriptionRank, items: filteredRemainder },
    ]).filter(s => enabledRanks.has(s.rank));

    const totalShown = sections.reduce((a, s) => a + s.items.length, 0);
    const apiKeyMissing = data && !data.apiKeyConfigured;

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-2">
                        <Bell size={28} className="text-blue-600" /> 오늘 접수중인 청약
                    </h1>
                    <p className="text-slate-500 mt-1">
                        {data?.asOf ?? ''} 기준 · 서울 + 경기 4개 지역 (의정부/남양주/하남/구리)
                        {lastFetched && <span className="ml-2 text-xs text-slate-400">갱신: {lastFetched.toLocaleTimeString('ko-KR')}</span>}
                    </p>
                </div>
                <button
                    onClick={fetchData}
                    disabled={loading}
                    className="bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 text-white font-semibold px-5 py-2.5 rounded-xl flex items-center gap-2 transition self-start"
                >
                    {loading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
                    새로고침
                </button>
            </div>

            {apiKeyMissing && (
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex gap-3 items-start">
                    <AlertCircle className="text-amber-600 flex-shrink-0 mt-0.5" size={18} />
                    <div className="text-sm text-amber-900">
                        <p className="font-semibold mb-1">청약홈 API 키가 설정되지 않았습니다.</p>
                        <p>공공데이터포털에서 "한국부동산원_청약홈 청약일정정보" API를 신청한 후, 프로젝트 루트의 <code className="bg-amber-100 px-1.5 py-0.5 rounded">.env</code> 파일에 <code className="bg-amber-100 px-1.5 py-0.5 rounded">APPLYHOME_API_KEY</code>를 설정하세요.</p>
                    </div>
                </div>
            )}

            {error && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex gap-3 items-start">
                    <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={18} />
                    <p className="text-sm text-red-900">{error}</p>
                </div>
            )}

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-4">
                <div>
                    <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">지역</h2>
                    <div className="flex flex-wrap gap-2">
                        {REGION_OPTIONS.map(opt => (
                            <ToggleChip key={opt.key} active={enabledRegions.has(opt.key)} onClick={() => toggleRegion(opt.key)}>
                                {opt.label}
                            </ToggleChip>
                        ))}
                    </div>
                </div>
                <div>
                    <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">순위</h2>
                    <div className="flex flex-wrap gap-2">
                        {(['FIRST', 'SECOND', 'REMAINDER'] as SubscriptionRank[]).map(r => (
                            <ToggleChip key={r} active={enabledRanks.has(r)} onClick={() => toggleRank(r)}>
                                {RANK_LABEL[r]}
                            </ToggleChip>
                        ))}
                    </div>
                </div>
            </div>

            {!loading && data && totalShown === 0 && !apiKeyMissing && (
                <div className="bg-slate-50 border border-slate-200 rounded-2xl p-12 text-center text-slate-500">
                    <Info size={36} className="mx-auto mb-3 opacity-60" />
                    <p className="font-medium">선택한 조건에 해당하는 청약이 없습니다.</p>
                    <p className="text-sm mt-1">필터를 조정하거나 새로고침해보세요.</p>
                </div>
            )}

            {sections.map(({ rank, items }) => (
                items.length > 0 && (
                    <section key={rank} className="space-y-3">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <span className={`px-3 py-1 rounded-full text-sm font-bold border ${RANK_COLOR[rank]}`}>
                                {RANK_LABEL[rank]}
                            </span>
                            <span className="text-slate-500 text-sm font-normal">{items.length}건</span>
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                            {items.map((item, idx) => (
                                <SubscriptionCard key={`${item.houseManageNo ?? item.name}-${idx}`} item={item} stage={rank} />
                            ))}
                        </div>
                    </section>
                )
            ))}
        </div>
    );
};

const ToggleChip = ({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) => (
    <button
        onClick={onClick}
        className={`px-4 py-1.5 rounded-full text-sm font-medium border transition ${
            active
                ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
        }`}
    >
        {children}
    </button>
);

const SubscriptionCard = ({ item, stage }: { item: SubscriptionItem; stage: SubscriptionRank }) => {
    const { begin, end } = pickStageDates(item, stage);
    const daysLeft = end ? daysUntil(end) : null;

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-3 hover:shadow-md transition">
            <div className="flex items-start justify-between gap-3">
                <h3 className="font-bold text-slate-900 leading-snug">{item.name}</h3>
                <div className="flex flex-col items-end gap-1 flex-shrink-0">
                    {item.houseType && (
                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 text-xs rounded-full">{item.houseType}</span>
                    )}
                </div>
            </div>

            {item.address && (
                <div className="flex items-start gap-1.5 text-sm text-slate-600">
                    <MapPin size={14} className="flex-shrink-0 mt-0.5 text-slate-400" />
                    <span className="leading-snug">{item.address}</span>
                </div>
            )}

            <div className="flex items-center gap-1.5 text-sm">
                <Calendar size={14} className="text-slate-400" />
                <span className="text-slate-700">
                    {fmtDate(begin)} ~ {fmtDate(end)}
                </span>
                {daysLeft !== null && daysLeft >= 0 && (
                    <span className={`ml-auto px-2 py-0.5 rounded-full text-xs font-bold ${
                        daysLeft <= 1 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                    }`}>
                        D-{daysLeft}
                    </span>
                )}
            </div>

            {item.totalSupplyHouseholds !== null && (
                <div className="flex items-center gap-1.5 text-sm text-slate-600">
                    <Building2 size={14} className="text-slate-400" />
                    <span>총 {item.totalSupplyHouseholds.toLocaleString()}세대</span>
                </div>
            )}

            {item.applyhomeUrl && (
                <a
                    href={item.applyhomeUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-blue-600 hover:text-blue-700 pt-1"
                >
                    청약홈에서 보기 <ExternalLink size={14} />
                </a>
            )}
        </div>
    );
};

function filterByRegion(items: SubscriptionItem[], enabled: Set<string>): SubscriptionItem[] {
    if (enabled.size === REGION_OPTIONS.length) return items;
    return items.filter(it => REGION_OPTIONS.some(opt => enabled.has(opt.key) && opt.predicate(it)));
}

function pickStageDates(item: SubscriptionItem, stage: SubscriptionRank): { begin: string | null; end: string | null } {
    switch (stage) {
        case 'FIRST': return { begin: item.firstRcptBegin, end: item.firstRcptEnd };
        case 'SECOND': return { begin: item.secondRcptBegin, end: item.secondRcptEnd };
        case 'REMAINDER': return { begin: item.remainderRcptBegin, end: item.remainderRcptEnd };
    }
}

function fmtDate(s: string | null): string {
    if (!s) return '-';
    return s.replace(/-/g, '.');
}

function daysUntil(isoDate: string): number {
    const target = new Date(isoDate + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.round((target.getTime() - today.getTime()) / 86400000);
}

export default SubscriptionsPage;
