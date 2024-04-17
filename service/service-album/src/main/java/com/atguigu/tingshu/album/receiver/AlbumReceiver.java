package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
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
 * @Date: 2024/04/16/20:45
 * @Description:
 */
@Slf4j
@Component
public class AlbumReceiver {

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 监听到声音统计消息
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateTrackStat(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[专辑服务]监听到更新声音统计信息消息：{}", value);
            TrackStatMqVo trackStatMqVo = JSON.parseObject(value, TrackStatMqVo.class);
            trackInfoService.updateTrackStat(trackStatMqVo);
        }
    }

}

