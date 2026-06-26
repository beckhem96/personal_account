import React, { useEffect, useMemo, useState } from 'react';
import { Bell, RefreshCw, AlertCircle, Loader2, MapPin, Calendar, ExternalLink, Info, Building2, Landmark } from 'lucide-react';
import { getTodaySubscriptions, getLhSubscriptions, getShSubscriptions, getGhSubscriptions } from '../api/services';
import type {
    SubscriptionItem, SubscriptionRank, SubscriptionsResponse,
    LhNoticeItem, LhNoticesResponse, LhSupplyCategory,
} from '../types';

type View = 'APPLYHOME' | 'LH' | 'SH' | 'GH';

const REGION_OPTIONS: Array<{ key: string; label: string; token: string }> = [
    { key: 'SEOUL', label: '서울', token: '서울' },
    { key: 'UIJEONGBU', label: '의정부', token: '의정부' },
    { key: 'NAMYANGJU', label: '남양주', token: '남양주' },
    { key: 'HANAM', label: '하남', token: '하남' },
    { key: 'GURI', label: '구리', token: '구리' },
    { key: 'YONGIN', label: '용인', token: '용인' },
    { key: 'SUWON', label: '수원', token: '수원' },
    { key: 'GIMPO', label: '김포', token: '김포' },
];

const RANK_LABEL: Record<SubscriptionRank, string> = {
    SPECIAL: '특별공급',
    FIRST: '1순위',
    SECOND: '2순위',
    REMAINDER: '무순위',
};

const RANK_COLOR: Record<SubscriptionRank, string> = {
    SPECIAL: 'bg-rose-100 text-rose-800 border-rose-200',
    FIRST: 'bg-blue-100 text-blue-800 border-blue-200',
    SECOND: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    REMAINDER: 'bg-amber-100 text-amber-800 border-amber-200',
};

// 공고 상태 — 접수중(RECEIVING) / 공고중(ANNOUNCED, 접수 시작 전)
type SubStatus = 'RECEIVING' | 'ANNOUNCED';
const STATUS_OPTIONS: Array<{ key: SubStatus; label: string }> = [
    { key: 'RECEIVING', label: '접수중' },
    { key: 'ANNOUNCED', label: '공고중' },
];
const STATUS_LABEL: Record<SubStatus, string> = { RECEIVING: '접수중', ANNOUNCED: '공고중' };
const STATUS_COLOR: Record<SubStatus, string> = {
    RECEIVING: 'bg-green-100 text-green-700',
    ANNOUNCED: 'bg-blue-100 text-blue-700',
};

const SubscriptionsPage = () => {
    const [view, setView] = useState<View>('APPLYHOME');

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-2">
                        <Bell size={28} className="text-blue-600" /> 청약 일정
                    </h1>
                    <p className="text-slate-500 mt-1">
                        서울 + 경기 4개 지역 (의정부/남양주/하남/구리) · 접수 예정 + 진행중 공고
                    </p>
                </div>
                <div className="inline-flex rounded-xl bg-slate-100 p-1 self-start flex-wrap gap-1">
                    <TabButton active={view === 'APPLYHOME'} onClick={() => setView('APPLYHOME')} icon={Building2}>
                        청약홈 (APT)
                    </TabButton>
                    <TabButton active={view === 'LH'} onClick={() => setView('LH')} icon={Landmark}>
                        LH 공공
                    </TabButton>
                    <TabButton active={view === 'SH'} onClick={() => setView('SH')} icon={Building2}>
                        SH 공공
                    </TabButton>
                    <TabButton active={view === 'GH'} onClick={() => setView('GH')} icon={Building2}>
                        GH 공공
                    </TabButton>
                </div>
            </div>

            {view === 'APPLYHOME' && <ApplyhomeView />}
            {view === 'LH' && <LhView />}
            {view === 'SH' && <ShView />}
            {view === 'GH' && <GhView />}
        </div>
    );
};

