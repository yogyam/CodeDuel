package com.coderace.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a test case for a competitive programming problem
 * Test cases are used to validate code submissions in the in-browser IDE
 */
@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Problem identifier (contestId + index, e.g., "1234A")
     * Foreign key to identify which problem this test case belongs to
     */
    @Column(nullable = false)
    private String problemId;

    /**
     * Input for this test case
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String input;

    /**
     * Expected output for this test case
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    /**
     * Type of test case: SAMPLE, EDGE_CASE, or LLM_GENERATED
     */
    @Column(nullable = false)
    private String type;

    /**
     * Whether this test case should be hidden from users
     * Sample cases are visible, others are hidden
     */
    @Column(nullable = false)
    private Boolean isHidden = false;

    /**
     * When this test case was created
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
