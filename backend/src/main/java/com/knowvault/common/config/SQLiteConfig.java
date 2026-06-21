package com.knowvault.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SQLiteConfig {

    private final Environment env;

    public SQLiteConfig(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logConfig() {
        String url = env.getProperty("spring.datasource.url", "not set");
        System.out.println("SQLite datasource URL: " + url);
    }
}
