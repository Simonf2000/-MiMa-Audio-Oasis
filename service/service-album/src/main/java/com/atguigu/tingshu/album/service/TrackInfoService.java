package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    Map<String, String> uploadTrack(MultipartFile trackFile);

    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    void saveTrackStat(Long trackId, String statType, int num);

    Page<TrackListVo> getUserTrackByPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    void removeTrackInfo(Long id);

    void updateTrackInfo(TrackInfo trackInfo);

    Page<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long userId, Long albumId);

    void updateTrackStat(TrackStatMqVo trackStatMqVo);
}
