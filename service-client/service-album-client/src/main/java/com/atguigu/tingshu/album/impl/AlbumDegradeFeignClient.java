package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlbumDegradeFeignClient implements AlbumFeignClient {

    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑模块]提供远程调用getAlbumInfo服务降级");
        return null;
    }
}
