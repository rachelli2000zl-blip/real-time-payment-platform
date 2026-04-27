import { Navigate, Route, Routes } from 'react-router-dom';
import NavBar from './components/NavBar';
import { useAuth } from './auth/AuthContext';
import OverviewPage from './pages/OverviewPage';
import ErrorsPage from './pages/ErrorsPage';
import DlqPage from './pages/DlqPage';
import ConfigPage from './pages/ConfigPage';

export default function App() {
  const auth = useAuth();

  if (!auth.isAuthenticated) {
    return (
      <div className="login-shell">
        <div className="login-card">
          <h1>Payments Stream Ops</h1>
          <p>Sign in with Amazon Cognito to access the control dashboard.</p>
          <button onClick={auth.login}>Sign in</button>
        </div>
      </div>
    );
  }

  if (!auth.token) {
    return <div className="login-shell">Loading...</div>;
  }

  return (
    <div className="app-shell">
      <NavBar username={auth.username} onLogout={auth.logout} />
      <main>
        <Routes>
          <Route path="/" element={<OverviewPage token={auth.token} />} />
          <Route path="/errors" element={<ErrorsPage token={auth.token} />} />
          <Route path="/dlq" element={<DlqPage token={auth.token} actor={auth.username} />} />
          <Route path="/config" element={<ConfigPage token={auth.token} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
