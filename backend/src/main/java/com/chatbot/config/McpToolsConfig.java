package com.chatbot.config;

import com.chatbot.tools.SampleTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider sampleToolCallbackProvider(SampleTools sampleTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(sampleTools)
                .build();
    }
}
