import React, { useState, useEffect } from 'react';
import './GameFilters.css';

/**
 * Component for selecting game filters (difficulty and algorithm tags)
 * Only editable by the room host
 */
function GameFilters({ onFiltersChange, isHost, initialFilters }) {
    const [minDifficulty, setMinDifficulty] = useState(initialFilters?.minDifficulty || 800);
    const [maxDifficulty, setMaxDifficulty] = useState(initialFilters?.maxDifficulty || 1500);
    const [selectedTags, setSelectedTags] = useState(initialFilters?.tags || []);

    // Sync state when initialFilters change (from WebSocket)
    useEffect(() => {
        if (initialFilters) {
            setMinDifficulty(initialFilters.minDifficulty || 800);
            setMaxDifficulty(initialFilters.maxDifficulty || 1500);
            setSelectedTags(initialFilters.tags || []);
        }
    }, [initialFilters]);

    // Common algorithm tags from backend
    const availableTags = [
        'dp', 'greedy', 'math', 'implementation', 'constructive algorithms',
        'data structures', 'brute force', 'binary search', 'dfs and similar',
        'graphs', 'trees', 'sortings', 'number theory', 'combinatorics',
        'two pointers', 'strings', 'geometry', 'bitmasks', 'dsu'
    ];

    const difficultyLevels = [
        { label: 'Beginner', min: 800, max: 1200 },
        { label: 'Easy', min: 1200, max: 1500 },
        { label: 'Medium', min: 1500, max: 1900 },
        { label: 'Hard', min: 1900, max: 2400 },
        { label: 'Expert', min: 2400, max: 3500 }
    ];

    const toggleTag = (tag) => {
        if (!isHost) return;

        const newTags = selectedTags.includes(tag)
            ? selectedTags.filter(t => t !== tag)
            : [...selectedTags, tag];

        setSelectedTags(newTags);
        notifyFiltersChange(minDifficulty, maxDifficulty, newTags);
    };

    const setDifficultyRange = (preset) => {
        if (!isHost) return;

        setMinDifficulty(preset.min);
        setMaxDifficulty(preset.max);
        notifyFiltersChange(preset.min, preset.max, selectedTags);
    };

    const notifyFiltersChange = (min, max, tags) => {
        if (onFiltersChange) {
            onFiltersChange({
                minDifficulty: min,
                maxDifficulty: max,
                tags: tags
            });
        }
    };

    return (
        <div className="game-filters">
            <h3 className="filters-title">
                🎯 Game Settings {!isHost && <span className="read-only">(Read-Only)</span>}
            </h3>

            {/* Difficulty Selection */}
            <div className="filter-section">
                <label className="filter-label">Difficulty Range:</label>
                <div className="difficulty-presets">
                    {difficultyLevels.map((level) => (
                        <button
                            key={level.label}
                            className={`difficulty-btn ${minDifficulty === level.min && maxDifficulty === level.max
                                ? 'active'
                                : ''
                                } ${!isHost ? 'disabled' : ''}`}
                            onClick={() => setDifficultyRange(level)}
                            disabled={!isHost}
                        >
                            {level.label}
                            <span className="difficulty-range">
                                ({level.min}-{level.max})
                            </span>
                        </button>
                    ))}
                </div>
            </div>

            {/* Algorithm Tags Selection */}
            <div className="filter-section">
                <label className="filter-label">
                    Algorithm Tags:
                    <span className="selected-count">
                        {selectedTags.length > 0 ? ` ${selectedTags.length} selected` : ' None selected'}
                    </span>
                </label>
                <div className="tags-grid">
                    {availableTags.map((tag) => (
                        <button
                            key={tag}
                            className={`tag-btn ${selectedTags.includes(tag) ? 'selected' : ''
                                } ${!isHost ? 'disabled' : ''}`}
                            onClick={() => toggleTag(tag)}
                            disabled={!isHost}
                        >
                            {selectedTags.includes(tag) && '✓ '}
                            {tag}
                        </button>
                    ))}
                </div>
            </div>

            {/* Summary */}
            {(selectedTags.length > 0 || minDifficulty !== 800 || maxDifficulty !== 1500) && (
                <div className="filter-summary">
                    <strong>Active Filters:</strong>
                    <div className="summary-content">
                        <span className="summary-item">
                            📊 Rating: {minDifficulty}-{maxDifficulty}
                        </span>
                        {selectedTags.length > 0 && (
                            <span className="summary-item">
                                🏷️ Tags: {selectedTags.join(', ')}
                            </span>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default GameFilters;
