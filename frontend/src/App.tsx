import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';

import Dashboard from './pages/Dashboard';

import Budget from './pages/Budget';

import Assets from './pages/Assets';

import Tax from './pages/Tax';

import Settings from './pages/Settings';

import Investment from './pages/Investment';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/budget" element={<Budget />} />
          <Route path="/assets" element={<Assets />} />
          <Route path="/tax" element={<Tax />} />
          <Route path="/investment" element={<Investment />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
