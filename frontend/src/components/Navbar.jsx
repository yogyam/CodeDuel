import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Navbar.css';

/**
 * Navigation bar with logo and user menu
 */
function Navbar() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [showDropdown, setShowDropdown] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <nav className="navbar">
            <div className="nav-content">
                <div className="nav-logo" onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer' }}>
                    <span className="nav-icon">🏁</span>
                    <span className="nav-text">CodeDuel</span>
                </div>

                <div className="user-menu">
                    <button
                        className="user-btn"
                        onClick={() => setShowDropdown(!showDropdown)}
                    >
                        <span className="user-avatar">{user?.username?.[0]?.toUpperCase() || 'U'}</span>
                        <span className="user-name">{user?.username || 'User'}</span>
                        <span className="dropdown-arrow">▼</span>
                    </button>

                    {showDropdown && (
                        <div className="dropdown-menu">
                            <button className="dropdown-item">
                                <span className="item-icon">👤</span>
                                Account
                            </button>
                            <button className="dropdown-item" onClick={handleLogout}>
                                <span className="item-icon">🚪</span>
                                Logout
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
