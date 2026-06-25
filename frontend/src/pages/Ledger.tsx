import { useState, useEffect } from 'react';
import {
    getTransactions, createTransaction, updateTransaction, deleteTransaction, getCategories,
    getCards, getAssets, getTransactionsByCard
} from '../api/services';
import type {
    TransactionResponse, CategoryResponse, Card, Asset, TransactionRequest, PaymentMethod
} from '../types';
import { formatCurrency, evaluateExpr } from '../utils';
import { format, startOfMonth, endOfMonth } from 'date-fns';
import { Plus, CreditCard, Banknote, Landmark, X, Trash2, Edit2, Wallet, SlidersHorizontal } from 'lucide-react';

const LedgerPage = () => {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [cards, setCards] = useState<Card[]>([]);
    const [assets, setAssets] = useState<Asset[]>([]);

    // 필터 및 날짜 상태
    const [filterType, setFilterType] = useState<string>('ALL');
    const [useCustomDateRange, setUseCustomDateRange] = useState(false);
    const [filterStartDate, setFilterStartDate] = useState<string>(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
    const [filterEndDate, setFilterEndDate] = useState<string>(format(endOfMonth(new Date()), 'yyyy-MM-dd'));

    // UI 상태
    const [isTxFormOpen, setIsTxFormOpen] = useState(false);
    const [editingTx, setEditingTx] = useState<TransactionResponse | null>(null);
    const [amountExpr, setAmountExpr] = useState('');
    const [loading, setLoading] = useState(true);

    // 폼 상태
    const [txForm, setTxForm] = useState<TransactionRequest>({
        amount: 0,
        memo: '',
        categoryId: 0,
        paymentMethod: 'CARD',
        date: format(new Date(), 'yyyy-MM-dd'),
        isConfirmed: true,
        cardId: undefined,
        assetId: undefined,
        toAssetId: undefined
    });

    useEffect(() => {
        if (!useCustomDateRange) {
            setFilterStartDate(format(startOfMonth(currentDate), 'yyyy-MM-dd'));
            setFilterEndDate(format(endOfMonth(currentDate), 'yyyy-MM-dd'));
        }
    }, [currentDate, useCustomDateRange]);

    useEffect(() => {
        fetchData();
    }, [currentDate, filterType, filterStartDate, filterEndDate, useCustomDateRange]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const start = useCustomDateRange ? filterStartDate : format(startOfMonth(currentDate), 'yyyy-MM-dd');
            const end = useCustomDateRange ? filterEndDate : format(endOfMonth(currentDate), 'yyyy-MM-dd');

            const [cData, cardData, assetData] = await Promise.all([
                getCategories(),
                getCards(),
                getAssets()
            ]);

            setCategories(cData);
            setCards(cardData);
            setAssets(assetData);

            let tData: TransactionResponse[] = [];
            if (filterType === 'ALL') {
                tData = await getTransactions(start, end);
            } else if (filterType === 'CASH') {
                tData = await getTransactions(start, end, 'CASH');
            } else if (filterType === 'BANK_TRANSFER') {
                tData = await getTransactions(start, end, 'BANK_TRANSFER');
            } else if (filterType.startsWith('card_')) {
                const cardId = Number(filterType.split('_')[1]);
                tData = await getTransactionsByCard(cardId, start, end);
            }

            // 최신 날짜 순 정렬
            tData.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
            setTransactions(tData);
        } catch (error) {
            console.error("가계부 거래 데이터를 로드하지 못했습니다.", error);
        } finally {
            setLoading(false);
        }
    };

    const handleMonthChange = (offset: number) => {
        const newDate = new Date(currentDate);
        newDate.setMonth(newDate.getMonth() + offset);
        setCurrentDate(newDate);
    };

    // 수식 입력 검증
    const computedAmount = evaluateExpr(amountExpr);
    const hasOperator = /[+\-*/]/.test(amountExpr.replace(/^-/, ''));

    const openTxForm = (tx: TransactionResponse | null = null) => {
        if (tx) {
            setEditingTx(tx);
            setAmountExpr(String(Math.abs(tx.amount)));
            setTxForm({
                amount: Math.abs(tx.amount),
                memo: tx.memo || '',
                categoryId: tx.categoryId,
                paymentMethod: tx.paymentMethod,
                date: tx.date,
                isConfirmed: tx.isConfirmed,
                cardId: tx.cardId || undefined,
                assetId: tx.assetId || undefined,
                toAssetId: tx.toAssetId || undefined
            });
        } else {
            setEditingTx(null);
            setAmountExpr('');
            setTxForm({
                amount: 0,
                memo: '',
                categoryId: categories.length > 0 ? categories[0].id : 0,
                paymentMethod: 'CARD',
                date: format(new Date(), 'yyyy-MM-dd'),
                isConfirmed: true,
                cardId: cards.length > 0 ? cards[0].id : undefined,
                assetId: assets.find(a => a.isDefault)?.id || undefined,
                toAssetId: undefined
            });
        }
        setIsTxFormOpen(true);
    };

    const handleTxSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const finalAmount = computedAmount ?? Number(txForm.amount);
        if (!finalAmount || finalAmount <= 0) return alert("유효한 금액을 입력하세요.");

        const cat = categories.find(c => c.id === Number(txForm.categoryId));
        if (!cat) return alert("카테고리를 선택해 주세요.");

        // 이체 또는 저축/투자 거래 유효성 검사 (FR-006)
        const isTransfer = cat.type === 'TRANSFER' || cat.name === '저축/투자';
        if (isTransfer) {
            if (!txForm.assetId) return alert("이체 또는 저축/투자 거래에는 출금 자산(From)을 필수 선택해야 합니다.");
            if (!txForm.toAssetId) return alert("이체 또는 저축/투자 거래에는 입금 자산(To)을 필수 선택해야 합니다.");
            if (txForm.assetId === txForm.toAssetId) return alert("출금 자산과 입금 자산은 동일할 수 없습니다.");
        }

        const data: TransactionRequest = {
            ...txForm,
            amount: cat.type === 'INCOME' ? finalAmount : -finalAmount, // 지출은 마이너스 금액 처리
            categoryId: Number(txForm.categoryId),
            cardId: txForm.paymentMethod === 'CARD' ? (txForm.cardId ? Number(txForm.cardId) : undefined) : undefined,
            assetId: txForm.assetId ? Number(txForm.assetId) : undefined,
            toAssetId: isTransfer && txForm.toAssetId ? Number(txForm.toAssetId) : undefined
        };

        try {
            if (editingTx) {
                await updateTransaction(editingTx.id, data);
            } else {
                await createTransaction(data);
            }
            setIsTxFormOpen(false);
            fetchData();
        } catch (error: any) {
            console.error(error);
            alert(error.response?.data?.message || "거래 저장 중 오류가 발생했습니다.");
        }
    };

    const handleDeleteTx = async (id: number) => {
        if (!confirm("정말로 이 거래 내역을 삭제하시겠습니까?")) return;
        try {
            await deleteTransaction(id);
            fetchData();
        } catch (error) {
            console.error(error);
            alert("거래 삭제 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="space-y-6">
            {/* 상단 툴바 및 필터 */}
            <div className="flex flex-col md:flex-row md:items-center justify-between bg-white p-6 rounded-2xl shadow-sm border border-slate-100 gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
                        <Wallet className="text-blue-500" size={24} />
                        가계부 거래 관리
                    </h1>
                    <p className="text-sm text-slate-500">수동 거래 입력, 카드/현금/이체 내역 조회 및 편집을 지원합니다.</p>
                </div>
                
                <button
                    onClick={() => openTxForm()}
                    className="flex items-center justify-center gap-2 bg-blue-600 text-white px-5 py-3 rounded-xl font-semibold hover:bg-blue-700 hover:shadow-lg hover:shadow-blue-500/20 active:scale-95 transition-all duration-200"
                >
                    <Plus size={20} />
                    <span>거래 수동 입력</span>
                </button>
            </div>

            {/* 필터 바 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 space-y-4">
                <div className="flex items-center gap-2 text-slate-700 font-semibold mb-2">
                    <SlidersHorizontal size={18} />
                    <span>조회 조건 설정</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* 결제 수단 필터 */}
                    <div className="space-y-2">
                        <label className="text-xs font-semibold text-slate-400 uppercase">결제 수단</label>
                        <select
                            value={filterType}
                            onChange={(e) => setFilterType(e.target.value)}
                            className="block w-full rounded-xl border-slate-200 focus:border-blue-500 focus:ring-blue-500 text-sm py-2.5 px-3 border"
                        >
                            <option value="ALL">전체 내역</option>
                            <option value="CASH">현금 지출</option>
                            <option value="BANK_TRANSFER">계좌 이체</option>
                            <optgroup label="보유 카드">
                                {cards.map(c => (
                                    <option key={c.id} value={`card_${c.id}`}>{c.name}</option>
                                ))}
                            </optgroup>
                        </select>
                    </div>

                    {/* 기간 필터 */}
                    <div className="space-y-2">
                        <div className="flex justify-between items-center">
                            <label className="text-xs font-semibold text-slate-400 uppercase">조회 기간</label>
                            <button
                                onClick={() => setUseCustomDateRange(!useCustomDateRange)}
                                className="text-xs text-blue-500 font-semibold hover:underline"
                            >
                                {useCustomDateRange ? '월별 선택으로 전환' : '기간 직접 입력'}
                            </button>
                        </div>

                        {useCustomDateRange ? (
                            <div className="flex items-center space-x-2">
                                <input
                                    type="date"
                                    value={filterStartDate}
                                    onChange={(e) => setFilterStartDate(e.target.value)}
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2 px-3 border"
                                />
                                <span className="text-slate-400">~</span>
                                <input
                                    type="date"
                                    value={filterEndDate}
                                    onChange={(e) => setFilterEndDate(e.target.value)}
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2 px-3 border"
                                />
                            </div>
                        ) : (
                            <div className="flex items-center justify-between bg-slate-50 px-4 py-2.5 rounded-xl border">
                                <button onClick={() => handleMonthChange(-1)} className="p-1 hover:bg-slate-200 rounded text-slate-600">
                                    <ChevronLeft size={16} />
                                </button>
                                <span className="font-bold text-slate-700 text-sm">{format(currentDate, 'yyyy년 MM월')}</span>
                                <button onClick={() => handleMonthChange(1)} className="p-1 hover:bg-slate-200 rounded text-slate-600">
                                    <ChevronRight size={16} />
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 거래 목록 테이블 */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-100 text-left">
                        <thead className="bg-slate-50 text-slate-500 font-semibold text-xs uppercase tracking-wider">
                            <tr>
                                <th className="px-6 py-4">일자</th>
                                <th className="px-6 py-4">내용 (메모)</th>
                                <th className="px-6 py-4">분류</th>
                                <th className="px-6 py-4">결제 수단</th>
                                <th className="px-6 py-4">금액</th>
                                <th className="px-6 py-4 text-right">관리</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 text-sm text-slate-600">
                            {loading ? (
                                <tr>
                                    <td colSpan={6} className="text-center py-12">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                                    </td>
                                </tr>
                            ) : transactions.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="text-center py-12 text-slate-400">
                                        기록된 거래가 없습니다.
                                    </td>
                                </tr>
                            ) : (
                                transactions.map(tx => {
                                    const isExpense = tx.amount < 0;
                                    return (
                                        <tr key={tx.id} className="hover:bg-slate-50/50 transition-colors">
                                            <td className="px-6 py-4 font-medium">{tx.date}</td>
                                            <td className="px-6 py-4">
                                                <div>
                                                    <span className="font-bold text-slate-800">{tx.memo || tx.categoryName}</span>
                                                    {tx.assetName && (
                                                        <p className="text-xxs text-slate-400 mt-0.5">
                                                            {tx.toAssetName ? `${tx.assetName} ➔ ${tx.toAssetName}` : `연결 자산: ${tx.assetName}`}
                                                        </p>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="bg-slate-100 text-slate-700 px-2.5 py-1 rounded-full text-xs font-semibold">
                                                    {tx.categoryName}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex items-center gap-1.5 text-xs font-medium">
                                                    {tx.paymentMethod === 'CARD' ? (
                                                        <>
                                                            <CreditCard size={14} className="text-blue-500" />
                                                            <span>카드 ({tx.cardName})</span>
                                                        </>
                                                    ) : tx.paymentMethod === 'CASH' ? (
                                                        <>
                                                            <Banknote size={14} className="text-emerald-500" />
                                                            <span>현금</span>
                                                        </>
                                                    ) : (
                                                        <>
                                                            <Landmark size={14} className="text-amber-500" />
                                                            <span>계좌 이체</span>
                                                        </>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 font-bold">
                                                <span className={isExpense ? "text-rose-500" : "text-emerald-500"}>
                                                    {isExpense ? '-' : '+'}{formatCurrency(Math.abs(tx.amount))}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <div className="flex items-center justify-end gap-2">
                                                    <button
                                                        onClick={() => openTxForm(tx)}
                                                        className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    >
                                                        <Edit2 size={16} />
                                                    </button>
                                                    <button
                                                        onClick={() => handleDeleteTx(tx.id)}
                                                        className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                    >
                                                        <Trash2 size={16} />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 수동 거래 입력 / 수정 모달 */}
            {isTxFormOpen && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
                        <div className="px-6 py-5 bg-slate-900 text-white flex justify-between items-center">
                            <h2 className="font-bold text-lg">{editingTx ? '거래 내역 수정' : '새 거래 수동 등록'}</h2>
                            <button onClick={() => setIsTxFormOpen(false)} className="text-slate-400 hover:text-white transition-colors">
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleTxSubmit} className="p-6 space-y-4">
                            {/* 날짜 */}
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-400 uppercase">거래 일자</label>
                                <input
                                    type="date"
                                    value={txForm.date}
                                    onChange={(e) => setTxForm({ ...txForm, date: e.target.value })}
                                    required
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                />
                            </div>

                            {/* 금액 수식 입력 필드 */}
                            <div className="space-y-1">
                                <div className="flex justify-between items-center">
                                    <label className="text-xs font-bold text-slate-400 uppercase">금액 (수식 지원)</label>
                                    {hasOperator && computedAmount !== null && (
                                        <span className="text-xs text-blue-500 font-semibold">
                                            연산 결과: {formatCurrency(computedAmount)}
                                        </span>
                                    )}
                                </div>
                                <input
                                    type="text"
                                    value={amountExpr}
                                    onChange={(e) => {
                                        setAmountExpr(e.target.value);
                                        const clean = evaluateExpr(e.target.value);
                                        if (clean !== null) {
                                            setTxForm(prev => ({ ...prev, amount: clean }));
                                        }
                                    }}
                                    placeholder="금액 또는 수식(예: 5000+4500) 입력"
                                    required
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500 font-semibold"
                                />
                            </div>

                            {/* 카테고리 */}
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-400 uppercase">카테고리</label>
                                <select
                                    value={txForm.categoryId}
                                    onChange={(e) => setTxForm({ ...txForm, categoryId: Number(e.target.value) })}
                                    required
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                >
                                    {categories.map(c => (
                                        <option key={c.id} value={c.id}>
                                            [{c.type === 'INCOME' ? '수입' : c.type === 'TRANSFER' ? '이체' : '지출'}] {c.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* 결제 수단 */}
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-400 uppercase">결제 수단</label>
                                <select
                                    value={txForm.paymentMethod}
                                    onChange={(e) => setTxForm({ ...txForm, paymentMethod: e.target.value as PaymentMethod })}
                                    required
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                >
                                    <option value="CARD">카드 결제</option>
                                    <option value="CASH">현금 사용</option>
                                    <option value="BANK_TRANSFER">계좌 이체</option>
                                </select>
                            </div>

                            {/* 결제수단이 카드일 경우 카드 선택 */}
                            {txForm.paymentMethod === 'CARD' && (
                                <div className="space-y-1">
                                    <label className="text-xs font-bold text-slate-400 uppercase">결제 카드</label>
                                    <select
                                        value={txForm.cardId || ''}
                                        onChange={(e) => setTxForm({ ...txForm, cardId: e.target.value ? Number(e.target.value) : undefined })}
                                        required
                                        className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                    >
                                        <option value="">카드를 선택해 주세요</option>
                                        {cards.map(c => (
                                            <option key={c.id} value={c.id}>{c.name}</option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            {/* 연결 자산 (출금 계좌) */}
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-400 uppercase">
                                    {categories.find(c => c.id === Number(txForm.categoryId))?.type === 'TRANSFER' ||
                                     categories.find(c => c.id === Number(txForm.categoryId))?.name === '저축/투자'
                                        ? '출금 자산 (From)' : '연결 자산 (출금)'}
                                </label>
                                <select
                                    value={txForm.assetId || ''}
                                    onChange={(e) => setTxForm({ ...txForm, assetId: e.target.value ? Number(e.target.value) : undefined })}
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                >
                                    <option value="">기본 결제자산 사용</option>
                                    {assets.map(a => (
                                        <option key={a.id} value={a.id}>{a.name} ({formatCurrency(a.balance)})</option>
                                    ))}
                                </select>
                            </div>

                            {/* 이체 / 저축투자일 경우 입금 계좌 (To) 노출 */}
                            {(categories.find(c => c.id === Number(txForm.categoryId))?.type === 'TRANSFER' ||
                              categories.find(c => c.id === Number(txForm.categoryId))?.name === '저축/투자') && (
                                <div className="space-y-1">
                                    <label className="text-xs font-bold text-slate-400 uppercase">입금 자산 (To)</label>
                                    <select
                                        value={txForm.toAssetId || ''}
                                        onChange={(e) => setTxForm({ ...txForm, toAssetId: e.target.value ? Number(e.target.value) : undefined })}
                                        required
                                        className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                    >
                                        <option value="">입금할 자산을 선택하세요</option>
                                        {assets.map(a => (
                                            <option key={a.id} value={a.id}>{a.name} ({formatCurrency(a.balance)})</option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            {/* 메모 */}
                            <div className="space-y-1">
                                <label className="text-xs font-bold text-slate-400 uppercase">메모 (가맹점/세부내용)</label>
                                <input
                                    type="text"
                                    value={txForm.memo}
                                    onChange={(e) => setTxForm({ ...txForm, memo: e.target.value })}
                                    placeholder="상세 내용을 적어주세요."
                                    className="block w-full rounded-xl border-slate-200 text-sm py-2.5 px-3 border focus:ring-blue-500"
                                />
                            </div>

                            {/* 제출 버튼 */}
                            <button
                                type="submit"
                                className="w-full py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 hover:shadow-lg active:scale-95 transition-all duration-200 mt-6"
                            >
                                저장하기
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

// Chevron 아이콘 대응용 내부 소형 컴포넌트
const ChevronLeft = ({ size }: { size: number }) => <ChevronLeftIcon size={size} />;
const ChevronRight = ({ size }: { size: number }) => <ChevronRightIcon size={size} />;
import { ChevronLeft as ChevronLeftIcon, ChevronRight as ChevronRightIcon } from 'lucide-react';

export default LedgerPage;
