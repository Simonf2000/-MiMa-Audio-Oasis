package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlbumInfoService extends IService<AlbumInfo> {


    void saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo);

    void saveAlbumStat(Long albumId, String statType, int num);

    Page<AlbumListVo> getUserAlbumPage(AlbumInfoQuery albumInfoQuery, Page<AlbumListVo> pageInfo);

    void removeAlbumInfo(Long id);

    AlbumInfo getAlbumInfo(Long id);

    void updateAlbumInfo(AlbumInfo albumInfo);
}
