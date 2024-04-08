package com.atguigu.tingshu.account.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/08/9:54
 * @Description:
 */
@Slf4j
@Component
public class AccountReceiver {


    @Autowired
    private UserAccountService userAccountService;



    /**
     * 每个消费者：1.是否需要做幂等性处理  2.是否需要进行事务管理
     * 初始化账户记录
     *
     * @param consumerRecord
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void initAccount(ConsumerRecord<String, String> consumerRecord) {
        String userId = consumerRecord.value();
        if (StringUtils.isNotBlank(userId)) {
            log.info("[账户服务]消费者，监听到初始化账户消息：{}", userId);
            userAccountService.initAccount(Long.valueOf(userId));
        }
    }
}