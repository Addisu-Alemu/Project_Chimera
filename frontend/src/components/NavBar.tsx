import React from 'react';

export type TabId = 'pipeline' | 'alerts' | 'ledger' | 'performance';

interface NavBarProps {
  activeTab: TabId;
  onTabChange: (tab: TabId) => void;
  pendingAlertCount: number;
}

const TABS: { id: TabId; label: string }[] = [
  { id: 'pipeline', label: 'Pipeline Monitor' },
  { id: 'alerts', label: 'Alert Inbox' },
  { id: 'ledger', label: 'Transaction Ledger' },
  { id: 'performance', label: 'Agent Performance' },
];

export const NavBar: React.FC<NavBarProps> = ({
  activeTab,
  onTabChange,
  pendingAlertCount,
}) => {
  return (
    <nav className="bg-slate-900 border-b border-slate-700 sticky top-0 z-50">
      <div className="max-w-screen-2xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center h-14">
          <div className="flex items-center gap-2 mr-8">
            <div className="w-7 h-7 bg-indigo-600 rounded-md flex items-center justify-center">
              <span className="text-white text-xs font-bold">C</span>
            </div>
            <span className="text-slate-100 font-semibold text-sm tracking-wide">
              Project Chimera
            </span>
          </div>
          <div className="flex gap-1">
            {TABS.map((tab) => (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`relative px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  activeTab === tab.id
                    ? 'bg-indigo-700 text-white'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                }`}
              >
                {tab.label}
                {tab.id === 'alerts' && pendingAlertCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-red-600 text-white text-xs font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1">
                    {pendingAlertCount > 99 ? '99+' : pendingAlertCount}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>
      </div>
    </nav>
  );
};
