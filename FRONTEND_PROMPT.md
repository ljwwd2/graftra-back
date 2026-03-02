# 前端登录模块开发提示词

请使用以下提示词让前端 AI Agent 帮助对接登录接口：

---

## 复制以下内容给前端 AI：

```
你是一个前端开发专家，请帮我对接 Graftra 项目的登录认证模块。

## 技术栈
- 前端框架：React / Vue / Angular（根据实际情况选择）
- HTTP客户端：axios / fetch
- 状态管理：根据项目实际使用

## 后端接口信息

Base URL: http://localhost:8080

所有接口返回统一的响应格式：
```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  error?: {
    message: string;
    code: string;
  };
}
```

## 需要对接的接口

### 1. 用户注册
POST /api/auth/register

请求类型：
```typescript
interface RegisterRequest {
  name?: string;           // 可选，用户昵称
  email: string;           // 必填，需要邮箱格式验证
  password: string;        // 必填，最少8位
  agreeToTerms: boolean;   // 必填，必须为true
}
```

响应类型：
```typescript
interface AuthResponse {
  user: {
    id: string;
    name: string;
    email: string;
    avatar: string | null;
    createdAt: string;
  };
  token: string;
  refreshToken: string;
  expiresIn: number;  // 秒数，通常604800（7天）
}
```

### 2. 邮箱登录
POST /api/auth/login

请求类型：
```typescript
interface LoginRequest {
  email: string;
  password: string;
  remember?: boolean;  // 可选
}
```

响应类型：同注册接口 AuthResponse

### 3. 微信登录
POST /api/auth/wechat

请求类型：
```typescript
interface WechatLoginRequest {
  code: string;      // 微信授权码
  state?: string;    // 可选
}
```

响应类型：同注册接口 AuthResponse

### 4. 退出登录
POST /api/auth/logout

请求头：Authorization: Bearer {token}

响应：成功消息

### 5. 刷新令牌
POST /api/auth/refresh

请求类型：
```typescript
interface RefreshTokenRequest {
  refreshToken: string;
}
```

响应类型：
```typescript
interface TokenRefreshResponse {
  token: string;
  expiresIn: number;
}
```

### 6. 获取当前用户信息
GET /api/auth/me

请求头：Authorization: Bearer {token}

响应类型：
```typescript
interface UserInfo {
  id: string;
  name: string;
  email: string;
  avatar: string | null;
  createdAt: string;
}
```

## 错误处理

常见错误码：
- EMAIL_EXISTS (400): 邮箱已被注册
- INVALID_CREDENTIALS (401): 邮箱或密码错误
- UNAUTHORIZED (401): 令牌无效或过期
- VALIDATION_ERROR (400): 参数验证失败
- RATE_LIMIT_EXCEEDED (429): 请求过于频繁

## Token 管理

1. Access Token 有效期 7 天，放入请求头 Authorization: Bearer {token}
2. Refresh Token 有效期 30 天，用于刷新 Access Token
3. 建议 Refresh Token 存储在 localStorage，Access Token 可存内存
4. 当 Access Token 过期时，自动调用刷新接口

## 需要实现的功能

请帮我实现以下功能：

1. **登录表单组件**
   - 邮箱输入（带格式验证）
   - 密码输入（最少8位验证）
   - 记住我选项
   - 错误提示显示
   - 登录按钮loading状态

2. **注册表单组件**
   - 邮箱输入（带格式验证）
   - 密码输入（最少8位，带强度提示）
   - 确认密码输入
   - 服务条款勾选
   - 错误提示显示

3. **API 封装**
   - 创建 auth 相关的 API 请求函数
   - 统一的错误处理
   - 自动 Token 刷新机制
   - 请求拦截器添加 Authorization 头

4. **状态管理**
   - 用户信息存储
   - Token 存储
   - 登录状态判断
   - 登出功能

5. **路由守卫**
   - 未登录用户重定向到登录页
   - 已登录用户访问登录页重定向到首页

请根据项目实际使用的框架和组件库，生成相应的代码实现。
```

---

## 快速测试命令

可以让前端用这些命令快速测试接口：

```bash
# 注册测试
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","agreeToTerms":true}'

# 登录测试
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# 获取用户信息（替换TOKEN为实际值）
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TOKEN"
```

---

## 接口文档地址

Swagger UI: http://localhost:8080/swagger-ui.html
