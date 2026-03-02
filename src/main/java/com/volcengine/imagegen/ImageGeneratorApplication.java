package com.volcengine.imagegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;

/**
 * Main application class for VolcEngine Image Generator
 */
@SpringBootApplication(
    exclude = {
        OpenAiAutoConfiguration.class
    }
)
@EnableConfigurationProperties
public class ImageGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageGeneratorApplication.class, args);
    }
}
