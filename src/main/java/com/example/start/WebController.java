package com.example.start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liuxin
 * 2022/3/24 10:58 AM
 */
@Slf4j
@RestController
public class WebController {



    /**
     * 当遇到 RuntimeException 进行重试，重试次数5次
     * 监听器Bean名称指定为sayRetryListener
     * 重试策略是固定时间1s重试一次
     * 注意: CircuitBreaker 和 Retryable 不要修饰同一个方法。要么进行断路要么进行重试。
     * @param flag 标记
     * @return String
     */
    @GetMapping("get/{flag}")
    @CircuitBreaker(openTimeout = 5000, maxAttempts = 2,resetTimeout = 10000)
//    @Retryable(maxAttempts = 5, value = RuntimeException.class, listeners = {"sayRetryListener"}, backoff = @Backoff(1000))
    public String get(@PathVariable Integer flag) {
        if (flag > 1) {
            log.info("重试进入");
            throw new RuntimeException("自定义异常");
        }
        return "处理正常";
    }

    @Recover
    public String getBackup(RuntimeException runtimeException) {
        log.error("重试一直失败,进入备用方法:" + runtimeException.getMessage());
        return "备用方法进去";
    }

}