const TabButton = ({ active, onClick, icon: Icon, children }: {
    active: boolean; onClick: () => void; icon: React.ElementType; children: React.ReactNode;
}) => (
    <button
        onClick={onClick}
        className={`px-4 py-2 rounded-lg text-sm font-semibold flex items-center gap-1.5 transition ${
            active ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
        }`}
    >
        <Icon size={16} /> {children}
    </button>
);

// ── 청약홈 (한국부동산원 APT 일반/무순위) ────────────────────────────────
const ApplyhomeView = () => {
    const [data, setData] = useState<SubscriptionsResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [enabledRegions, setEnabledRegions] = useState<Set<string>>(new Set(REGION_OPTIONS.map(r => r.key)));
    const [enabledRanks, setEnabledRanks] = useState<Set<SubscriptionRank>>(new Set(['SPECIAL', 'FIRST', 'SECOND', 'REMAINDER']));
    const [enabledStatuses, setEnabledStatuses] = useState<Set<SubStatus>>(new Set(['RECEIVING', 'ANNOUNCED']));
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

    useEffect(() => { fetchData(); }, []);

    const narrow = (items: SubscriptionItem[], stage: SubscriptionRank) =>
        filterByRegion(items, enabledRegions, applyhomeRegionText)
            .filter(it => enabledStatuses.has(applyhomeStatus(it, stage)));
    const filteredSpecial = useMemo(() => narrow(data?.special ?? [], 'SPECIAL'), [data, enabledRegions, enabledStatuses]);
    const filteredFirst = useMemo(() => narrow(data?.firstRank ?? [], 'FIRST'), [data, enabledRegions, enabledStatuses]);
    const filteredSecond = useMemo(() => narrow(data?.secondRank ?? [], 'SECOND'), [data, enabledRegions, enabledStatuses]);
    const filteredRemainder = useMemo(() => narrow(data?.remainder ?? [], 'REMAINDER'), [data, enabledRegions, enabledStatuses]);

    const sections: Array<{ rank: SubscriptionRank; items: SubscriptionItem[] }> = ([
        { rank: 'SPECIAL' as SubscriptionRank, items: filteredSpecial },
        { rank: 'FIRST' as SubscriptionRank, items: filteredFirst },
        { rank: 'SECOND' as SubscriptionRank, items: filteredSecond },
        { rank: 'REMAINDER' as SubscriptionRank, items: filteredRemainder },
    ]).filter(s => enabledRanks.has(s.rank));

    const totalShown = sections.reduce((a, s) => a + s.items.length, 0);
    const apiKeyMissing = data && !data.apiKeyConfigured;

    return (
        <div className="space-y-6">
            <Toolbar onRefresh={fetchData} loading={loading} lastFetched={lastFetched} asOf={data?.asOf} />

            {apiKeyMissing && (
                <KeyMissingNotice
                    serviceName="한국부동산원_청약홈 청약일정정보"
                    envVar="APPLYHOME_API_KEY"
                />
            )}
            {error && <ErrorNotice message={error} />}

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-4">
                <ChipGroup title="지역">
                    {REGION_OPTIONS.map(opt => (
                        <ToggleChip key={opt.key} active={enabledRegions.has(opt.key)} onClick={() => setEnabledRegions(toggle(enabledRegions, opt.key))}>
                            {opt.label}
                        </ToggleChip>
                    ))}
                </ChipGroup>
                <ChipGroup title="순위">
                    {(['SPECIAL', 'FIRST', 'SECOND', 'REMAINDER'] as SubscriptionRank[]).map(r => (
                        <ToggleChip key={r} active={enabledRanks.has(r)} onClick={() => setEnabledRanks(toggle(enabledRanks, r))}>
                            {RANK_LABEL[r]}
                        </ToggleChip>
                    ))}
                </ChipGroup>
                <ChipGroup title="상태">
                    {STATUS_OPTIONS.map(s => (
                        <ToggleChip key={s.key} active={enabledStatuses.has(s.key)} onClick={() => setEnabledStatuses(toggle(enabledStatuses, s.key))}>
                            {s.label}
                        </ToggleChip>
                    ))}
                </ChipGroup>
            </div>

            {!loading && data && totalShown === 0 && !apiKeyMissing && (
                <EmptyNotice />
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
                                <ApplyhomeCard key={`${item.houseManageNo ?? item.name}-${idx}`} item={item} stage={rank} />
                            ))}
                        </div>
                    </section>
                )
            ))}
        </div>
    );
};

