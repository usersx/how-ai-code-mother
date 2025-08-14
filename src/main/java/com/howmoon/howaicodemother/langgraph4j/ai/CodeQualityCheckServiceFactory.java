package com.howmoon.howaicodemother.langgraph4j.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码质量检查服务工厂类
 * 用于创建代码质量检查的AI服务实例
 */
@Slf4j // 日志注解，用于在类中添加日志功能
@Configuration// Spring配置注解，表明这是一个配置类
/**
 * 代码质量检查服务工厂类
 */
public class CodeQualityCheckServiceFactory {
    // Spring资源注入注解，用于自动注入依赖
    @Resource // 聊天模型，用于AI对话功能
    private ChatModel chatModel;

    /**
     * @return CodeQualityCheckService 代码质量检查服务实例
     * 创建代码质量检查 AI 服务
     */ // Spring Bean注解，表明该方法返回一个Bean实例
    @Bean
    public CodeQualityCheckService createCodeQualityCheckService() {
        return AiServices.builder(CodeQualityCheckService.class) // 设置聊天模型
                .chatModel(chatModel) // 构建并返回服务实例
                .build();
    }
}
