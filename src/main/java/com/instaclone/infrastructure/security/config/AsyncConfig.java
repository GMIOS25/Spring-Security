package com.instaclone.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "securityAsyncTaskExecutor")
    public Executor securityAsyncTaskExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(new SimpleAsyncTaskExecutor("security-async-"));
    }

    @Bean(name = "standardAsyncTaskExecutor")
    public Executor standardAsyncTaskExecutor() {
        return new SimpleAsyncTaskExecutor("standard-async-");
    }
}
