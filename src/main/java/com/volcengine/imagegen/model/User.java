package com.volcengine.imagegen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User entity
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_wechat_openid", columnList = "wechat_openid")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_method", nullable = false)
    @Builder.Default
    private LoginMethod loginMethod = LoginMethod.EMAIL;

    @Column(name = "wechat_openid", unique = true, length = 255)
    private String wechatOpenid;

    @Column(name = "wechat_unionid", length = 255)
    private String wechatUnionid;

    @Column(name = "wechat_nickname", length = 255)
    private String wechatNickname;

    @Column(name = "wechat_avatar", length = 500)
    private String wechatAvatar;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Get formatted creation time
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return null;
        }
        return createdAt.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
