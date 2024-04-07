package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private VodUploadClient vodUploadClient;

    @Autowired
    private VodConstantProperties properties;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private VodService vodService;

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Override
    public Map<String, String> uploadTrack(MultipartFile trackFile) {
        try {
            //1.将用户提交文件保存到本地
            String tempFilePath = UploadFileUtil.uploadTempPath(properties.getTempPath(), trackFile);
            //2.调用云点播SDK方法进行文件上传
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(tempFilePath);
            request.setCoverFilePath("");
            VodUploadResponse response = vodUploadClient.upload(properties.getRegion(), request);
            //3.解析点播平台响应结果获取：文件播放地址及文件唯一标识
            if (response != null) {
                String mediaFileId = response.getFileId();
                String mediaUrl = response.getMediaUrl();
                Map<String, String> map = new HashMap<>();
                map.put("mediaUrl", mediaUrl);
                map.put("mediaFileId", mediaFileId);
                return map;
            }
            return null;
        } catch (Exception e) {
            log.error("[专辑服务]音频文件上传异常：{}", e);
            throw new GuiguException(500, "上传音频文件异常！");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
        //将声音VO转为PO对象
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
        // 设置声音对象中：用户ID  来源  状态
        trackInfo.setUserId(userId);
        trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
        if (trackMediaInfoVo != null) {
            // 音频文件时长
            trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
            // 音频文件类型
            trackInfo.setMediaType(trackMediaInfoVo.getType());
            // 音频文件大小
            trackInfo.setMediaSize(trackMediaInfoVo.getSize());
        }
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
        trackInfoMapper.insert(trackInfo);

        Long trackId = trackInfo.getId();

        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PLAY, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COLLECT, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PRAISE, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COMMENT, 0);

        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        albumInfoMapper.updateById(albumInfo);
    }

    @Override
    public void saveTrackStat(Long trackId, String statType, int num) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(num);
        trackStatMapper.insert(trackStat);
    }

    @Override
    public Page<TrackListVo> getUserTrackByPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.getUserTrackByPage(pageInfo, trackInfoQuery);
    }
}