// ── LH/SH/GH 공공분양·임대 통합 컴포넌트 ──────────────────────────────────
interface PublicAgencyViewProps {
    agencyName: string;
    apiKeyEnvVar: string;
    apiServiceName: string;
    fetchApi: () => Promise<LhNoticesResponse>;
    infoMessage?: string;
    detailLinkLabel: string;
}

const PublicAgencyView = ({
    agencyName,
    apiKeyEnvVar,
    apiServiceName,
    fetchApi,
    infoMessage,
    detailLinkLabel,
}: PublicAgencyViewProps) => {
    const [data, setData] = useState<LhNoticesResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [enabledRegions, setEnabledRegions] = useState<Set<string>>(new Set(REGION_OPTIONS.map(r => r.key)));
    const [disabledTypes, setDisabledTypes] = useState<Set<string>>(new Set());
    const [enabledStatuses, setEnabledStatuses] = useState<Set<SubStatus>>(new Set(['RECEIVING', 'ANNOUNCED']));
    const [lastFetched, setLastFetched] = useState<Date | null>(null);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetchApi();
            setData(res);
            setDisabledTypes(new Set()); // 새 데이터 로드 시 전체 선택
            setLastFetched(new Date());
        } catch (e: any) {
            setError(e?.response?.data?.message || e?.message || `${agencyName} 공고를 불러올 수 없습니다.`);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, [fetchApi]);

    // 공급유형 칩은 응답에 실제로 존재하는 유형명(AIS_TP_CD_NM)으로 동적 구성
    const availableTypes = useMemo(() => {
        const set = new Set<string>();
        [...(data?.sale ?? []), ...(data?.rent ?? [])].forEach(it => {
            if (it.supplyTypeName) set.add(it.supplyTypeName);
        });
        return Array.from(set).sort();
    }, [data]);

    const narrow = (items: LhNoticeItem[]) =>
        filterByType(filterByRegion(items, enabledRegions, lhRegionText), disabledTypes)
            .filter(it => enabledStatuses.has(lhStatus(it)));
    const sale = useMemo(() => narrow(data?.sale ?? []), [data, enabledRegions, disabledTypes, enabledStatuses]);
    const rent = useMemo(() => narrow(data?.rent ?? []), [data, enabledRegions, disabledTypes, enabledStatuses]);

    const totalShown = sale.length + rent.length;
    const apiKeyMissing = data && !data.apiKeyConfigured;

    return (
        <div className="space-y-6">
            <Toolbar onRefresh={fetchData} loading={loading} lastFetched={lastFetched} asOf={data?.asOf} />

            {apiKeyMissing && (
                <KeyMissingNotice
                    serviceName={apiServiceName}
                    envVar={apiKeyEnvVar}
                />
            )}
            {error && <ErrorNotice message={error} />}
            {!apiKeyMissing && infoMessage && (
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex gap-3 items-start">
                    <Info className="text-blue-600 flex-shrink-0 mt-0.5" size={18} />
                    <p className="text-sm text-blue-900">
                        {infoMessage}
                    </p>
                </div>
            )}

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-4">
                <ChipGroup title="지역">
                    {REGION_OPTIONS.map(opt => (
                        <ToggleChip key={opt.key} active={enabledRegions.has(opt.key)} onClick={() => setEnabledRegions(toggle(enabledRegions, opt.key))}>
                            {opt.label}
                        </ToggleChip>
                    ))}
                </ChipGroup>
                {availableTypes.length > 0 && (
                    <ChipGroup title="공급유형">
                        {availableTypes.map(t => (
                            <ToggleChip key={t} active={!disabledTypes.has(t)} onClick={() => setDisabledTypes(toggle(disabledTypes, t))}>
                                {t}
                            </ToggleChip>
                        ))}
                    </ChipGroup>
                )}
                <ChipGroup title="상태">
                    {STATUS_OPTIONS.map(s => (
                        <ToggleChip key={s.key} active={enabledStatuses.has(s.key)} onClick={() => setEnabledStatuses(toggle(enabledStatuses, s.key))}>
                            {s.label}
                        </ToggleChip>
                    ))}
                </ChipGroup>
            </div>

            {!loading && data && totalShown === 0 && !apiKeyMissing && <EmptyNotice />}

            <LhSection title="분양" category="SALE" items={sale} detailLinkLabel={detailLinkLabel} />
            <LhSection title="임대" category="RENT" items={rent} detailLinkLabel={detailLinkLabel} />
        </div>
    );
};

