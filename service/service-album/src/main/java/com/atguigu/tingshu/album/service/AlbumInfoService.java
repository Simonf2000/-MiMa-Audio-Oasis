package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo);

    void saveAlbumStat(Long albumId, String statType, int num);
}
