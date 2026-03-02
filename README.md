# VolcEngine Image Generator

基于火山引擎 Doubao Seed Vision API 的图片生成服务。该服务允许用户上传参考图片和文档/文本内容，根据这些内容生成与参考图风格类似的图片。

## 功能特性

- 上传参考图片
- 支持上传文档文件（PDF、DOC、DOCX、TXT）提取文本内容
- 支持直接输入文本内容
- 调用火山引擎 Doubao-Seed-1.6-vision 模型生成图片
- RESTful API 接口

## 技术栈

- Java 17
- Spring Boot 3.2.5
- OkHttp 4.12.0
- Apache POI（文档处理）
- PDFBox（PDF 处理）

## 项目结构

```
volcengine-image-generator/
├── src/
│   ├── main/
│   │   ├── java/com/volcengine/imagegen/
│   │   │   ├── ImageGeneratorApplication.java
│   │   │   ├── config/           # 配置类
│   │   │   ├── controller/       # REST 控制器
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── model/           # 模型类
│   │   │   └── service/         # 服务类
│   │   └── resources/
│   │       └── application.yml   # 配置文件
│   └── test/
├── pom.xml
└── README.md
```

## 配置说明

在 `application.yml` 中配置火山引擎 API 相关参数：

```yaml
volcengine:
  api:
    endpoint: https://ark.cn-beijing.volces.com/api/v3
    model: doubao-seed-1-6-vision
    api-key: your-api-key  # 从火山引擎控制台获取
```

也可以通过环境变量配置：

```bash
export VOLCENGINE_API_KEY=your-api-key
```

## 运行项目

### 使用 Maven 运行

```bash
cd volcengine-image-generator
mvn spring-boot:run
```

### 打包运行

```bash
mvn clean package
java -jar target/image-generator-1.0.0.jar
```

服务将在 `http://localhost:8080` 启动。

## API 接口

### 生成图片

**POST** `/api/v1/image-generation/generate`

Content-Type: `multipart/form-data`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| referenceImage | File | 否 | 参考图片文件 |
| referenceImageUrl | String | 否 | 参考图片 URL |
| document | File | 否 | 文档文件（与 textContent 二选一） |
| textContent | String | 否 | 直接文本内容（与 document 二选一） |
| prompt | String | 是 | 生成提示词 |
| count | Integer | 否 | 生成数量，默认 1 |
| style | String | 否 | 风格参数 |
| temperature | Double | 否 | 温度参数，默认 0.7 |

**请求示例（使用 curl）：**

```bash
curl -X POST http://localhost:8080/api/v1/image-generation/generate \
  -F "referenceImage=@/path/to/reference.jpg" \
  -F "prompt=请根据参考图片的风格，生成一张类似的图片" \
  -F "textContent=这是一张关于山水的图片，风格应该清新淡雅"
```

**响应示例：**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "imageUrl": "https://generated-image-url...",
    "requestId": "req-123",
    "tokensUsed": 1500,
    "status": "completed"
  }
}
```

### 健康检查

**GET** `/api/v1/image-generation/health`

### API 文档

**GET** `/api/v1/image-generation/docs`

## 获取火山引擎 API 密钥

1. 访问 [火山引擎控制台](https://console.volcengine.com/ark/region:ark+cn-beijing/model/detail?Id=doubao-seed-1-6-vision)
2. 登录账号
3. 进入"模型推理"页面
4. 创建 API Key
5. 将 API Key 配置到 `application.yml` 或环境变量中

## 注意事项

1. 图片和文档文件大小限制为 10MB
2. 文本内容会被截断到 8000 字符以内
3. 上传的文件会保存在 `./uploads` 目录下
4. 请根据火山引擎 API 文档调整具体的 endpoint 和参数格式

## 许可证

MIT License