const LhView = () => (
    <PublicAgencyView
        agencyName="LH"
        apiKeyEnvVar="LH_API_KEY"
        apiServiceName="한국토지주택공사_분양임대공고문 조회 서비스"
        fetchApi={getLhSubscriptions}
        infoMessage="LH API는 공공데이터포털에서 별도 '활용신청'이 필요합니다. 결과가 비어 있으면 신청 승인 여부를 확인하세요. 목록에 접수기간이 없는 공고는 상세 링크에서 일정을 확인할 수 있습니다."
        detailLinkLabel="LH 청약센터에서 보기"
    />
);

const ShView = () => (
    <PublicAgencyView
        agencyName="SH"
        apiKeyEnvVar="MYHOME_API_KEY"
        apiServiceName="국토교통부 마이홈포털 공공주택 모집공고 조회 서비스"
        fetchApi={getShSubscriptions}
        infoMessage="SH 공고는 국토교통부 마이홈포털 API를 활용하여 수집됩니다. 목록에 접수기간이 없는 공고는 상세 링크에서 일정을 확인할 수 있습니다."
        detailLinkLabel="SH 청약센터에서 보기"
    />
);

const GhView = () => (
    <PublicAgencyView
        agencyName="GH"
        apiKeyEnvVar="MYHOME_API_KEY"
        apiServiceName="국토교통부 마이홈포털 공공주택 모집공고 조회 서비스"
        fetchApi={getGhSubscriptions}
        infoMessage="GH 공고는 국토교통부 마이홈포털 API를 활용하여 수집됩니다. 목록에 접수기간이 없는 공고는 상세 링크에서 일정을 확인할 수 있습니다."
        detailLinkLabel="GH 청약센터에서 보기"
    />
);

const LhSection = ({ title, category, items, detailLinkLabel }: { title: string; category: LhSupplyCategory; items: LhNoticeItem[]; detailLinkLabel?: string }) => {
    if (items.length === 0) return null;
    const color = category === 'SALE'
        ? 'bg-emerald-100 text-emerald-800 border-emerald-200'
        : 'bg-violet-100 text-violet-800 border-violet-200';
    return (
        <section className="space-y-3">
            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                <span className={`px-3 py-1 rounded-full text-sm font-bold border ${color}`}>{title}</span>
                <span className="text-slate-500 text-sm font-normal">{items.length}건</span>
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {items.map((item, idx) => (
                    <LhCard key={`${item.panId ?? item.name}-${idx}`} item={item} detailLinkLabel={detailLinkLabel} />
                ))}
            </div>
        </section>
    );
};

// ── 공통 UI ──────────────────────────────────────────────────────────────
const Toolbar = ({ onRefresh, loading, lastFetched, asOf }: {
    onRefresh: () => void; loading: boolean; lastFetched: Date | null; asOf?: string;
}) => (
    <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
            {asOf ?? ''} 기준
            {lastFetched && <span className="ml-2 text-xs text-slate-400">갱신: {lastFetched.toLocaleTimeString('ko-KR')}</span>}
        </p>
        <button
            onClick={onRefresh}
            disabled={loading}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 text-white font-semibold px-5 py-2.5 rounded-xl flex items-center gap-2 transition"
        >
            {loading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
            새로고침
        </button>
    </div>
);

const ChipGroup = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <div>
        <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">{title}</h2>
        <div className="flex flex-wrap gap-2">{children}</div>
    </div>
);

