import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext(null);

// Get backend URL from environment
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Authentication context provider
 * Manages user authentication state and JWT tokens
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [token, setToken] = useState(localStorage.getItem('auth_token'));

    // Load user on mount if token exists
    useEffect(() => {
        const initAuth = async () => {
            try {
                // Check for OAuth callback token in URL
                const params = new URLSearchParams(window.location.search);
                const urlToken = params.get('token');

                if (urlToken) {
                    // Token from OAuth callback
                    localStorage.setItem('auth_token', urlToken);
                    setToken(urlToken);
                    // Validate new token
                    const response = await fetch(`${API_URL}/api/auth/me`, {
                        headers: { 'Authorization': `Bearer ${urlToken}` }
                    });
                    if (response.ok) {
                        const userData = await response.json();
                        setUser(userData);
                        setLoading(false);
                        // Use setTimeout to ensure state is set before redirect
                        setTimeout(() => {
                            window.location.href = '/dashboard';
                        }, 100);
                        return; // Don't set loading to false again
                    } else {
                        localStorage.removeItem('auth_token');
                        setToken(null);
                    }
                } else {
                    // Check for existing token in localStorage
                    const storedToken = localStorage.getItem('auth_token');
                    if (storedToken) {
                        // Validate existing token
                        const response = await fetch(`${API_URL}/api/auth/me`, {
                            headers: { 'Authorization': `Bearer ${storedToken}` }
                        });
                        if (response.ok) {
                            const userData = await response.json();
                            setUser(userData);
                        } else {
                            localStorage.removeItem('auth_token');
                            setToken(null);
                        }
                    }
                }
            } catch (error) {
                console.error('Auth initialization error:', error);
                localStorage.removeItem('auth_token');
                setToken(null);
                setUser(null);
            } finally {
                setLoading(false);
            }
        };

        initAuth();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // Only run once on mount

    /**
     * Login with JWT token
     */
    const login = (jwtToken, userData) => {
        localStorage.setItem('auth_token', jwtToken);
        setToken(jwtToken);
        setUser(userData);
    };

    /**
     * Logout - clear token and user
     */
    const logout = () => {
        localStorage.removeItem('auth_token');
        setToken(null);
        setUser(null);
    };

    /**
     * Initiate Google OAuth login
     */
    const loginWithGoogle = () => {
        window.location.href = `${API_URL}/oauth2/authorization/google`;
    };

    const value = {
        user,
        token,
        loading,
        isAuthenticated: !!user,
        login,
        logout,
        loginWithGoogle,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook to use auth context
 */
export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
}
