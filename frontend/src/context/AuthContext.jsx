import { createContext, useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  const loadUser = useCallback(async () => {
    setAuthError(null);
    try {
      const response = await api.get('/api/v1/auth/me');
      setUser(response.data);
    } catch (err) {
      setUser(null);
      setAuthError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = async (email, password) => {
    const response = await api.post('/api/v1/auth/login', { email, password });
    setUser(response.data);
    return response.data;
  };

  const signup = async (name, email, password) => {
    const response = await api.post('/api/v1/auth/signup', { name, email, password });
    return response.data;
  };

  const logout = async () => {
    await api.post('/api/v1/auth/logout');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, authError, login, signup, logout, loadUser }}>
      {children}
    </AuthContext.Provider>
  );
}
