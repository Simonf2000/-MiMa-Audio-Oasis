package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private static final String INDEX_NAME = "albuminfo";

    /**
     * 将指定专辑封装专辑索引库文档对象，完成文档信息
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //1.远程调用专辑服务，获取专辑信息（包含专辑标签列表），属性拷贝到索引库文档对象
            //1.1 远程调用专辑服务
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑{}信息为空", albumId);

            //1.2 封装专辑文档对象专辑信息
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);

            //1.3 封装专辑文档对象专辑标签列表
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList
                        = albumAttributeValueVoList
                        .stream()
                        .map(albumAttributeValue -> BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class))
                        .collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //2.远程调用专辑服务,获取分类信息，封装三级分类ID
            BaseCategoryView categoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(categoryView, "分类{}信息为空", albumInfo.getCategory3Id());
            albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        }, threadPoolExecutor);


        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //3.远程调用用户服务，获取主播信息，封装主播名称
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "主播{}信息为空", albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        }, threadPoolExecutor);


        //4.TODO 为了方便进行检索测试，随机产生专辑统计数值 封装专辑统计信息
        //4.1 随机产生四个数值作为统计值
        int num1 = RandomUtil.randomInt(500, 2000);
        int num2 = RandomUtil.randomInt(500, 1500);
        int num3 = RandomUtil.randomInt(500, 1000);
        int num4 = RandomUtil.randomInt(500, 1000);
        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);

        //4.2 基于统计值计算当前文档热度 统计量*动态权重
        BigDecimal bigDecimal1 = new BigDecimal("0.1").multiply(new BigDecimal(num1));
        BigDecimal bigDecimal2 = new BigDecimal("0.2").multiply(new BigDecimal(num2));
        BigDecimal bigDecimal3 = new BigDecimal("0.3").multiply(new BigDecimal(num3));
        BigDecimal bigDecimal4 = new BigDecimal("0.4").multiply(new BigDecimal(num4));
        BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
        albumInfoIndex.setHotScore(hotScore.doubleValue());

        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                categoryCompletableFuture,
                userInfoCompletableFuture
        ).join();
        //5.调用文档持久层接口保存专辑
        albumInfoIndexRepository.save(albumInfoIndex);
        System.out.println(Thread.currentThread().getName() + "主线程执行");
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        try {
            SearchRequest searchRequest = this.buildDSL(albumIndexQuery);
            System.out.println("本次检索DSL:");
            System.out.println(searchRequest);
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
            return this.parseResult(searchResponse);
        } catch (Exception e) {
            log.error("[搜索服务]站内搜索异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
//        builder.query();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        int from = (pageNo - 1) * pageSize;
        builder.from(from).size(pageSize);
        //builder.sort();
        if (StringUtils.isNotBlank(albumIndexQuery.getKeyword())) {
            builder.highlight(h -> h.fields("albumTitle", f -> f.preTags("<font style='color:red'>").postTags("</font>")));
        }
        builder.source(s -> s.filter(f -> f.excludes("isFinished", "category1Id", "category2Id", "category3Id", "hotScore", "attributeValueIndexList.attributeId", "attributeValueIndexList.valueId")));
        return builder.build();


    }

    @Override
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse) {
        return null;
    }
}