package com.howmoon.howaicodemother.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * AI 应用名称生成服务
 */
public interface AiAppNameGeneratorService {

    /**
     * 根据用户的应用描述自动生成一个合适的应用名称
     * 只返回名称文本
     */
    @SystemMessage(fromResource = "prompt/app-name-generator-system-prompt.txt")
    String generateAppName(String userPrompt);
}


