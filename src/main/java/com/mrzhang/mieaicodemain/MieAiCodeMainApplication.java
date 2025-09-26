package com.mrzhang.mieaicodemain;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.mrzhang.mieaicodemain.mapper")
public class MieAiCodeMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MieAiCodeMainApplication.class, args);
    }

}
