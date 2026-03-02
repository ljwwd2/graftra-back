# Graftra 登录接口协议

## Base URL
- 开发环境: `http://localhost:8080`
- API前缀: `/api/auth`

## 通用响应格式

所有接口返回统一的JSON格式：

```json
{
  "success": true|false,
  "message": "string",
  "data": { ... },
  "error": {
    "message": "string",
    "code": "string"
  }
}
```

---

## 1. 用户注册

**接口**: `POST /api/auth/register`

**Content-Type**: `application/json`

**请求体**:
```json
{
  "name": "张三",           // 可选，用户昵称
  "email": "user@example.com",  // 必填，邮箱格式
  "password": "password123",    // 必填，最少8位
  "agreeToTerms": true         // 必填，必须为true
}
```

**成功响应** (201):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "张三",
      "email": "user@example.com",
      "avatar": null,
      "createdAt": "2024-03-02T10:30:00"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 604800
  }
}
```

**错误响应** (400):
```json
{
  "success": false,
  "message": "该邮箱已被注册",
  "error": {
    "message": "该邮箱已被注册",
    "code": "EMAIL_EXISTS"
  }
}
```

---

## 2. 邮箱登录

**接口**: `POST /api/auth/login`

**Content-Type**: `application/json`

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "remember": false    // 可选，是否记住登录状态
}
```

**成功响应** (200): 同注册响应

**错误响应** (401):
```json
{
  "success": false,
  "message": "邮箱或密码错误",
  "error": {
    "message": "邮箱或密码错误",
    "code": "INVALID_CREDENTIALS"
  }
}
```

---

## 3. 微信登录

**接口**: `POST /api/auth/wechat`

**Content-Type**: `application/json`

**请求体**:
```json
{
  "code": "081aBcDe23fgHiJ456kLmNo7pqR89st",  // 微信授权码
  "state": "optional_state"                   // 可选，状态参数
}
```

**成功响应** (200): 同注册响应

---

## 4. 退出登录

**接口**: `POST /api/auth/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**成功响应** (200):
```json
{
  "success": true,
  "message": "退出成功"
}
```

---

## 5. 刷新令牌

**接口**: `POST /api/auth/refresh`

**Content-Type**: `application/json`

**请求体**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**成功响应** (200):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 604800
  }
}
```

---

## 6. 获取当前用户信息

**接口**: `GET /api/auth/me`

**请求头**:
```
Authorization: Bearer {token}
```

**成功响应** (200):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "email": "user@example.com",
    "avatar": null,
    "createdAt": "2024-03-02T10:30:00"
  }
}
```

---

## 错误码说明

| 错误码 | HTTP状态 | 说明 |
|--------|----------|------|
| `EMAIL_EXISTS` | 400 | 邮箱已被注册 |
| `TERMS_NOT_AGREED` | 400 | 未同意服务条款 |
| `VALIDATION_ERROR` | 400 | 参数验证失败 |
| `INVALID_CREDENTIALS` | 401 | 邮箱或密码错误 |
| `UNAUTHORIZED` | 401 | 未授权或令牌无效 |
| `FORBIDDEN` | 403 | 无访问权限 |
| `USER_NOT_FOUND` | 404 | 用户不存在 |
| `RATE_LIMIT_EXCEEDED` | 429 | 请求过于频繁 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |

---

## Token 使用说明

### Access Token
- 有效期: 7天 (604800秒)
- 用途: 访问需要认证的接口
- 使用方式: 放入请求头 `Authorization: Bearer {token}`

### Refresh Token
- 有效期: 30天
- 用途: 刷新过期的 Access Token
- 存储建议: localStorage 或 sessionStorage

### Token 刷新流程
1. 当 Access Token 过期时，使用 Refresh Token 调用 `/api/auth/refresh`
2. 获取新的 Access Token
3. 如果 Refresh Token 也过期，需要重新登录

---

## 限流说明

登录/注册接口有频率限制：
- 每个IP每分钟最多 5 次请求
- 超过限制返回 429 状态码

---

## 请求示例 (cURL)

### 注册
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "agreeToTerms": true
  }'
```

### 登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 获取用户信息
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```
