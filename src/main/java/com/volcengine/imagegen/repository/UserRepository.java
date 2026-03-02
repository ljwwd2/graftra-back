package com.volcengine.imagegen.repository;

import com.volcengine.imagegen.model.User;
import com.volcengine.imagegen.model.LoginMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by WeChat OpenID
     */
    Optional<User> findByWechatOpenid(String openid);

    /**
     * Check if WeChat OpenID exists
     */
    boolean existsByWechatOpenid(String openid);

    /**
     * Find user by email and login method
     */
    Optional<User> findByEmailAndLoginMethod(String email, LoginMethod loginMethod);
}
