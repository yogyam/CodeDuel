package com.coderace.repository;

import com.coderace.entity.User;
import com.coderace.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Submission entity
 * Provides database access for code submissions
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Find all submissions by a specific user
     * 
     * @param user The user
     * @return List of submissions ordered by submission time descending
     */
    List<Submission> findByUserOrderBySubmittedAtDesc(User user);

    /**
     * Find all submissions for a specific game room
     * 
     * @param gameRoomId The game room ID
     * @return List of submissions
     */
    List<Submission> findByGameRoomId(String gameRoomId);

    /**
     * Find submissions by user and room, ordered by time descending
     * 
     * @param user       The user
     * @param gameRoomId The game room ID
     * @return List of submissions
     */
    List<Submission> findByUserAndGameRoomIdOrderBySubmittedAtDesc(User user, String gameRoomId);

    /**
     * Find all submissions for a specific problem
     * 
     * @param problemId The problem identifier
     * @return List of submissions
     */
    List<Submission> findByProblemId(String problemId);

    /**
     * Find the latest submission by a user in a specific game room
     * 
     * @param user       The user
     * @param gameRoomId The game room ID
     * @return Optional of the latest submission
     */
    Optional<Submission> findFirstByUserAndGameRoomIdOrderBySubmittedAtDesc(User user, String gameRoomId);

    /**
     * Find all accepted submissions for a user in a game room
     * 
     * @param user       The user
     * @param gameRoomId The game room ID
     * @param verdict    The verdict (e.g., "ACCEPTED")
     * @return List of accepted submissions
     */
    List<Submission> findByUserAndGameRoomIdAndVerdict(User user, String gameRoomId, String verdict);

    /**
     * Count submissions by a user in a game room
     * 
     * @param user       The user
     * @param gameRoomId The game room ID
     * @return Number of submissions
     */
    long countByUserAndGameRoomId(User user, String gameRoomId);
}
