package com.coderace.dto;

/**
 * DTO for GameRoom list/summary responses
 * Used in paginated endpoints
 */
public record GameRoomSummaryDTO(
        String roomId,
        String state,
        int participantCount,
        int maxParticipants,
        String difficulty,
        String category,
        String createdAt) {
}
