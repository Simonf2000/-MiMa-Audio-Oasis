package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    Page<AlbumListVo> getUserAlbumPage(Page<AlbumListVo> pageInfo,@Param("vo") AlbumInfoQuery albumInfoQuery);

    AlbumStatVo getAlbumStatVo(Long albumId);

    void updateStat(@Param("albumId") Long albumId, @Param("statType") String statType, @Param("count") Integer count);
}
