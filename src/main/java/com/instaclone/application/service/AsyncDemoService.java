package com.instaclone.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncDemoService {

    @Async("securityAsyncTaskExecutor")
    public CompletableFuture<String> getPrincipalWithSecurityExecutor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return CompletableFuture.completedFuture(auth != null ? auth.getName() : null);
    }

    @Async("standardAsyncTaskExecutor")
    public CompletableFuture<String> getPrincipalWithStandardExecutor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return CompletableFuture.completedFuture(auth != null ? auth.getName() : null);
    }
}