const ToggleChip = ({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) => (
    <button
        onClick={onClick}
        className={`px-4 py-1.5 rounded-full text-sm font-medium border transition ${
            active ? 'bg-blue-600 text-white border-blue-600 shadow-sm' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
        }`}
    >
        {children}
    </button>
);

const KeyMissingNotice = ({ serviceName, envVar }: { serviceName: string; envVar: string }) => (
    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex gap-3 items-start">
        <AlertCircle className="text-amber-600 flex-shrink-0 mt-0.5" size={18} />
        <div className="text-sm text-amber-900">
            <p className="font-semibold mb-1">API 키가 설정되지 않았습니다.</p>
            <p>공공데이터포털에서 "{serviceName}"를 신청한 후, 프로젝트 루트의 <code className="bg-amber-100 px-1.5 py-0.5 rounded">.env</code> 파일에 <code className="bg-amber-100 px-1.5 py-0.5 rounded">{envVar}</code>를 설정하세요.</p>
        </div>
    </div>
);

const ErrorNotice = ({ message }: { message: string }) => (
    <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex gap-3 items-start">
        <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={18} />
        <p className="text-sm text-red-900">{message}</p>
    </div>
);

const EmptyNotice = () => (
    <div className="bg-slate-50 border border-slate-200 rounded-2xl p-12 text-center text-slate-500">
        <Info size={36} className="mx-auto mb-3 opacity-60" />
        <p className="font-medium">선택한 조건에 해당하는 청약이 없습니다.</p>
        <p className="text-sm mt-1">필터를 조정하거나 새로고침해보세요.</p>
    </div>
);

const ApplyhomeCard = ({ item, stage }: { item: SubscriptionItem; stage: SubscriptionRank }) => {
    const { begin, end } = pickStageDates(item, stage);

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-3 hover:shadow-md transition">
            <div className="flex items-start justify-between gap-3">
                <h3 className="font-bold text-slate-900 leading-snug">{item.name}</h3>
                <div className="flex flex-col items-end gap-1 flex-shrink-0">
                    <StatusBadge status={applyhomeStatus(item, stage)} />
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
                <span className="text-slate-700">{fmtDate(begin)} ~ {fmtDate(end)}</span>
                <ScheduleBadge begin={begin} end={end} />
            </div>

            {item.totalSupplyHouseholds !== null && (
                <div className="flex items-center gap-1.5 text-sm text-slate-600">
                    <Building2 size={14} className="text-slate-400" />
                    <span>총 {item.totalSupplyHouseholds.toLocaleString()}세대</span>
                </div>
            )}

            {item.applyhomeUrl && <DetailLink href={item.applyhomeUrl} label="청약홈에서 보기" />}
        </div>
    );
};

