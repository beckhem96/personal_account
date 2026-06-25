import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';

import Dashboard from './pages/Dashboard';
import Budget from './pages/Budget';
import Ledger from './pages/Ledger';
import Assets from './pages/Assets';
import Tax from './pages/Tax';
import Settings from './pages/Settings';
import Investment from './pages/Investment';
import StockAnalysis from './pages/StockAnalysis';
import Housing from './pages/Housing';
import Statements from './pages/Statements';
import Subscriptions from './pages/Subscriptions';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/ledger" element={<Ledger />} />
          <Route path="/budget" element={<Budget />} />
          <Route path="/assets" element={<Assets />} />
          <Route path="/tax" element={<Tax />} />
          <Route path="/investment" element={<Investment />} />
          <Route path="/stocks" element={<StockAnalysis />} />
          <Route path="/housing" element={<Housing />} />
          <Route path="/statements" element={<Statements />} />
          <Route path="/subscriptions" element={<Subscriptions />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
