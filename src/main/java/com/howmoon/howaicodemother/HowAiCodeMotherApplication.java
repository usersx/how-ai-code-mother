package com.howmoon.howaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})

@MapperScan("com.howmoon.howaicodemother.mapper")
public class HowAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(HowAiCodeMotherApplication.class, args);
    }

}
