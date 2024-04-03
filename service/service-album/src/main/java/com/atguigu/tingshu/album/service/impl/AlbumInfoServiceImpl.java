package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo) {
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        albumInfo.setUserId(userId);
        albumInfo.setTracksForFree(5);
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //当前实例对象的save方法
        this.save(albumInfo);
        Long albumId = albumInfo.getId();

        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
                AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                albumAttributeValue.setAlbumId(albumId);
                albumAttributeValueMapper.insert(albumAttributeValue);
            }
        }
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_PLAY, 1);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_SUBSCRIBE, 1);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_BUY, 1);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_COMMENT, 1);
    }

    @Override
    public void saveAlbumStat(Long albumId, String statType, int num) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(num);
        albumStatMapper.insert(albumStat);
    }
}
