import React, { useState, useEffect, useCallback } from 'react';
import { NavBar } from './components/NavBar';
import { PipelineMonitor } from './components/PipelineMonitor';
import { AlertInbox } from './components/AlertInbox';
import { TransactionLedger } from './components/TransactionLedger';
import { AgentPerformance } from './components/AgentPerformance';
import { getAlerts } from './api/actService';
import type { TabId } from './components/NavBar';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabId>('pipeline');
  const [pendingAlertCount, setPendingAlertCount] = useState(0);

  const refreshPendingCount = useCallback(async () => {
    try {
      const alerts = await getAlerts();
      const pending = alerts.filter((a) => a.status === 'PENDING').length;
      setPendingAlertCount(pending);
    } catch (err) {
      console.error('[act-service] refreshPendingCount error:', err);
    }
  }, []);

  useEffect(() => {
    refreshPendingCount();
    const id = setInterval(refreshPendingCount, 10000);
    return () => clearInterval(id);
  }, [refreshPendingCount]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <NavBar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        pendingAlertCount={pendingAlertCount}
      />
      <main className="max-w-screen-2xl mx-auto">
        {activeTab === 'pipeline' && <PipelineMonitor />}
        {activeTab === 'alerts' && <AlertInbox />}
        {activeTab === 'ledger' && <TransactionLedger />}
        {activeTab === 'performance' && <AgentPerformance />}
      </main>
    </div>
  );
};

export default App;
