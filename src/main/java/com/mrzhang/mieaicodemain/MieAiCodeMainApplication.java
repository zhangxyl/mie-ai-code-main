package com.mrzhang.mieaicodemain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class MieAiCodeMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MieAiCodeMainApplication.class, args);
    }

}
