import React, { Component } from 'react';

/**
 * Error Boundary Component
 * Catches JavaScript errors anywhere in the child component tree
 */
class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        // Log error to console in development
        if (import.meta.env.DEV) {
            console.error('Error caught by boundary:', error, errorInfo);
        }

        // In production, you would send this to an error reporting service
        // Example: Sentry.captureException(error, { extra: errorInfo });

        this.setState({
            error,
            errorInfo
        });
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{
                    minHeight: '100vh',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    padding: '2rem'
                }}>
                    <div style={{
                        background: 'white',
                        borderRadius: '16px',
                        padding: '3rem',
                        maxWidth: '600px',
                        textAlign: 'center',
                        boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
                    }}>
                        <h1 style={{ fontSize: '3rem', marginBottom: '1rem' }}>😢</h1>
                        <h2 style={{ color: '#333', marginBottom: '1rem' }}>Oops! Something went wrong</h2>
                        <p style={{ color: '#666', marginBottom: '2rem' }}>
                            We're sorry for the inconvenience. The application encountered an unexpected error.
                        </p>
                        <button
                            onClick={() => window.location.href = '/'}
                            style={{
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                color: 'white',
                                border: 'none',
                                padding: '1rem 2rem',
                                borderRadius: '8px',
                                fontSize: '1rem',
                                fontWeight: '600',
                                cursor: 'pointer'
                            }}
                        >
                            Return to Home
                        </button>
                        {import.meta.env.DEV && this.state.error && (
                            <details style={{ marginTop: '2rem', textAlign: 'left' }}>
                                <summary style={{ cursor: 'pointer', color: '#667eea' }}>
                                    Error Details (Development Only)
                                </summary>
                                <pre style={{
                                    background: '#f5f5f5',
                                    padding: '1rem',
                                    borderRadius: '4px',
                                    overflow: 'auto',
                                    fontSize: '0.875rem',
                                    marginTop: '1rem'
                                }}>
                                    {this.state.error.toString()}
                                    {this.state.errorInfo && this.state.errorInfo.componentStack}
                                </pre>
                            </details>
                        )}
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
