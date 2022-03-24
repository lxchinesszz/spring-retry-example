package com.example.start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

/**
 * @author liuxin
 * 2022/3/24 11:03 AM
 */
@Slf4j
@Configuration
public class RetryConfig {

    @Bean("sayRetryListener")
    public SayRetryListener sayRetryListener() {
        return new SayRetryListener();
    }

    public static class SayRetryListener extends RetryListenerSupport {
        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            log.error("SayRetryListener: 重试操作开始调用");
            return super.open(context, callback);
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.error("SayRetryListener: 第" + context.getRetryCount() + "次,调用失败" + throwable);
            super.onError(context, callback, throwable);
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.error("SayRetryListener: 重试操作结束调用");
            super.close(context, callback, throwable);
        }
    }
}
