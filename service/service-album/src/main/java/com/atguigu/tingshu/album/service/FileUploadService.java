package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/03/9:06
 * @Description:
 */
public interface FileUploadService {

    String fileUpload(MultipartFile file);
}
