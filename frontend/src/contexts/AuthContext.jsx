import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext(null);

// Get backend URL from environment
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Authentication context provider
 * Manages user authentication state using httpOnly cookies
 * JWT tokens are stored in cookies by the backend (not accessible to JavaScript)
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Load user on mount - check if authenticated via token
    useEffect(() => {
        const initAuth = async () => {
            try {
                const token = localStorage.getItem('jwtToken');

                // Call /me endpoint with token in header
                const headers = {
                    'Content-Type': 'application/json'
                };

                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const response = await fetch(`${API_URL}/api/auth/me`, {
                    credentials: 'include', // Still send cookies if available
                    headers
                });

                if (response.ok) {
                    const userData = await response.json();
                    setUser(userData);
                } else {
                    // Not authenticated or token expired
                    setUser(null);
                    localStorage.removeItem('jwtToken');
                }
            } catch (error) {
                console.error('Auth initialization error:', error);
                setUser(null);
                localStorage.removeItem('jwtToken');
            } finally {
                setLoading(false);
            }
        };

        initAuth();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // Only run once on mount

    /**
     * Logout - call backend to clear cookie
     */
    const logout = async () => {
        try {
            await fetch(`${API_URL}/api/auth/logout`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            setUser(null);
        }
    };

    /**
     * Register with email and password
     * Backend sets httpOnly cookie automatically
     */
    const registerWithEmail = async (email, password) => {
        const response = await fetch(`${API_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // Allow cookies to be set
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Registration failed');
        }

        const data = await response.json();
        setUser(data.user);
        window.location.href = '/dashboard';
        return data;
    };

    /**
     * Login with email and password
     * Backend sets httpOnly cookie automatically
     */
    const loginWithEmail = async (email, password) => {
        const response = await fetch(`${API_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // Allow cookies to be set
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();
        setUser(data.user);
        window.location.href = '/dashboard';
        return data;
    };

    /**
     * Initiate Google OAuth login
     */
    const loginWithGoogle = () => {
        window.location.href = `${API_URL}/oauth2/authorization/google`;
    };

    const value = {
        user,
        loading,
        isAuthenticated: !!user,
        logout,
        loginWithGoogle,
        registerWithEmail,
        loginWithEmail,
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
