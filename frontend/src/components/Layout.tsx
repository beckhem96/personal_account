import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Wallet, CreditCard, Calculator, Menu, X, LogOut, User, Settings, TrendingUp } from 'lucide-react';
import { cn } from '../utils';

interface LayoutProps {
  children: React.ReactNode;
}

const SidebarItem = ({ to, icon: Icon, label, active, onClick }: { to: string; icon: any; label: string; active: boolean; onClick?: () => void }) => (
  <Link
    to={to}
    onClick={onClick}
    className={cn(
      "flex items-center space-x-3 px-4 py-3 rounded-xl transition-all duration-200 group",
      active 
        ? "bg-blue-600 text-white shadow-lg shadow-blue-900/50" 
        : "text-slate-400 hover:bg-slate-800 hover:text-white"
    )}
  >
    <Icon size={20} className={cn("transition-transform group-hover:scale-110", active && "scale-110")} />
    <span className="font-medium text-sm tracking-wide">{label}</span>
  </Link>
);

const Layout = ({ children }: LayoutProps) => {
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/budget', icon: CreditCard, label: 'Budget & Ledger' },
    { to: '/assets', icon: Wallet, label: 'Assets' },
    { to: '/tax', icon: Calculator, label: 'Tax Simulation' },
    { to: '/investment', icon: TrendingUp, label: 'Investment Sim' },
    { to: '/settings', icon: Settings, label: 'Settings' },
  ];

  const toggleMenu = () => setIsMobileMenuOpen(!isMobileMenuOpen);
  const closeMenu = () => setIsMobileMenuOpen(false);

  const SidebarContent = () => (
    <div className="flex flex-col h-full bg-slate-900 text-white">
        <div className="p-8 pb-4">
            <div className="flex items-center gap-2 mb-8">
                <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center font-bold text-white">F</div>
                <h1 className="text-xl font-bold tracking-tight text-white">Finance.AI</h1>
            </div>
            
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Menu</div>
            <nav className="space-y-1">
                {navItems.map((item) => (
                    <SidebarItem
                        key={item.to}
                        to={item.to}
                        icon={item.icon}
                        label={item.label}
                        active={location.pathname === item.to}
                        onClick={closeMenu}
                    />
                ))}
            </nav>
        </div>

        <div className="mt-auto p-6 border-t border-slate-800">
            <button className="flex items-center space-x-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white transition-colors w-full">
                <LogOut size={20} />
                <span className="font-medium text-sm">Sign Out</span>
            </button>
        </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* Desktop Sidebar */}
      <aside className="w-72 flex-shrink-0 hidden md:block shadow-xl z-20">
        <SidebarContent />
      </aside>

      {/* Mobile Sidebar (Overlay & Drawer) */}
      {isMobileMenuOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
            <div className="fixed inset-0 bg-black/60 backdrop-blur-sm" onClick={closeMenu} />
            <aside className="relative w-72 h-full shadow-2xl animate-in slide-in-from-left duration-300">
                <SidebarContent />
                <button onClick={closeMenu} className="absolute top-4 right-4 text-slate-400 hover:text-white">
                    <X size={24} />
                </button>
            </aside>
        </div>
      )}

      {/* Main Content */}
      <main className="flex-1 flex flex-col h-screen overflow-hidden">
        {/* Mobile Header */}
        <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 px-6 py-4 flex items-center justify-between md:hidden sticky top-0 z-10">
            <span className="font-bold text-lg text-slate-800">Finance.AI</span>
            <button onClick={toggleMenu} className="p-2 rounded-lg hover:bg-slate-100 transition-colors">
                <Menu className="w-6 h-6 text-slate-600" />
            </button>
        </header>
        
        {/* Desktop Header (Optional User Profile) */}
        <header className="hidden md:flex bg-white/50 backdrop-blur-sm border-b border-slate-200 px-8 py-4 justify-between items-center">
             <div className="text-sm text-slate-500">
                Welcome back, User
             </div>
             <div className="flex items-center space-x-4">
                <div className="w-8 h-8 bg-slate-200 rounded-full flex items-center justify-center text-slate-500">
                    <User size={18} />
                </div>
             </div>
        </header>

        <div className="flex-1 overflow-y-auto p-4 md:p-8">
          <div className="max-w-7xl mx-auto space-y-8 animate-in fade-in duration-500">
            {children}
          </div>
        </div>
      </main>
    </div>
  );
};

export default Layout;
