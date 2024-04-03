package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/03/9:06
 * @Description:
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties properties;

    @Override
    public String fileUpload(MultipartFile file) {
        //TODO 图片大小
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new GuiguException(400, "图片格式非法!");
            }
            String extName = FileNameUtil.extName(file.getOriginalFilename());
            String objectName = "/" + DateUtil.today() + "/" + IdUtil.randomUUID() + "." + extName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(objectName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return properties.getEndpointUrl() + "/" + properties.getBucketName() + objectName;
        } catch (Exception e) {
            log.error("[专辑服务]文件上传接口异常:", e);
            throw new RuntimeException("文件上传接口异常");
        }
    }
}
