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
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

        String reviewMediaTaskId = vodService.startReviewMediaTask(trackInfo.getMediaFileId());
        trackInfo.setReviewTaskId(reviewMediaTaskId);
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEWING);
        trackInfoMapper.updateById(trackInfo);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long id) {
        //获取要被删除声音信息 得到 当前声音序号
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();
        //删除声音记录
        trackInfoMapper.deleteById(id);

        //删除声音统计信息
        LambdaQueryWrapper<TrackStat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackStat::getTrackId, id);
        trackStatMapper.delete(queryWrapper);
        //根据序号修改比当前声音序号大的声音序号
        trackInfoMapper.updateOrderNum(albumId, orderNum);
        vodService.deleteMedia(trackInfo.getMediaFileId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateTrackInfo(TrackInfo trackInfo) {
        //1.根据声音ID查询声音信息
        TrackInfo oldtrackInfo = trackInfoMapper.selectById(trackInfo.getId());
        //2.判断音频信息是否发生变化
        String oldMediaFileId = oldtrackInfo.getMediaFileId();

        if (!oldMediaFileId.equals(trackInfo.getMediaFileId())) {
            //3.调用腾讯云点播平台获取最新音频文件信息 更新 声音中音频信息
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfo.getMediaFileId());
            if (mediaInfo != null) {
                trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
                trackInfo.setMediaType(mediaInfo.getType());
                trackInfo.setMediaSize(mediaInfo.getSize());
                vodService.deleteMedia(oldMediaFileId);
            }
        }
        //更新声音信息
        trackInfoMapper.updateById(trackInfo);
    }

    /**
     * 分页展示专辑下声音列表-动态显示付费标识
     * 根据用户登录状态、身份、专辑付费类型、购买情况综合判断付费标识
     *
     * @param pageInfo 分页对象
     * @param userId   用户ID
     * @param albumId  专辑ID
     * @return
     */
    @Override
    public Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long userId, Long albumId) {
        //1.获取指定专辑下包含声音分页列表（不考虑付费标识 AlbumTrackListVo中付费标识：false）
        pageInfo = trackInfoMapper.getAlbumTrackPage(pageInfo, albumId);
        //2.TODO 根据用户登录状态、身份、专辑付费类型、购买情况综合判断付费标识
        //2. 根据专辑ID查询专辑信息-获取专辑付费类型-付费类型: 0101-免费、0102-vip免费、0103-付费
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        //2.1 获取专辑付费类型
        String payType = albumInfo.getPayType();
        //2.2 获取专辑免费试听集数
        Integer tracksForFreeCount = albumInfo.getTracksForFree();

        //3. 处理用户未登录情况
        if (userId == null) {
            //3.1 判断专辑付费类型是否为VIP免费或者付费
            if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType) || SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                //3.2 将本页中声音列表除去免费试听声音将声音付费标识设置为：true
                pageInfo.getRecords()
                        .stream()
                        .filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > tracksForFreeCount) //去除免费试听声音
                        .forEach(albumTrackListVo -> {
                            albumTrackListVo.setIsShowPaidMark(true);
                        });
            }
        }
        return pageInfo;
    }
}
