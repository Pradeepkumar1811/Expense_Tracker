import { createContext, useContext, useState, useCallback, useMemo } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('accessToken'));

  const isAuthenticated = !!token;

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password);
    setToken(data.accessToken);
    return data;
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setToken(null);
  }, []);

  const value = useMemo(
    () => ({ token, isAuthenticated, login, logout }),
    [token, isAuthenticated, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