const LhCard = ({ item, detailLinkLabel = "LH 청약센터에서 보기" }: { item: LhNoticeItem; detailLinkLabel?: string }) => {
    const daysLeft = item.rcptEnd ? daysUntil(item.rcptEnd) : null;
    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 space-y-3 hover:shadow-md transition">
            <div className="flex items-start justify-between gap-3">
                <h3 className="font-bold text-slate-900 leading-snug">{item.name}</h3>
                {item.supplyTypeName && (
                    <span className="px-2 py-0.5 bg-slate-100 text-slate-600 text-xs rounded-full flex-shrink-0">{item.supplyTypeName}</span>
                )}
            </div>

            {item.regionLabel && (
                <div className="flex items-start gap-1.5 text-sm text-slate-600">
                    <MapPin size={14} className="flex-shrink-0 mt-0.5 text-slate-400" />
                    <span className="leading-snug">{item.regionLabel}</span>
                </div>
            )}

            {item.noticeDate && (
                <div className="flex items-center gap-1.5 text-sm text-slate-600">
                    <Calendar size={14} className="text-slate-400" />
                    <span>공고일 {fmtDate(item.noticeDate)}</span>
                </div>
            )}

            {item.rcptEnd && (
                <div className="flex items-center gap-1.5 text-sm">
                    <Calendar size={14} className="text-slate-400" />
                    <span className="text-slate-700">마감 {fmtDate(item.rcptEnd)}</span>
                    {daysLeft !== null && daysLeft >= 0 && (
                        <span className={`ml-auto px-2 py-0.5 rounded-full text-xs font-bold ${daysLeft <= 1 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                            D-{daysLeft}
                        </span>
                    )}
                </div>
            )}

            <div className="flex items-center gap-1.5">
                <StatusBadge status={lhStatus(item)} />
                {item.noticeStatus && item.noticeStatus !== STATUS_LABEL[lhStatus(item)] && (
                    <span className="text-xs text-slate-400">{item.noticeStatus}</span>
                )}
            </div>

            {item.detailUrl && <DetailLink href={item.detailUrl} label={detailLinkLabel} />}
        </div>
    );
};

const StatusBadge = ({ status }: { status: SubStatus }) => (
    <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${STATUS_COLOR[status]}`}>
        {STATUS_LABEL[status]}
    </span>
);

// 접수 시작 전이면 '시작 D-N'(예정, 파랑), 진행중이면 '마감 D-N'(임박 빨강/여유 초록)
const ScheduleBadge = ({ begin, end }: { begin: string | null; end: string | null }) => {
    const toBegin = begin ? daysUntil(begin) : null;
    if (toBegin !== null && toBegin > 0) {
        return (
            <span className="ml-auto px-2 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-blue-700">
                시작 D-{toBegin}
            </span>
        );
    }
    const toEnd = end ? daysUntil(end) : null;
    if (toEnd !== null && toEnd >= 0) {
        return (
            <span className={`ml-auto px-2 py-0.5 rounded-full text-xs font-bold ${toEnd <= 1 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                마감 D-{toEnd}
            </span>
        );
    }
    return null;
};

const DetailLink = ({ href, label }: { href: string; label: string }) => (
    <a href={href} target="_blank" rel="noreferrer"
       className="inline-flex items-center gap-1.5 text-sm font-medium text-blue-600 hover:text-blue-700 pt-1">
        {label} <ExternalLink size={14} />
    </a>
);

// ── 유틸 ────────────────────────────────────────────────────────────────
function toggle<T>(set: Set<T>, key: T): Set<T> {
    const next = new Set(set);
    if (next.has(key)) next.delete(key); else next.add(key);
    return next;
}

function applyhomeRegionText(it: SubscriptionItem): string {
    return `${it.regionLabel ?? ''} ${it.address ?? ''}`;
}

function lhRegionText(it: LhNoticeItem): string {
    return `${it.regionLabel ?? ''} ${it.name ?? ''}`;
}

function filterByRegion<T>(items: T[], enabled: Set<string>, textOf: (it: T) => string): T[] {
    if (enabled.size === REGION_OPTIONS.length) return items;
    return items.filter(it => {
        const text = textOf(it);
        return REGION_OPTIONS.some(opt => enabled.has(opt.key) && text.includes(opt.token));
    });
}

function filterByType(items: LhNoticeItem[], disabled: Set<string>): LhNoticeItem[] {
    if (disabled.size === 0) return items;
    return items.filter(it => !disabled.has(it.supplyTypeName ?? ''));
}

function applyhomeStatus(item: SubscriptionItem, stage: SubscriptionRank): SubStatus {
    const { begin } = pickStageDates(item, stage);
    // 접수 시작일이 미래면 공고중, 그 외(진행중·시작일 미상)는 접수중
    return begin && daysUntil(begin) > 0 ? 'ANNOUNCED' : 'RECEIVING';
}

function lhStatus(item: LhNoticeItem): SubStatus {
    return item.noticeStatus === '접수중' ? 'RECEIVING' : 'ANNOUNCED';
}

function pickStageDates(item: SubscriptionItem, stage: SubscriptionRank): { begin: string | null; end: string | null } {
    switch (stage) {
        case 'SPECIAL': return { begin: item.specialRcptBegin, end: item.specialRcptEnd };
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
