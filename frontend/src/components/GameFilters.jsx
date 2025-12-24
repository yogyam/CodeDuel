import React, { useState, useRef, useEffect } from 'react';
import './GameFilters.css';

/**
 * Component for entering problem description with autocomplete suggestions
 * Only editable by the room host
 */
function GameFilters({ onFiltersChange, isHost, initialFilters }) {
    const [description, setDescription] = useState(initialFilters?.description || '');
    const [showSuggestions, setShowSuggestions] = useState(false);
    const textareaRef = useRef(null);

    // Sync state when initialFilters change (from WebSocket)
    useEffect(() => {
        if (initialFilters) {
            setDescription(initialFilters.description || '');
        }
    }, [initialFilters]);

    // Common algorithm patterns and topics for suggestions
    const suggestions = [
        'dynamic programming',
        'two pointers',
        'sliding window',
        'geometry',
        'trees',
        'graphs',
        'binary search',
        'greedy',
        'hash tables',
        'stacks',
        'queues',
        'heaps',
        'backtracking',
        'divide and conquer',
        'depth first search',
        'breadth first search',
        'sorting',
        'bit manipulation',
        'string matching',
        'dijkstra',
        'floyd-warshall',
        'union find',
        'segment trees',
        'fenwick tree'
    ];

    const handleDescriptionChange = (e) => {
        if (!isHost) return;
        const newDescription = e.target.value;
        setDescription(newDescription);

        // Notify parent component
        if (onFiltersChange) {
            onFiltersChange({ description: newDescription });
        }
    };

    const insertSuggestion = (suggestion) => {
        if (!isHost) return;

        const textarea = textareaRef.current;
        const cursorPos = textarea.selectionStart;
        const textBefore = description.substring(0, cursorPos);
        const textAfter = description.substring(cursorPos);

        // Add space if needed
        const needsSpace = textBefore.length > 0 && !textBefore.endsWith(' ');
        const newDescription = textBefore + (needsSpace ? ' ' : '') + suggestion + textAfter;

        setDescription(newDescription);
        setShowSuggestions(false);

        // Notify parent component
        if (onFiltersChange) {
            onFiltersChange({ description: newDescription });
        }

        // Focus back on textarea
        setTimeout(() => textarea.focus(), 0);
    };

    return (
        <div className="game-filters">
            <h3 className="filters-title">
                🎯 Problem Description {!isHost && <span className="read-only">(Read-Only)</span>}
            </h3>

            {/* Description Input */}
            <div className="filter-section">
                <label className="filter-label">
                    Describe the problem you want:
                </label>
                <textarea
                    ref={textareaRef}
                    className={`description-input ${!isHost ? 'disabled' : ''}`}
                    placeholder="Example: a tree problem with dynamic programming, or a graph traversal using BFS..."
                    value={description}
                    onChange={handleDescriptionChange}
                    onFocus={() => isHost && setShowSuggestions(true)}
                    onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                    disabled={!isHost}
                    rows={3}
                />
            </div>

            {/* Autocomplete Suggestions */}
            {isHost && showSuggestions && (
                <div className="filter-section">
                    <label className="filter-label">Quick Suggestions:</label>
                    <div className="tags-grid">
                        {suggestions.map((suggestion) => (
                            <button
                                key={suggestion}
                                className="tag-btn suggestion-chip"
                                onMouseDown={(e) => {
                                    e.preventDefault(); // Prevent textarea blur
                                    insertSuggestion(suggestion);
                                }}
                            >
                                + {suggestion}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* Summary */}
            {description && description.trim() && (
                <div className="filter-summary">
                    <strong>Your Request:</strong>
                    <div className="summary-content">
                        <span className="summary-item">
                            📝 {description}
                        </span>
                    </div>
                </div>
            )}
        </div>
    );
}

export default GameFilters;
