package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;


    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, "");
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);

            if (resp != null) {
                MediaInfo mediaInfo = resp.getMediaInfoSet()[0];

                if (mediaInfo != null) {

                    //5.1 获取音频文件基本信息-获取文件类型
                    MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                    String type = basicInfo.getType();
                    trackMediaInfoVo.setType(type);
                    //5.2 获取音频文件元信息-获取文件时长、大小
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    Float audioDuration = metaData.getAudioDuration();
                    trackMediaInfoVo.setDuration(audioDuration);
                    Long size = metaData.getSize();
                    trackMediaInfoVo.setSize(size);

                    return trackMediaInfoVo;
                }
            }

            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return null;
    }
}
