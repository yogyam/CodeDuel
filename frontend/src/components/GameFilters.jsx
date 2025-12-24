import React, { useState, useEffect } from 'react';
import './GameFilters.css';

/**
 * Component for selecting problem category, difficulty, and optional subtype
 * Replaces free-text description with structured dropdowns
 */
function GameFilters({ onFiltersChange, isHost, initialFilters }) {
    // Problem categories with their subtypes
    const categories = [
        {
            value: 'DYNAMIC_PROGRAMMING',
            label: 'Dynamic Programming',
            subtypes: ['Knapsack Variants', 'Longest Common Subsequence', 'String DP', 'State Machine DP', '2D Grid DP', 'Interval DP']
        },
        {
            value: 'GRAPHS',
            label: 'Graphs',
            subtypes: ['Shortest Path', 'Minimum Spanning Tree', 'Topological Sort', 'Connected Components', 'Cycle Detection', 'BFS/DFS Traversal']
        },
        {
            value: 'TREES',
            label: 'Trees',
            subtypes: ['Binary Search Tree', 'Tree Traversal', 'Lowest Common Ancestor', 'Path Sum Problems', 'Tree DP']
        },
        {
            value: 'GREEDY',
            label: 'Greedy',
            subtypes: ['Interval Scheduling', 'Activity Selection', 'Huffman Coding', 'Job Sequencing']
        },
        {
            value: 'BINARY_SEARCH',
            label: 'Binary Search',
            subtypes: ['Search in Sorted Array', 'Binary Search on Answer', 'Element Finding', 'Lower/Upper Bound']
        },
        {
            value: 'SORTING_SEARCHING',
            label: 'Sorting & Searching',
            subtypes: ['Custom Comparators', 'Two Pointers', 'Sliding Window', 'Merge Sort Applications']
        },
        {
            value: 'ARRAYS_HASHING',
            label: 'Arrays & Hashing',
            subtypes: ['Prefix Sum', 'Kadane\'s Algorithm', 'Hash Map Techniques', 'Subarray Problems']
        },
        {
            value: 'BACKTRACKING',
            label: 'Backtracking',
            subtypes: ['N-Queens Variants', 'Subset Generation', 'Permutations', 'Combination Sum']
        }
    ];

    const difficulties = [
        { value: 'EASY', label: 'Easy' },
        { value: 'MEDIUM', label: 'Medium' },
        { value: 'HARD', label: 'Hard' },
        { value: 'EXPERT', label: 'Expert' }
    ];

    const [category, setCategory] = useState(initialFilters?.category || 'DYNAMIC_PROGRAMMING');
    const [difficulty, setDifficulty] = useState(initialFilters?.difficulty || 'MEDIUM');
    const [subtype, setSubtype] = useState(initialFilters?.subtype || '');

    const selectedCategory = categories.find(c => c.value === category);

    // Sync state when initialFilters change
    useEffect(() => {
        if (initialFilters) {
            setCategory(initialFilters.category || 'DYNAMIC_PROGRAMMING');
            setDifficulty(initialFilters.difficulty || 'MEDIUM');
            setSubtype(initialFilters.subtype || '');
        }
    }, [initialFilters]);

    // Notify parent when filters change
    useEffect(() => {
        if (onFiltersChange) {
            onFiltersChange({ category, difficulty, subtype });
        }
    }, [category, difficulty, subtype, onFiltersChange]);

    const handleCategoryChange = (e) => {
        if (!isHost) return;
        setCategory(e.target.value);
        setSubtype(''); // Reset subtype when category changes
    };

    const handleDifficultyChange = (e) => {
        if (!isHost) return;
        setDifficulty(e.target.value);
    };

    const handleSubtypeChange = (e) => {
        if (!isHost) return;
        setSubtype(e.target.value);
    };

    return (
        <div className="game-filters">
            <h3 className="filters-title">
                Problem Selection {!isHost && <span className="read-only">(Read-Only)</span>}
            </h3>

            {/* Category Selector */}
            <div className="filter-section">
                <label className="filter-label">
                    Category:
                </label>
                <select
                    className={`filter-select ${!isHost ? 'disabled' : ''}`}
                    value={category}
                    onChange={handleCategoryChange}
                    disabled={!isHost}
                >
                    {categories.map(cat => (
                        <option key={cat.value} value={cat.value}>
                            {cat.label}
                        </option>
                    ))}
                </select>
            </div>

            {/* Difficulty Selector */}
            <div className="filter-section">
                <label className="filter-label">
                    Difficulty:
                </label>
                <select
                    className={`filter-select ${!isHost ? 'disabled' : ''}`}
                    value={difficulty}
                    onChange={handleDifficultyChange}
                    disabled={!isHost}
                >
                    {difficulties.map(diff => (
                        <option key={diff.value} value={diff.value}>
                            {diff.label}
                        </option>
                    ))}
                </select>
            </div>

            {/* Subtype Selector (Optional) */}
            {selectedCategory && selectedCategory.subtypes && (
                <div className="filter-section">
                    <label className="filter-label">
                        Specific Topic (Optional):
                    </label>
                    <select
                        className={`filter-select ${!isHost ? 'disabled' : ''}`}
                        value={subtype}
                        onChange={handleSubtypeChange}
                        disabled={!isHost}
                    >
                        <option value="">Any</option>
                        {selectedCategory.subtypes.map(sub => (
                            <option key={sub} value={sub}>
                                {sub}
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {/* Summary */}
            <div className="filter-summary">
                <strong>Your Selection:</strong>
                <div className="summary-content">
                    <span className="summary-item">
                        Category: {selectedCategory?.label || 'Not selected'}
                    </span>
                    <span className="summary-item">
                        Difficulty: {difficulties.find(d => d.value === difficulty)?.label || 'Not selected'}
                    </span>
                    {subtype && (
                        <span className="summary-item">
                            Topic: {subtype}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
}

export default GameFilters;
