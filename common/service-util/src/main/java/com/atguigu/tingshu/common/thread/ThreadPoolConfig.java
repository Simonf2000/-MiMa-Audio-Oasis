package com.atguigu.tingshu.common.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/10/10:22
 * @Description:
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //1.动态得到线程数 IO密集型：CPU逻辑处理器个数*2
        int processorsCount = Runtime.getRuntime().availableProcessors();
        int coreCount = processorsCount * 2;

        //2.通过线程池构造创建线程池对象
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreCount,
                coreCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                (r, e) -> {
                    //r:被拒绝任务  e:线程池对象
                    //自定义拒绝策略：重试-将任务再次提交给线程执行
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    e.submit(r);
                }
        );
        //3.线程池核心线程第一个任务提交才创建
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }
}
