package com.hatice.loginsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.analysis-executor.core-pool-size}")
    private int corePoolSize;

    @Value("${app.analysis-executor.max-pool-size}")
    private int maxPoolSize;

    @Value("${app.analysis-executor.queue-capacity}")
    private int queueCapacity;

    @Value("${app.analysis-executor.thread-name-prefix}")
    private String threadNamePrefix;

    @Value("${app.upload-merge-executor.core-pool-size}")
    private int mergeCorePoolSize;

    @Value("${app.upload-merge-executor.max-pool-size}")
    private int mergeMaxPoolSize;

    @Value("${app.upload-merge-executor.queue-capacity}")
    private int mergeQueueCapacity;

    @Value("${app.upload-merge-executor.thread-name-prefix}")
    private String mergeThreadNamePrefix;

    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }

    @Bean(name = "uploadMergeTaskExecutor")
    public Executor uploadMergeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(mergeCorePoolSize);
        executor.setMaxPoolSize(mergeMaxPoolSize);
        executor.setQueueCapacity(mergeQueueCapacity);
        executor.setThreadNamePrefix(mergeThreadNamePrefix);
        executor.initialize();
        return executor;
    }
}