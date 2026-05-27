import React, { useEffect, useState } from 'react';
import { Upload, FileSpreadsheet, CheckCircle2, AlertCircle, Loader2, Info } from 'lucide-react';
import { getCards, getSupportedCardCompanies, importStatement } from '../api/services';
import type { Card, CardCompany, StatementImportResponse, SupportedCardCompany } from '../types';
import { formatCurrency } from '../utils';

const StatementsPage = () => {
    const [cards, setCards] = useState<Card[]>([]);
    const [supported, setSupported] = useState<SupportedCardCompany[]>([]);
    const [selectedCardId, setSelectedCardId] = useState<number | ''>('');
    const [file, setFile] = useState<File | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [result, setResult] = useState<StatementImportResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [dragOver, setDragOver] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const [cs, sup] = await Promise.all([getCards(), getSupportedCardCompanies()]);
                setCards(cs);
                setSupported(sup);
            } catch (e) {
                console.error(e);
                setError('카드 목록을 불러올 수 없습니다.');
            }
        })();
    }, []);

    const supportedCodes: Set<CardCompany> = new Set(supported.map(s => s.code));
    const eligibleCards = cards.filter(c => c.company && supportedCodes.has(c.company));

    const handleFile = (f: File | null) => {
        setResult(null);
        setError(null);
        if (!f) {
            setFile(null);
            return;
        }
        if (!f.name.toLowerCase().endsWith('.xlsx')) {
            setError('xlsx 파일만 업로드 가능합니다.');
            setFile(null);
            return;
        }
        setFile(f);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!file || !selectedCardId) return;
        setIsUploading(true);
        setError(null);
        setResult(null);
        try {
            const res = await importStatement(Number(selectedCardId), file);
            setResult(res);
            setFile(null);
        } catch (e: any) {
            const msg = e?.response?.data?.message || e?.message || '업로드에 실패했습니다.';
            setError(msg);
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold text-slate-900">카드 명세서 가져오기</h1>
                <p className="text-slate-500 mt-1">카드사가 제공하는 월별 이용내역 엑셀(xlsx)을 업로드하면 가계부에 자동 등록됩니다.</p>
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex gap-3">
                <Info className="text-blue-600 flex-shrink-0 mt-0.5" size={18} />
                <div className="text-sm text-blue-900 space-y-1">
                    <p>
                        <strong>지원 카드사:</strong>{' '}
                        {supported.length === 0 ? '불러오는 중…' : supported.map(s => s.displayName).join(', ')}
                    </p>
                    <p>업로드된 거래는 <strong>미확정 상태</strong>로 등록되며, Gemini로 카테고리가 자동 분류됩니다. 분류 결과를 확인한 뒤 Budget 페이지에서 확정하세요.</p>
                    <p>같은 명세서를 다시 업로드해도 중복은 자동으로 스킵됩니다. 할부 거래는 매월 분할 결제로 자동 전개됩니다.</p>
                </div>
            </div>

            <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-5">
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-2">카드 선택</label>
                    <select
                        required
                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={selectedCardId}
                        onChange={e => setSelectedCardId(e.target.value ? Number(e.target.value) : '')}
                    >
                        <option value="">카드를 선택하세요</option>
                        {eligibleCards.map(c => {
                            const sup = supported.find(s => s.code === c.company);
                            return (
                                <option key={c.id} value={c.id}>
                                    {c.name} ({sup?.displayName ?? c.company})
                                </option>
                            );
                        })}
                    </select>
                    {eligibleCards.length === 0 && cards.length > 0 && (
                        <p className="text-xs text-amber-600 mt-2">
                            등록된 카드 중 카드사가 지정된 카드가 없습니다. Assets 페이지에서 카드를 생성할 때 카드사를 지정하세요.
                        </p>
                    )}
                    {cards.length === 0 && (
                        <p className="text-xs text-slate-500 mt-2">먼저 Assets 페이지에서 카드를 등록하세요.</p>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-2">명세서 파일 (.xlsx)</label>
                    <label
                        className={`block border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition ${
                            dragOver ? 'border-blue-500 bg-blue-50' : 'border-slate-300 hover:border-slate-400 bg-slate-50'
                        }`}
                        onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                        onDragLeave={() => setDragOver(false)}
                        onDrop={e => {
                            e.preventDefault();
                            setDragOver(false);
                            const f = e.dataTransfer.files?.[0] ?? null;
                            handleFile(f);
                        }}
                    >
                        <input
                            type="file"
                            accept=".xlsx"
                            className="hidden"
                            onChange={e => handleFile(e.target.files?.[0] ?? null)}
                        />
                        <FileSpreadsheet size={36} className="mx-auto text-slate-400 mb-2" />
                        {file ? (
                            <p className="text-sm font-medium text-slate-800">{file.name}</p>
                        ) : (
                            <>
                                <p className="text-sm text-slate-600">클릭하거나 파일을 끌어다 놓으세요</p>
                                <p className="text-xs text-slate-400 mt-1">.xlsx 만 지원합니다 (최대 10MB)</p>
                            </>
                        )}
                    </label>
                </div>

                <button
                    type="submit"
                    disabled={!file || !selectedCardId || isUploading}
                    className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-xl flex items-center justify-center gap-2 transition"
                >
                    {isUploading ? (
                        <>
                            <Loader2 size={18} className="animate-spin" /> 업로드 중…
                        </>
                    ) : (
                        <>
                            <Upload size={18} /> 가져오기 실행
                        </>
                    )}
                </button>
            </form>

            {error && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex gap-3 items-start">
                    <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={18} />
                    <p className="text-sm text-red-900">{error}</p>
                </div>
            )}

            {result && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-4">
                    <div className="flex items-center gap-2 text-green-700">
                        <CheckCircle2 size={20} />
                        <h2 className="font-bold text-lg">가져오기 완료</h2>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        <StatCard label="신규 등록" value={result.imported} color="green" />
                        <StatCard label="중복 스킵" value={result.skipped} color="slate" />
                        <StatCard label="실패" value={result.failed} color={result.failed > 0 ? 'red' : 'slate'} />
                        <StatCard label="미분류" value={result.unclassified} color={result.unclassified > 0 ? 'amber' : 'slate'} />
                    </div>

                    <p className="text-sm text-slate-600">
                        등록된 거래는 <strong>Budget 페이지의 예정 거래</strong>에서 확인하고 확정할 수 있습니다.
                    </p>

                    {result.summary.length > 0 && (
                        <div>
                            <h3 className="text-sm font-semibold text-slate-700 mb-2">미리보기 (최대 {result.summary.length}건)</h3>
                            <div className="overflow-x-auto border border-slate-200 rounded-xl">
                                <table className="w-full text-sm">
                                    <thead className="bg-slate-50 text-slate-600">
                                        <tr>
                                            <th className="px-3 py-2 text-left font-semibold">날짜</th>
                                            <th className="px-3 py-2 text-left font-semibold">가맹점</th>
                                            <th className="px-3 py-2 text-right font-semibold">금액</th>
                                            <th className="px-3 py-2 text-left font-semibold">카테고리</th>
                                            <th className="px-3 py-2 text-left font-semibold">할부</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {result.summary.map((item, idx) => (
                                            <tr key={idx} className="border-t border-slate-100">
                                                <td className="px-3 py-2 text-slate-700">{item.date}</td>
                                                <td className="px-3 py-2 text-slate-900">{item.merchant}</td>
                                                <td className="px-3 py-2 text-right font-medium text-slate-900">{formatCurrency(item.amount)}</td>
                                                <td className="px-3 py-2 text-slate-700">{item.categoryName}</td>
                                                <td className="px-3 py-2 text-slate-500">
                                                    {item.installmentMonths && item.installmentMonths > 1
                                                        ? `${item.installmentSeq ?? '?'}/${item.installmentMonths}`
                                                        : '일시불'}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

const StatCard = ({ label, value, color }: { label: string; value: number; color: 'green' | 'red' | 'amber' | 'slate' }) => {
    const colorMap: Record<string, string> = {
        green: 'bg-green-50 text-green-700 border-green-200',
        red: 'bg-red-50 text-red-700 border-red-200',
        amber: 'bg-amber-50 text-amber-700 border-amber-200',
        slate: 'bg-slate-50 text-slate-700 border-slate-200',
    };
    return (
        <div className={`rounded-xl border p-4 ${colorMap[color]}`}>
            <div className="text-xs font-semibold uppercase tracking-wider opacity-70">{label}</div>
            <div className="text-2xl font-bold mt-1">{value.toLocaleString()}</div>
        </div>
    );
};

export default StatementsPage;
