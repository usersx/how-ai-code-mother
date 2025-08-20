package com.howmoon.howaicodemother.ai;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class AiAppNameGeneratorServiceTest {

    @Resource
    private AiAppNameGeneratorService aiAppNameGeneratorService;

    @Test
    public void testGenerateAppName_cnPrompt() {
        String prompt = "创建一个现代化的个人博客网站，支持Markdown与标签搜索";
        String name = aiAppNameGeneratorService.generateAppName(prompt);
        log.info("prompt: {} -> name: {}", prompt, name);
        Assertions.assertNotNull(name);
        Assertions.assertFalse(name.trim().isEmpty());
    }

    @Test
    public void testGenerateAppName_adminSystem() {
        String prompt = "做一个电商管理系统，包含用户管理、商品管理、订单管理";
        String name = aiAppNameGeneratorService.generateAppName(prompt);
        log.info("prompt: {} -> name: {}", prompt, name);
        Assertions.assertNotNull(name);
        Assertions.assertFalse(name.trim().isEmpty());
    }

    @Test
    public void testGenerateAppName_enPrompt() {
        String prompt = "Build a portfolio site for a designer with gallery and project pages";
        String name = aiAppNameGeneratorService.generateAppName(prompt);
        log.info("prompt: {} -> name: {}", prompt, name);
        Assertions.assertNotNull(name);
        Assertions.assertFalse(name.trim().isEmpty());
    }
}


