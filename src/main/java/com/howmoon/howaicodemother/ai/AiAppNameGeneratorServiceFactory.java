package com.howmoon.howaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用名称生成服务工厂
 */
@Slf4j
@Configuration
public class AiAppNameGeneratorServiceFactory {

    @Resource(name = "routingChatModelPrototype")
    private ChatModel routingChatModel;

    public AiAppNameGeneratorService createAiAppNameGeneratorService() {
        return AiServices.builder(AiAppNameGeneratorService.class)
                .chatModel(routingChatModel)
                .build();
    }

    @Bean
    public AiAppNameGeneratorService aiAppNameGeneratorService() {
        return createAiAppNameGeneratorService();
    }
}


