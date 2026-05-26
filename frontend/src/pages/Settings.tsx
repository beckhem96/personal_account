import React, { useState, useEffect } from 'react';
import { getCategories, createCategory, updateCategory, deleteCategory, getCards, createCard, deleteCard } from '../api/services';
import type { CategoryResponse, CategoryRequest, TransactionType, Card, CardRequest, CardType } from '../types';
import { cn } from '../utils';
import { Plus, Edit2, Trash2, X, CreditCard, Tag } from 'lucide-react';

const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
    INCOME: '수입',
    EXPENSE: '지출',
    TRANSFER: '이체',
};

const TRANSACTION_TYPE_COLORS: Record<TransactionType, string> = {
    INCOME: 'bg-green-50 text-green-700 border-green-100',
    EXPENSE: 'bg-red-50 text-red-700 border-red-100',
    TRANSFER: 'bg-blue-50 text-blue-700 border-blue-100',
};

const SettingsPage = () => {
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [cards, setCards] = useState<Card[]>([]);

    // Category Modal
    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<CategoryResponse | null>(null);
    const [categoryForm, setCategoryForm] = useState<CategoryRequest>({ name: '', type: 'EXPENSE' });
    const [categoryError, setCategoryError] = useState('');

    // Card Modal
    const [isCardModalOpen, setIsCardModalOpen] = useState(false);
    const [cardForm, setCardForm] = useState<CardRequest>({ name: '', type: 'CREDIT' });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [catData, cardData] = await Promise.all([getCategories(), getCards()]);
            setCategories(catData);
            setCards(cardData);
        } catch (error) {
            console.error(error);
        }
    };

    // Category handlers
    const openCategoryCreate = () => {
        setEditingCategory(null);
        setCategoryForm({ name: '', type: 'EXPENSE' });
        setCategoryError('');
        setIsCategoryModalOpen(true);
    };

    const openCategoryEdit = (cat: CategoryResponse) => {
        setEditingCategory(cat);
        setCategoryForm({ name: cat.name, type: cat.type });
        setCategoryError('');
        setIsCategoryModalOpen(true);
    };

    const handleCategorySubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setCategoryError('');
        try {
            if (editingCategory) {
                await updateCategory(editingCategory.id, categoryForm);
            } else {
                await createCategory(categoryForm);
            }
            setIsCategoryModalOpen(false);
            fetchData();
        } catch (error: any) {
            const msg = error?.response?.data?.message || error?.response?.data || '카테고리 저장에 실패했습니다.';
            setCategoryError(typeof msg === 'string' ? msg : '카테고리 저장에 실패했습니다.');
        }
    };

    const handleDeleteCategory = async (id: number) => {
        if (!confirm('이 카테고리를 삭제하시겠습니까?')) return;
        try {
            await deleteCategory(id);
            fetchData();
        } catch (error: any) {
            const msg = error?.response?.data?.message || error?.response?.data || '카테고리 삭제에 실패했습니다.';
            alert(typeof msg === 'string' ? msg : '카테고리 삭제에 실패했습니다.');
        }
    };

    // Card handlers
    const handleCardSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await createCard(cardForm);
            setIsCardModalOpen(false);
            setCardForm({ name: '', type: 'CREDIT' });
            fetchData();
        } catch (error) {
            alert('카드 추가에 실패했습니다.');
        }
    };

    const handleDeleteCard = async (id: number) => {
        if (!confirm('이 카드를 삭제하시겠습니까?')) return;
        try {
            await deleteCard(id);
            fetchData();
        } catch (error) {
            alert('카드 삭제에 실패했습니다.');
        }
    };

    const groupedCategories = (['INCOME', 'EXPENSE', 'TRANSFER'] as TransactionType[]).map(type => ({
        type,
        label: TRANSACTION_TYPE_LABELS[type],
        items: categories.filter(c => c.type === type),
    }));

    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Settings</h1>
                <p className="text-slate-500 mt-1">카테고리와 카드를 관리합니다.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Categories Section */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                        <div className="flex items-center gap-2">
                            <Tag size={20} className="text-slate-600" />
                            <h2 className="text-lg font-bold text-slate-800">Categories</h2>
                        </div>
                        <button
                            onClick={openCategoryCreate}
                            className="text-blue-600 hover:text-blue-700 text-sm font-semibold flex items-center"
                        >
                            <Plus size={16} className="mr-1" /> 추가
                        </button>
                    </div>

                    <div className="p-6 space-y-6">
                        {groupedCategories.map(group => (
                            <div key={group.type}>
                                <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">
                                    {group.label} ({group.items.length})
                                </h3>
                                <div className="space-y-2">
                                    {group.items.map(cat => (
                                        <div
                                            key={cat.id}
                                            className="flex items-center justify-between px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-colors group"
                                        >
                                            <div className="flex items-center gap-3">
                                                <span className={cn(
                                                    "px-2.5 py-1 rounded-full text-xs font-semibold border",
                                                    TRANSACTION_TYPE_COLORS[cat.type]
                                                )}>
                                                    {TRANSACTION_TYPE_LABELS[cat.type]}
                                                </span>
                                                <span className="font-medium text-slate-800">{cat.name}</span>
                                            </div>
                                            <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <button
                                                    onClick={() => openCategoryEdit(cat)}
                                                    className="p-2 hover:bg-slate-200 rounded-lg text-slate-400 hover:text-slate-700 transition-colors"
                                                >
                                                    <Edit2 size={14} />
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteCategory(cat.id)}
                                                    className="p-2 hover:bg-red-50 rounded-lg text-slate-400 hover:text-red-600 transition-colors"
                                                >
                                                    <Trash2 size={14} />
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                    {group.items.length === 0 && (
                                        <p className="text-sm text-slate-400 px-4 py-3">카테고리가 없습니다.</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Cards Section */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                        <div className="flex items-center gap-2">
                            <CreditCard size={20} className="text-slate-600" />
                            <h2 className="text-lg font-bold text-slate-800">Cards</h2>
                        </div>
                        <button
                            onClick={() => {
                                setCardForm({ name: '', type: 'CREDIT' });
                                setIsCardModalOpen(true);
                            }}
                            className="text-blue-600 hover:text-blue-700 text-sm font-semibold flex items-center"
                        >
                            <Plus size={16} className="mr-1" /> 추가
                        </button>
                    </div>

                    <div className="p-6 space-y-3">
                        {cards.map(card => (
                            <div
                                key={card.id}
                                className="flex items-center justify-between px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-colors group"
                            >
                                <div className="flex items-center gap-3">
                                    <span className={cn(
                                        "px-2.5 py-1 rounded-full text-xs font-semibold border",
                                        card.type === 'CREDIT'
                                            ? "bg-purple-50 text-purple-700 border-purple-100"
                                            : "bg-blue-50 text-blue-700 border-blue-100"
                                    )}>
                                        {card.type === 'CREDIT' ? '신용' : '체크'}
                                    </span>
                                    <span className="font-medium text-slate-800">{card.name}</span>
                                </div>
                                <button
                                    onClick={() => handleDeleteCard(card.id)}
                                    className="p-2 hover:bg-red-50 rounded-lg text-slate-400 hover:text-red-600 transition-colors opacity-0 group-hover:opacity-100"
                                >
                                    <Trash2 size={14} />
                                </button>
                            </div>
                        ))}
                        {cards.length === 0 && (
                            <div className="py-12 text-center text-slate-400 flex flex-col items-center">
                                <CreditCard size={48} className="mb-4 opacity-20" />
                                <p>등록된 카드가 없습니다.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Category Modal */}
            {isCategoryModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsCategoryModalOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative animate-in zoom-in-95 duration-200">
                        <button onClick={() => setIsCategoryModalOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20} />
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">
                            {editingCategory ? '카테고리 수정' : '카테고리 추가'}
                        </h3>

                        <form onSubmit={handleCategorySubmit} className="space-y-5">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">이름</label>
                                <input
                                    type="text"
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={categoryForm.name}
                                    onChange={e => setCategoryForm({ ...categoryForm, name: e.target.value })}
                                    placeholder="예: 식비, 교통비"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">유형</label>
                                <select
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={categoryForm.type}
                                    onChange={e => setCategoryForm({ ...categoryForm, type: e.target.value as TransactionType })}
                                >
                                    <option value="EXPENSE">지출</option>
                                    <option value="INCOME">수입</option>
                                    <option value="TRANSFER">이체</option>
                                </select>
                            </div>
                            {categoryError && (
                                <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{categoryError}</p>
                            )}
                            <button type="submit" className="w-full bg-blue-600 text-white py-3.5 rounded-xl font-semibold hover:bg-blue-700 transition mt-2">
                                {editingCategory ? '수정' : '추가'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Card Modal */}
            {isCardModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsCardModalOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-8 relative animate-in zoom-in-95 duration-200">
                        <button onClick={() => setIsCardModalOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20} />
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">카드 추가</h3>

                        <form onSubmit={handleCardSubmit} className="space-y-5">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">카드 이름</label>
                                <input
                                    type="text"
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={cardForm.name}
                                    onChange={e => setCardForm({ ...cardForm, name: e.target.value })}
                                    placeholder="예: 삼성카드"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">유형</label>
                                <select
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={cardForm.type}
                                    onChange={e => setCardForm({ ...cardForm, type: e.target.value as CardType })}
                                >
                                    <option value="CREDIT">신용카드</option>
                                    <option value="CHECK">체크카드</option>
                                </select>
                            </div>
                            <button type="submit" className="w-full bg-slate-900 text-white py-3.5 rounded-xl font-semibold hover:bg-slate-800 transition mt-2">
                                추가
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SettingsPage;
