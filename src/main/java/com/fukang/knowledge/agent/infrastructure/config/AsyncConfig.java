package com.fukang.knowledge.agent.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>为文档后处理管道提供独立线程池，避免与系统其他异步任务共享线程资源</p>
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private final DocumentProcessingProperties properties;

    /**
     * 文档处理专用线程池
     * <p>独立线程池，避免与系统其他异步任务（如 AI 调用日志）共享 ForkJoinPool.commonPool()</p>
     *
     * <p>线程池参数说明：
     * <ul>
     *   <li>核心线程数 2：同时处理最多 2 个文档（避免 Embedding API 并发过高）</li>
     *   <li>最大线程数 4：高峰期允许 4 个并发处理</li>
     *   <li>队列容量 100：超出核心线程数的任务排队等待</li>
     *   <li>拒绝策略 CallerRunsPolicy：队列满时由事件发布线程执行（天然背压，防止 OOM）</li>
     *   <li>线程前缀 document-processor-：便于日志排查</li>
     * </ul>
     */
    @Bean("documentProcessingExecutor")
    public ThreadPoolTaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("document-processor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("文档处理线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * QA 流式问答线程池。
     * <p>SSE 请求会长时间占用连接，单独线程池可以避免影响文档处理等后台任务。</p>
     */
    @Bean("qaStreamExecutor")
    public ThreadPoolTaskExecutor qaStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("qa-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("QA 流式问答线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
