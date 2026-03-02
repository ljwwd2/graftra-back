package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Prompt configuration properties
 */
@Data
@ConfigurationProperties(prefix = "prompts")
public class PromptProperties {

    /**
     * 图表提取提示词文件路径
     */
    private org.springframework.core.io.Resource chartExtraction;

    /**
     * 读取图表提取提示词内容
     */
    public String getChartExtractionPromptText() {
        try {
            if (chartExtraction != null && chartExtraction.exists()) {
                return new String(chartExtraction.getInputStream().readAllBytes());
            }
        } catch (Exception e) {
            // 文件读取失败，返回默认提示词
            return getDefaultChartExtractionPrompt();
        }
        return "";
    }

    /**
     * 获取默认的图表提取提示词
     */
    private String getDefaultChartExtractionPrompt() {
        return """
                你是信息可视化语义架构专家，同时也是图表内容设计专家。

任务：
阅读输入文档，识别出最核心、最有价值、最适合可视化表达的 3-5 张图表。

目标不是绘制图表，而是为每张图表生成：
1）用于向量检索图表模板的查询文本
2）用于向量检索优化的结构关键词
3）用于替换参考矢量图内容的完整内容填充提示词（用于精确替换参考图中的文字和矢量图标）

最终输出必须是 JSON 格式。只输出 JSON，不要包含任何其他内容。
                """;
    }
}
