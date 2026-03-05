-- ================================================================
-- Graftra 数据库建表脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ================================================================
-- 遵循阿里云 Java 开发规范：
-- 1. 不使用外键约束，在应用层保证数据一致性
-- 2. 使用 VARCHAR(36) 存储 UUID
-- 3. 使用 DATETIME 类型存储时间
-- 4. 索引名称使用 idx_ 前缀，唯一索引使用 uk_ 前缀
-- ================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `graftra`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `graftra`;

-- ================================================================
-- 2. 用户表 (users)
-- ================================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` VARCHAR(36) NOT NULL COMMENT '用户唯一ID (UUID)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '用户昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码哈希 (BCrypt)',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `login_method` VARCHAR(20) NOT NULL DEFAULT 'PHONE' COMMENT '登录方式: PHONE, WECHAT',
    `wechat_openid` VARCHAR(255) DEFAULT NULL COMMENT '微信OpenID',
    `wechat_unionid` VARCHAR(255) DEFAULT NULL COMMENT '微信UnionID',
    `wechat_nickname` VARCHAR(255) DEFAULT NULL COMMENT '微信昵称',
    `wechat_avatar` VARCHAR(500) DEFAULT NULL COMMENT '微信头像',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
    INDEX `idx_login_method` (`login_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================================================
-- 3. 刷新令牌表 (refresh_tokens)
-- 注意：不使用外键，在应用层保证数据一致性
-- ================================================================
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` VARCHAR(36) NOT NULL COMMENT '令牌ID (UUID)',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `token` VARCHAR(500) NOT NULL COMMENT 'JWT刷新令牌',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `revoked_at` DATETIME DEFAULT NULL COMMENT '撤销时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_expires_at` (`expires_at`),
    INDEX `idx_user_expires` (`user_id`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

-- ================================================================
-- 4. 查询验证
-- ================================================================
-- 查看所有表
SHOW TABLES;

-- 查看用户表结构
DESC users;

-- 查看刷新令牌表结构
DESC refresh_tokens;

-- ================================================================
-- 5. 数据一致性说明
-- ================================================================
-- 本数据库不使用外键约束，数据一致性由应用层保证：
--
-- 1. 删除用户时，需要同时删除该用户的所有 refresh_tokens
--    DELETE FROM refresh_tokens WHERE user_id = ?
--    DELETE FROM users WHERE id = ?
--
-- 2. 清理过期令牌（建议定时任务）
--    DELETE FROM refresh_tokens
--    WHERE revoked_at IS NOT NULL OR expires_at < NOW();
-- ================================================================
