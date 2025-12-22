package com.coderace.repository;

import com.coderace.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TestCase entity
 * Provides database access for test cases
 */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    /**
     * Find all test cases for a specific problem
     * 
     * @param problemId The problem identifier (contestId + index)
     * @return List of test cases for the problem
     */
    List<TestCase> findByProblemId(String problemId);

    /**
     * Find only visible (non-hidden) test cases for a problem
     * 
     * @param problemId The problem identifier
     * @param isHidden  False to get visible test cases
     * @return List of visible test cases
     */
    List<TestCase> findByProblemIdAndIsHidden(String problemId, Boolean isHidden);

    /**
     * Delete all test cases for a specific problem
     * 
     * @param problemId The problem identifier
     */
    void deleteByProblemId(String problemId);

    /**
     * Count test cases for a problem
     * 
     * @param problemId The problem identifier
     * @return Number of test cases
     */
    long countByProblemId(String problemId);
}
