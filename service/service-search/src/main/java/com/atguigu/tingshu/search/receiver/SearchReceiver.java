package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/10/15:18
 * @Description:
 */
@Slf4j
@Component
public class SearchReceiver {


    @Autowired
    private SearchService searchService;

    /**
     * 监听到专辑上架消息
     * 该消费者保存专辑相当于新增或修改，具备幂等性
     *
     * @param consumerRecord
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[搜索服务]监听到专辑{}上架", value);
            searchService.upperAlbum(Long.valueOf(value));
        }
    }



    /**
     * 监听到专辑下架消息
     *  删除专辑也具备幂等性
     *
     * @param consumerRecord
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[搜索服务]监听到专辑{}下架", value);
            searchService.lowerAlbum(Long.valueOf(value));
        }
    }
}
