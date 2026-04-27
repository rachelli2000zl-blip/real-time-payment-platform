import { ReactNode, createContext, useContext, useEffect, useMemo, useState } from 'react';
import { buildLoginUrl, buildLogoutUrl, parseJwt, parseTokenFromHash } from './cognito';

type AuthContextValue = {
  token: string | null;
  username: string;
  isAuthenticated: boolean;
  login: () => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const TOKEN_STORAGE_KEY = 'dashboard_jwt';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY));

  useEffect(() => {
    const hashToken = parseTokenFromHash();
    if (hashToken) {
      localStorage.setItem(TOKEN_STORAGE_KEY, hashToken);
      setToken(hashToken);
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const claims = token ? parseJwt(token) : null;
    const username = String(claims?.['cognito:username'] ?? claims?.['email'] ?? 'dashboard-user');

    return {
      token,
      username,
      isAuthenticated: Boolean(token),
      login: () => {
        window.location.href = buildLoginUrl();
      },
      logout: () => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken(null);
        window.location.href = buildLogoutUrl();
      }
    };
  }, [token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
