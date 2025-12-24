import React from 'react';
import { useNavigate } from 'react-router-dom';
import './PublicLanding.css';

/**
 * Public Landing Page - shown to unauthenticated users
 */
function PublicLanding() {
    const navigate = useNavigate();

    const handleSignIn = () => {
        navigate('/login');
    };

    const handleGetStarted = () => {
        navigate('/signup');
    };

    return (
        <div className="landing">
            {/* Hero Section */}
            <div className="hero">
                <div className="hero-content">
                    <div className="logo">
                        <span className="logo-icon">🏁</span>
                        <span className="logo-text">CodeDuel</span>
                    </div>
                    <button className="sign-in-btn" onClick={handleSignIn}>
                        Sign In
                    </button>
                </div>

                <div className="hero-center">
                    <h1 className="hero-title">
                        Race Against Friends to Solve Codeforces Problems
                    </h1>
                    <p className="hero-subtitle">
                        Compete in real-time coding challenges and improve your skills together
                    </p>
                    <button className="cta-btn" onClick={handleGetStarted}>
                        Get Started →
                    </button>
                </div>
            </div>

            {/* How it Works */}
            <div className="how-section">
                <h2 className="section-title">How It Works</h2>
                <div className="steps">
                    <div className="step">
                        <div className="step-icon">🚀</div>
                        <h3>Create a Room</h3>
                        <p>Start a coding challenge and invite your friends</p>
                    </div>
                    <div className="step">
                        <div className="step-icon">1</div>
                        <h3>Select Problem</h3>
                        <p>Choose difficulty and get a random Codeforces problem</p>
                    </div>
                    <div className="step">
                        <div className="step-icon">2</div>
                        <h3>Race to Solve</h3>
                        <p>First to solve wins! Track progress in real-time</p>
                    </div>
                </div>
            </div>

            {/* Footer */}
            <div className="footer">
                <p>© 2024 CodeDuel. Built for competitive programmers.</p>
            </div>
        </div>
    );
}

export default PublicLanding;
