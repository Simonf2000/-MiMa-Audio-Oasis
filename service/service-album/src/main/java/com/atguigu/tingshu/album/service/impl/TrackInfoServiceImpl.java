package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
