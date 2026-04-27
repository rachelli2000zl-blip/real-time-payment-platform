import { Navigate, Route, Routes } from 'react-router-dom';
import NavBar from './components/NavBar';
import { useAuth } from './auth/AuthContext';
import OverviewPage from './pages/OverviewPage';
import ErrorsPage from './pages/ErrorsPage';
import DlqPage from './pages/DlqPage';
import ConfigPage from './pages/ConfigPage';

export default function App() {
  const auth = useAuth();

  // Development-only: bypass Cognito authentication for local testing.
  // If no real auth token is available, use a mock token.
  const devToken = auth.token ?? 'dev-local-token';

  // If no real username is available, use a mock username.
  const devUsername = auth.username ?? 'local-dev';

  return (
    <div className="app-shell">
      <NavBar username={devUsername} onLogout={auth.logout} />
      <main>
        <Routes>
          <Route path="/" element={<OverviewPage token={devToken} />} />
          <Route path="/errors" element={<ErrorsPage token={devToken} />} />
          <Route path="/dlq" element={<DlqPage token={devToken} actor={devUsername} />} />
          <Route path="/config" element={<ConfigPage token={devToken} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
