package com.coderace.service;

import com.coderace.dto.SubmissionVerdict;
import com.coderace.entity.User;
import com.coderace.model.Submission;
import com.coderace.repository.SubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing code submissions
 */
@Service
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Save a new code submission
     * 
     * @param user       User who submitted
     * @param gameRoomId Game room ID
     * @param problemId  Problem identifier
     * @param code       Submitted code
     * @param language   Programming language
     * @param verdict    Submission verdict
     * @return Saved submission entity
     */
    @Transactional
    public Submission saveSubmission(User user, String gameRoomId, String problemId,
            String code, String language, SubmissionVerdict verdict) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setGameRoomId(gameRoomId);
        submission.setProblemId(problemId);
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setVerdict(verdict.getVerdict());
        submission.setTestsPassed(verdict.getTestsPassed());
        submission.setTotalTests(verdict.getTotalTests());

        if (!verdict.getErrors().isEmpty()) {
            submission.setErrorMessage(String.join("\n", verdict.getErrors()));
        }

        submission.setSubmittedAt(LocalDateTime.now());

        Submission saved = submissionRepository.save(submission);

        log.info("Saved submission {} for user {} in room {}: verdict={}, {}/{}",
                saved.getId(), user.getUsername(), gameRoomId,
                verdict.getVerdict(), verdict.getTestsPassed(), verdict.getTotalTests());

        return saved;
    }

    /**
     * Get all submissions by a user in a game room
     */
    public List<Submission> getSubmissionsByUserAndRoom(User user, String gameRoomId) {
        return submissionRepository.findByUserAndGameRoomIdOrderBySubmittedAtDesc(user, gameRoomId);
    }

    /**
     * Get the latest submission by a user in a room
     */
    public Optional<Submission> getLatestSubmission(User user, String gameRoomId) {
        return submissionRepository.findFirstByUserAndGameRoomIdOrderBySubmittedAtDesc(user, gameRoomId);
    }

    /**
     * Check if user has an accepted submission for the problem in the room
     */
    public boolean hasAcceptedSubmission(User user, String gameRoomId) {
        List<Submission> accepted = submissionRepository
                .findByUserAndGameRoomIdAndVerdict(user, gameRoomId, "ACCEPTED");
        return !accepted.isEmpty();
    }

    /**
     * Count submissions by user in a room
     */
    public long countSubmissions(User user, String gameRoomId) {
        return submissionRepository.countByUserAndGameRoomId(user, gameRoomId);
    }
}
