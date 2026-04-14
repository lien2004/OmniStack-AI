package com.linglong.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 玲珑AI平台启动类
 */
@SpringBootApplication(scanBasePackages = {"com.linglong.project", "com.linglong.common"})
public class LingLongApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingLongApplication.class, args);
    }
}
