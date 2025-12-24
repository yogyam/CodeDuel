import React from 'react';
import './ProblemTitleSelector.css';

/**
 * Component for selecting from 3 generated problem title options
 * Part of the two-step problem generation flow
 */
function ProblemTitleSelector({ titleOptions, onSelect, isLoading }) {
    if (isLoading) {
        return (
            <div className="title-selector loading">
                <div className="loading-spinner"></div>
                <p>Generating problem options...</p>
            </div>
        );
    }

    if (!titleOptions || titleOptions.length === 0) {
        return null;
    }

    return (
        <div className="title-selector">
            <h3 className="selector-title">Choose a Problem:</h3>
            <p className="selector-subtitle">Select one of these AI-generated problem ideas</p>

            <div className="title-options">
                {titleOptions.map((option, index) => (
                    <div
                        key={index}
                        className="title-card"
                        onClick={() => onSelect(option)}
                    >
                        <div className="title-card-header">
                            <span className="option-number">Option {index + 1}</span>
                            <span className="concept-tag">{option.concept}</span>
                        </div>
                        <h4 className="title-name">{option.title}</h4>
                        <p className="title-description">{option.briefDescription}</p>
                        <div className="select-button">
                            <span>Select this problem</span>
                            <span className="arrow">→</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default ProblemTitleSelector;
