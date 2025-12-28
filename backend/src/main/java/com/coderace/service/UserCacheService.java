package com.coderace.service;

import com.coderace.entity.User;
import com.coderace.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for caching user objects to reduce database queries
 * Uses Spring's caching abstraction with Caffeine cache
 */
@Service
@Slf4j
public class UserCacheService {

    private final UserRepository userRepository;

    public UserCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get user by ID with caching
     * Cache key is user ID, TTL matches JWT expiration (24 hours)
     * 
     * @param userId User's database ID
     * @return User object or null if not found
     */
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(Long userId) {
        log.debug("Cache miss - loading user {} from database", userId);
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Evict user from cache when updated
     * Call this after password changes, profile updates, etc.
     * 
     * @param userId User's database ID
     */
    @CacheEvict(value = "users", key = "#userId")
    public void evictUser(Long userId) {
        log.debug("Evicted user {} from cache", userId);
    }

    /**
     * Clear entire user cache
     * Use sparingly - mainly for testing or emergency cache invalidation
     */
    @CacheEvict(value = "users", allEntries = true)
    public void evictAll() {
        log.info("Cleared entire user cache");
    }
}
