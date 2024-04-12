package com.atguigu.tingshu.job;

import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/12/18:47
 * @Description:
 */
@Component
@EnableScheduling
public class ReviewResultTask {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private VodService vodService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void updateReviewResult() {
        //1.查询声音表中审核状态为：审核中的 声音列表 获取审核任务ID
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getStatus, SystemConstant.TRACK_STATUS_REVIEWING);
        queryWrapper.select(TrackInfo::getId, TrackInfo::getReviewTaskId);
        //接口请求频率限制：100次/秒。
        queryWrapper.last("limit 90");
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(queryWrapper);
        //2.调用点播平台任务的结果
        if (CollectionUtil.isNotEmpty(trackInfoList)) {
            for (TrackInfo trackInfo : trackInfoList) {
                String reviewTaskId = trackInfo.getReviewTaskId();
                if(StringUtils.isNotBlank(reviewTaskId)){
                    String suggestion = vodService.getReviewMediaTaskResult(reviewTaskId);
                    if (StringUtils.isBlank(suggestion)) {
                        continue;
                    }
                    if ("pass".equals(suggestion)) {
                        //审核通过
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
                    } else if ("block".equals(suggestion) || "review".equals(suggestion)) {
                        //审核不通过
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
                    }
                    trackInfoMapper.updateById(trackInfo);
                }
            }
        }
    }

}
