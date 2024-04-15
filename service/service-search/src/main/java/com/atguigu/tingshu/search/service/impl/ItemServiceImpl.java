package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * 获取渲染专辑详情页面所需数据
     *
     * @param albumId
     * @return {albumInfo:{},baseCategoryView:{},albumStatVo:{},announcer:{}}
     */
    @Override
    public Map<String, Object> getItemData(Long albumId) {
        //1.创建结果Map对象 存在多线程写Map，提供线程安全聚合类ConcurrentHashMap
        Map<String, Object> map = new ConcurrentHashMap<>();

        //2.远程调用"专辑服务"根据ID查询专辑消息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑：{}不存在！", albumId);
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);

        //3.远程调用"专辑服务"根据专辑三级分类ID查询分类消息
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "专辑分类：{}不存在！", albumId);
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);

        //4.远程调用"专辑服务"根据专辑ID查询统计消息
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑统计：{}不存在！", albumId);
            map.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);

        //5.远程调用"用户服务"根据专辑用户ID查询用户信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "专辑主播：{}不存在！", albumId);
            map.put("announcer", userInfoVo);
        }, threadPoolExecutor);

        //6.组合异步任务
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                albumStatVoCompletableFuture,
                baseCategoryViewCompletableFuture,
                announcerCompletableFuture
        ).join();
        return map;
    }
}
