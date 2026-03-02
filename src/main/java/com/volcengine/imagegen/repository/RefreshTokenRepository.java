package com.volcengine.imagegen.repository;

import com.volcengine.imagegen.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh token repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all valid refresh tokens for a user
     */
    List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(String userId, LocalDateTime now);

    /**
     * Find all refresh tokens for a user
     */
    List<RefreshToken> findByUserId(String userId);

    /**
     * Delete all expired tokens
     */
    void deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * Delete all refresh tokens for a user
     */
    void deleteByUserId(String userId);

    /**
     * Check if token exists
     */
    boolean existsByToken(String token);
}
