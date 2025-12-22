package com.coderace.model;

import com.coderace.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a code submission in a game
 * Tracks user code submissions, execution results, and verdicts
 */
@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who submitted the code
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Game room ID where this submission was made
     */
    @Column(nullable = false, length = 10)
    private String gameRoomId;

    /**
     * Problem identifier (contestId + index)
     */
    @Column(nullable = false)
    private String problemId;

    /**
     * The submitted code
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    /**
     * Programming language used (python, java, cpp, javascript, go)
     */
    @Column(nullable = false, length = 50)
    private String language;

    /**
     * Verdict: ACCEPTED, WRONG_ANSWER, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, etc.
     */
    @Column(length = 50)
    private String verdict;

    /**
     * Number of test cases passed
     */
    @Column
    private Integer testsPassed;

    /**
     * Total number of test cases
     */
    @Column
    private Integer totalTests;

    /**
     * Error message if submission failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When this submission was made
     */
    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if this submission passed all test cases
     */
    public boolean isAccepted() {
        return "ACCEPTED".equals(verdict) &&
                testsPassed != null &&
                totalTests != null &&
                testsPassed.equals(totalTests);
    }
}
