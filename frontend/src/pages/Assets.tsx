import React, { useState, useEffect } from 'react';
import { getAssets, createAsset, updateAsset, getNetWorth, getCards, createCard, deleteCard } from '../api/services';
import type { AssetResponse, AssetType, NetWorthResponse, AssetRequest, Card, CardRequest, CardType } from '../types';
import { formatCurrency, cn } from '../utils';
import { Plus, Edit2, Building2, TrendingUp, DollarSign, X, CreditCard, Trash2 } from 'lucide-react';

const AssetsPage = () => {
    const [activeTab, setActiveTab] = useState<'ASSETS' | 'CARDS'>('ASSETS');
    const [assets, setAssets] = useState<AssetResponse[]>([]);
    const [cards, setCards] = useState<Card[]>([]);
    const [netWorth, setNetWorth] = useState<NetWorthResponse | null>(null);
    
    // Asset Form State
    const [isAssetFormOpen, setIsAssetFormOpen] = useState(false);
    const [editingAsset, setEditingAsset] = useState<AssetResponse | null>(null);
    const [assetFormData, setAssetFormData] = useState<AssetRequest>({
        type: 'CASH',
        name: '',
        balance: 0,
        purchasePrice: 0
    });

    // Card Form State
    const [isCardFormOpen, setIsCardFormOpen] = useState(false);
    const [cardFormData, setCardFormData] = useState<CardRequest>({
        name: '',
        type: 'CREDIT'
    });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [aData, nData, cData] = await Promise.all([
                getAssets(),
                getNetWorth(),
                getCards()
            ]);
            setAssets(aData);
            setNetWorth(nData);
            setCards(cData);
        } catch (error) {
            console.error(error);
        }
    };

    const handleAssetSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            if (editingAsset) {
                await updateAsset(editingAsset.id, assetFormData);
            } else {
                await createAsset(assetFormData);
            }
            setIsAssetFormOpen(false);
            setEditingAsset(null);
            setAssetFormData({ type: 'CASH', name: '', balance: 0, purchasePrice: 0 });
            fetchData();
        } catch (error) {
            alert('Failed to save asset');
        }
    };

    const handleCardSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await createCard(cardFormData);
            setIsCardFormOpen(false);
            setCardFormData({ name: '', type: 'CREDIT' });
            fetchData();
        } catch (error) {
            alert('Failed to save card');
        }
    };

    const handleDeleteCard = async (id: number) => {
        if (!confirm('Are you sure you want to delete this card?')) return;
        try {
            await deleteCard(id);
            fetchData();
        } catch (error) {
            alert('Failed to delete card');
        }
    };

    const openAssetEdit = (asset: AssetResponse) => {
        setEditingAsset(asset);
        setAssetFormData({
            type: asset.type,
            name: asset.name,
            balance: asset.balance,
            purchasePrice: asset.purchasePrice || 0
        });
        setIsAssetFormOpen(true);
    };

    const AssetSummaryCard = ({ label, value, colorClass, icon: Icon }: { label: string, value: string, colorClass: string, icon: any }) => (
        <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm flex items-center justify-between group hover:shadow-md transition-all">
            <div>
                <p className="text-slate-500 text-sm font-medium mb-1">{label}</p>
                <p className="text-2xl font-bold text-slate-900 tracking-tight">{value}</p>
            </div>
            <div className={cn("p-4 rounded-xl transition-colors", colorClass)}>
                <Icon size={24} />
            </div>
        </div>
    );

    return (
        <div className="space-y-8">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Assets & Cards</h1>
                    <p className="text-slate-500 mt-1">Manage your wealth, debts, and payment cards.</p>
                </div>
                
                <div className="flex bg-white p-1 rounded-xl border border-slate-200 shadow-sm">
                    <button 
                        onClick={() => setActiveTab('ASSETS')}
                        className={cn("px-4 py-2 rounded-lg text-sm font-medium transition-all", activeTab === 'ASSETS' ? "bg-slate-900 text-white shadow" : "text-slate-500 hover:text-slate-900")}
                    >
                        Assets
                    </button>
                    <button 
                        onClick={() => setActiveTab('CARDS')}
                        className={cn("px-4 py-2 rounded-lg text-sm font-medium transition-all", activeTab === 'CARDS' ? "bg-slate-900 text-white shadow" : "text-slate-500 hover:text-slate-900")}
                    >
                        Cards
                    </button>
                </div>
            </div>

            {activeTab === 'ASSETS' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                    {/* Net Worth Summary */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <AssetSummaryCard 
                            label="Total Assets" 
                            value={netWorth ? formatCurrency(netWorth.totalAssets) : '-'} 
                            colorClass="bg-blue-50 text-blue-600 group-hover:bg-blue-100"
                            icon={Building2}
                        />
                        <AssetSummaryCard 
                            label="Total Liabilities" 
                            value={netWorth ? formatCurrency(netWorth.totalLiabilities) : '-'} 
                            colorClass="bg-red-50 text-red-600 group-hover:bg-red-100"
                            icon={DollarSign}
                        />
                        <AssetSummaryCard 
                            label="Net Worth" 
                            value={netWorth ? formatCurrency(netWorth.netWorth) : '-'} 
                            colorClass="bg-green-50 text-green-600 group-hover:bg-green-100"
                            icon={TrendingUp}
                        />
                    </div>

                    {/* Assets List */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                        <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                            <h2 className="text-lg font-bold text-slate-800">Asset Portfolio</h2>
                            <button 
                                onClick={() => {
                                    setEditingAsset(null);
                                    setAssetFormData({ type: 'CASH', name: '', balance: 0, purchasePrice: 0 });
                                    setIsAssetFormOpen(true);
                                }}
                                className="text-blue-600 hover:text-blue-700 text-sm font-semibold flex items-center"
                            >
                                <Plus size={16} className="mr-1"/> Add Asset
                            </button>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-slate-50 text-slate-500 font-medium uppercase tracking-wider text-xs">
                                    <tr>
                                        <th className="px-6 py-4">Type</th>
                                        <th className="px-6 py-4">Name</th>
                                        <th className="px-6 py-4 text-right">Balance / Value</th>
                                        <th className="px-6 py-4 text-right">Return Rate</th>
                                        <th className="px-6 py-4 text-center">Action</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {assets.map(asset => (
                                        <tr key={asset.id} className="hover:bg-slate-50/80 transition-colors group">
                                            <td className="px-6 py-4">
                                                <span className={cn(
                                                    "px-2.5 py-1 rounded-full text-xs font-semibold border",
                                                    asset.type === 'DEBT' ? "bg-red-50 text-red-700 border-red-100" :
                                                    asset.type === 'STOCK' ? "bg-purple-50 text-purple-700 border-purple-100" :
                                                    "bg-blue-50 text-blue-700 border-blue-100"
                                                )}>
                                                    {asset.type}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 font-semibold text-slate-800">{asset.name}</td>
                                            <td className="px-6 py-4 text-right font-bold text-slate-900 text-base">
                                                {formatCurrency(asset.balance)}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                {asset.type === 'STOCK' && asset.returnRate !== undefined && asset.returnRate !== null ? (
                                                    <span className={cn(
                                                        "font-medium",
                                                        (asset.returnRate || 0) >= 0 ? "text-red-500" : "text-blue-500"
                                                    )}>
                                                        {(asset.returnRate || 0) > 0 && '+'}{asset.returnRate}%
                                                    </span>
                                                ) : <span className="text-slate-300">-</span>}
                                            </td>
                                            <td className="px-6 py-4 text-center">
                                                <button 
                                                    onClick={() => openAssetEdit(asset)}
                                                    className="p-2 hover:bg-slate-200 rounded-lg text-slate-400 hover:text-slate-700 transition-colors"
                                                >
                                                    <Edit2 size={16} />
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    {assets.length === 0 && (
                                        <tr><td colSpan={5} className="px-6 py-12 text-center text-slate-400">No assets found.</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'CARDS' && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                     <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
                        <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                            <h2 className="text-lg font-bold text-slate-800">My Cards</h2>
                            <button 
                                onClick={() => setIsCardFormOpen(true)}
                                className="bg-slate-900 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-slate-800 flex items-center shadow-lg shadow-slate-900/10"
                            >
                                <Plus size={16} className="mr-2"/> Add Card
                            </button>
                        </div>

                        <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {cards.map(card => (
                                <div key={card.id} className="bg-gradient-to-br from-slate-800 to-slate-900 rounded-2xl p-6 text-white shadow-xl relative group">
                                    <div className="flex justify-between items-start mb-8">
                                        <div className="bg-white/10 p-2 rounded-lg backdrop-blur-sm">
                                            <CreditCard size={24} className="text-white"/>
                                        </div>
                                        <span className="text-xs font-semibold bg-white/20 px-2 py-1 rounded text-white/90">
                                            {card.type}
                                        </span>
                                    </div>
                                    <div>
                                        <p className="text-slate-400 text-xs font-medium uppercase tracking-wider mb-1">Card Name</p>
                                        <h3 className="text-xl font-bold tracking-wide">{card.name}</h3>
                                    </div>
                                    <button 
                                        onClick={() => handleDeleteCard(card.id)}
                                        className="absolute top-4 right-4 text-white/30 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100"
                                    >
                                        <Trash2 size={18} />
                                    </button>
                                </div>
                            ))}
                             {cards.length === 0 && (
                                <div className="col-span-full py-12 text-center text-slate-400 flex flex-col items-center">
                                    <CreditCard size={48} className="mb-4 opacity-20"/>
                                    <p>No cards added yet.</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Asset Modal */}
            {isAssetFormOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsAssetFormOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative animate-in zoom-in-95 duration-200">
                        <button onClick={() => setIsAssetFormOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20}/>
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">{editingAsset ? 'Edit Asset' : 'Add New Asset'}</h3>
                        
                        <form onSubmit={handleAssetSubmit} className="space-y-5">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Type</label>
                                <select 
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={assetFormData.type}
                                    onChange={e => setAssetFormData({...assetFormData, type: e.target.value as AssetType})}
                                >
                                    <option value="CASH">Cash</option>
                                    <option value="SAVINGS">Savings</option>
                                    <option value="STOCK">Stock / Investment</option>
                                    <option value="DEBT">Debt / Loan</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Name</label>
                                <input 
                                    type="text" 
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={assetFormData.name}
                                    onChange={e => setAssetFormData({...assetFormData, name: e.target.value})}
                                    placeholder="e.g. Main Account"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Balance</label>
                                <input 
                                    type="number" 
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={assetFormData.balance}
                                    onChange={e => setAssetFormData({...assetFormData, balance: Number(e.target.value)})}
                                />
                            </div>
                            {assetFormData.type === 'STOCK' && (
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Purchase Price</label>
                                    <input 
                                        type="number" 
                                        className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                        value={assetFormData.purchasePrice}
                                        onChange={e => setAssetFormData({...assetFormData, purchasePrice: Number(e.target.value)})}
                                    />
                                </div>
                            )}
                            <button type="submit" className="w-full bg-blue-600 text-white py-3.5 rounded-xl font-semibold hover:bg-blue-700 transition mt-2">
                                Save Asset
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Card Modal */}
            {isCardFormOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity" onClick={() => setIsCardFormOpen(false)} />
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-8 relative animate-in zoom-in-95 duration-200">
                        <button onClick={() => setIsCardFormOpen(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-800">
                            <X size={20}/>
                        </button>
                        <h3 className="text-xl font-bold mb-6 text-slate-900">Add New Card</h3>
                        
                        <form onSubmit={handleCardSubmit} className="space-y-5">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Card Name</label>
                                <input 
                                    type="text" 
                                    required
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={cardFormData.name}
                                    onChange={e => setCardFormData({...cardFormData, name: e.target.value})}
                                    placeholder="e.g. Samsung Card"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-2">Type</label>
                                <select 
                                    className="w-full border border-slate-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={cardFormData.type}
                                    onChange={e => setCardFormData({...cardFormData, type: e.target.value as CardType})}
                                >
                                    <option value="CREDIT">Credit Card</option>
                                    <option value="CHECK">Check/Debit Card</option>
                                </select>
                            </div>
                            <button type="submit" className="w-full bg-slate-900 text-white py-3.5 rounded-xl font-semibold hover:bg-slate-800 transition mt-2">
                                Add Card
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AssetsPage;
