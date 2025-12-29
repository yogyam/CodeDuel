import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Navbar from './Navbar';
import './Dashboard.css';

/**
 * Dashboard - Home page after login
 * Shows welcome and quick actions to create or join room
 */
function Dashboard() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    // Extract token from URL after OAuth redirect
    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            // Store token in localStorage
            localStorage.setItem('jwtToken', token);

            // Clean up URL (remove token from query params)
            searchParams.delete('token');
            setSearchParams(searchParams);

            // Reload to update auth context
            window.location.reload();
        }
    }, [searchParams, setSearchParams]);

    return (
        <div className="dashboard">
            <Navbar />

            <div className="dashboard-content">
                <h1 className="welcome">Welcome, {user?.username}! 👋</h1>
                <p className="welcome-subtitle">Ready to race against your friends?</p>

                <div className="action-cards">
                    <div className="action-card" onClick={() => navigate('/room')}>
                        <div className="card-icon">🚀</div>
                        <h2>Create New Room</h2>
                        <p>Start a new coding challenge and invite friends</p>
                        <button className="card-btn">Create Room →</button>
                    </div>

                    <div className="action-card" onClick={() => navigate('/room')}>
                        <div className="card-icon">★</div>
                        <h2>Join Room</h2>
                        <p>Enter a room ID to join an existing challenge</p>
                        <button className="card-btn">Join Room →</button>
                    </div>
                </div>

                {/* Optional: Recent Activity */}
                <div className="recent-section">
                    <h3>Quick Tips</h3>
                    <ul className="tips-list">
                        <li>Share room IDs with friends to compete together</li>
                        <li>First to solve wins the race!</li>
                    </ul>
                </div>
            </div>
        </div>
    );
}

export default Dashboard;
