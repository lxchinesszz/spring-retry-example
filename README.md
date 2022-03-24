
[Springlearn技术站](https://java.springlearn.cn/)
[作者博客](https://blog.springlearn.cn/)

## 一、简介

前面我们了解到了,Guava的重试组件,我们可以基于Guava的能力,来封装我们需要的能力来满足我们的业务。今天来分享Spring-Retry重试组件。当然Spring只是帮我们封装好了，如果你不想自定义
重试组件，那么我们可以直接使用Spring的能力来实现。


API 也是非常的简单，几个注解就可以搞定。

## 二、依赖

```xml 
    <!--springboot项目都不用引入版本号-->
    <dependency>
      <groupId>org.springframework.retry</groupId>
      <artifactId>spring-retry</artifactId>
    </dependency>
    <!--还是需要aop的支持的(如果已经引入了aop就不用再添加这个依赖了)-->
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-aspects</artifactId>
    </dependency>
```

## 三、使用

### 3.1 @EnableRetry 开启重试

SpringBoot启动类上添加开启重试注解

```java
    @EnableRetry
    @SpringBootApplication
    public class Application {
        public static void main(String[] args) {
            ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        }
    }
```

### 3.2 @Retryable 重试策略

在需要重试的方法上加注解@Retryable

```java
    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(delay = 100))
    public String say(String param) {
        double random = Math.random();
        if (random > 0.1) {
            throw new RuntimeException("超时");
        }
        return random + "";
    }
```

- [x] value = RuntimeException.class：是指方法抛出RuntimeException异常时，进行重试。这里可以指定你想要拦截的异常。
- [x] maxAttempts：是最大重试次数。如果不写，则是默认3次。
- [x] backoff = @Backoff(delay = 100)：是指重试间隔。delay=100意味着下一次的重试，要等100毫秒之后才能执行。

### 3.3 @Recover 重试失败

当@Retryable方法重试失败之后，最后就会调用@Recover方法。用于@Retryable失败时的“兜底”处理方法。 @Recover的方法必须要与@Retryable注解的方法保持一致，第一入参为要重试的异常，其他参数与@Retryable保持一致，返回值也要一样，否则无法执行！


```java 
    @Retryable(value = IllegalAccessException.class)
    public void say() throws IllegalAccessException {
        log.info("do something... {}", LocalDateTime.now());
        throw new IllegalAccessException();
    }


    @Recover
    public void sayBackup(IllegalAccessException e) {
        log.info("service retry after Recover => {}", e.getMessage());
    }
```

### 3.4 @CircuitBreaker 熔断策略

规定时间内如果重试次数达到了最大次数,开启熔断策略。
5秒内,这个方法重试了2次,就会断路。直接走@Recover修饰的方法。当超过10s后进行重置,继续走get方法。

注意@Retryable和@CircuitBreaker不要修饰同一个方法。

```java 
    @CircuitBreaker(openTimeout = 5000, maxAttempts = 2,resetTimeout = 10000)
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
```

|属性|意思|
|:--|:--|
|include|指定处理的异常类。默认为空|
|exclude|指定不需要处理的异常。默认为空|
|value|指定要重试的异常。默认为空|
|maxAttempts|最大重试次数。默认3次|
|openTimeout|配置熔断器打开的超时时间，默认5s，当超过openTimeout之后熔断器电路变成半打开状态（只要有一次重试成功，则闭合电路）|
|resetTimeout|配置熔断器重新闭合的超时时间，默认20s，超过这个时间断路器关闭|
|include|指定处理的异常类。默认为空|




### 3.5 RetryListener 监听器

spring-retry和guava-retry一样同样有监听器。我们可以自定义我们的监听器

```java 
@Slf4j
public class DefaultListenerSupport extends RetryListenerSupport {
    @Override
    public <T, E extends Throwable> void close(RetryContext context,
                                               RetryCallback<T, E> callback, Throwable throwable) {
        log.info("onClose");
        super.close(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback, Throwable throwable) {
        log.info("onError");
        super.onError(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context,
                                                 RetryCallback<T, E> callback) {
        log.info("onOpen");
        return super.open(context, callback);
    }
}

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(); //设置重试策略
        retryPolicy.setMaxAttempts(2);
        retryTemplate.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy(); //设置退避策略
        fixedBackOffPolicy.setBackOffPeriod(2000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        
        retryTemplate.registerListener(new DefaultListenerSupport()); //设置retryListener
        return retryTemplate;
    }
}
```

### 3.6 RetryPolicy 重试策略

|属性|意思|
|:--|:--|
|NeverRetryPolicy|只允许调用RetryCallback一次，不允许重试；|
|AlwaysRetryPolicy|允许无限重试，直到成功，此方式逻辑不当会导致死循环；|
|SimpleRetryPolicy|固定次数重试策略，默认重试最大次数为3次，RetryTemplate默认使用的策略；|
|TimeoutRetryPolicy|超时时间重试策略，默认超时时间为1秒，在指定的超时时间内允许重试；|
|CircuitBreakerRetryPolicy|有熔断功能的重试策略，需设置3个参数openTimeout、resetTimeout和delegate|
|CompositeRetryPolicy|组合重试策略，有两种组合方式，乐观组合重试策略是指只要有一个策略允许重试即可以，悲观组合重试策略是指只要有一个策略不允许重试即可以，但不管哪种组合方式，组合中的每一个策略都会执行。|

### 3.7 BackOffPolicy 退避策略

下一次重试的策略。
退避是指怎么去做下一次的重试，在这里其实就是等待多长时间。

|属性|意思|
|:--|:--|
|FixedBackOffPolicy| 默认固定延迟1秒后执行下一次重试|
|ExponentialBackOffPolicy| 指数递增延迟执行重试，默认初始0.1秒，系数是2，那么下次延迟0.2秒，再下次就是延迟0.4秒，如此类推，最大30秒。|
|ExponentialRandomBackOffPolicy| 在上面那个策略上增加随机性|
|UniformRandomBackOffPolicy| 这个跟上面的区别就是，上面的延迟会不停递增，这个只会在固定的区间随机|
|StatelessBackOffPolicy| 这个说明是无状态的，所谓无状态就是对上次的退避无感知，从它下面的子类也能看出来|

## 四、总结

天下代码一大抄，看你会抄不会抄。发现无论是guava还是spring的重试，基本都是类似的思路。只是看谁的功能比较鉴权而已。
guava提供了基础的能力，你任意封装。
spring基于spring提供了已经完好的能力，直接使用就好。不过因为是spring给你封装的能力，所以你要先了解清楚才行。不然可能使用错误，造成故障。

以上两款工具都挺好，不过他们都不支持分布式重试的能力。不过这已经满足我们的日常开发了，如果真遇到分布式的重试，就自己来实现咯。
