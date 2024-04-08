package com.atguigu.tingshu.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;


    /**
     * 发送Kafka消息
     *
     * @param topic 话题名称
     * @param data  业务数据 StringSerializer
     */
    public void sendKafkaMessage(String topic, String data) {
        this.sendKafkaMessage(topic, null, data);
    }

    /**
     * 发送Kafka消息
     *
     * @param topic 话题名称
     * @param key   可选参数：消息Key
     * @param data  业务数据 StringSerializer
     */
    public void sendKafkaMessage(String topic, String key, String data) {
        //1.发送消息后得到异步任务
        CompletableFuture sendResultCompletableFuture = kafkaTemplate.send(topic, key, data);
        //2.获取异步任务正常回调
        //3.获取异步任务异常回调
        sendResultCompletableFuture.whenCompleteAsync((t, e) -> {
            if (e != null) {
                logger.error("[Kafka生产者]发送消息异常：{}", e);
            } else {
                logger.info("[Kafka生产者]发送消息成功,话题名称：{}, key:{}, 消息：{}", topic, key, data);
            }
        });
    }

}
