package com.coderace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * User entity for authentication and user management
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    /**
     * BCrypt hashed password - null for OAuth users
     */
    private String passwordHash;

    /**
     * Google OAuth ID - null for email/password users
     */
    @Column(unique = true)
    private String googleId;

    /**
     * Optional Codeforces handle for user profile
     */
    private String codeforcesHandle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
