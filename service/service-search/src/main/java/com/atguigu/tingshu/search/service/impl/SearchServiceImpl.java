package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
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
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
            return this.parseResult(searchResponse, albumIndexQuery);
        } catch (Exception e) {
            log.error("[搜索服务]站内搜索异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
        //2.设置查询条件对应请求体参数中"query" 封装关键字、分类过滤、标签过滤
        String keyword = albumIndexQuery.getKeyword();
        //2.1 创建最外层组合多条件查询对象-封装所有查询条件
        BoolQuery.Builder allBoolQueryBuilder = new BoolQuery.Builder();
        //2.2 处理关键字检索 关键字全文检索专辑标题，简介。等值精确查询作者名称
        if (StringUtils.isNotBlank(keyword)) {
            allBoolQueryBuilder.must(
                    m -> m.bool(
                            b -> b.should(s -> s.match(ma -> ma.field("albumTitle").query(keyword)))
                                    .should(s -> s.match(ma -> ma.field("albumIntro").query(keyword)))
                                    .should(s -> s.term(t -> t.field("announcerName").value(keyword)))
                    )
            );
        }

        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }

        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }

        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            allBoolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }

        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollectionUtil.isNotEmpty(attributeList)) {
            //2.4.1 前端可能提交多个标签过滤条件 提交标签形式  标签id：标签值Id
            for (String s : attributeList) {
                //每循环一次，封装标签Nested查询
                String[] split = s.split(":");
                if (split != null && split.length == 2) {

                    allBoolQueryBuilder.filter(f -> f.nested(n ->
                            n.path("attributeValueIndexList")
                                    .query(q -> q.bool(
                                            b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                                    ))
                    ));
                }
            }
        }

        builder.query(allBoolQueryBuilder.build()._toQuery());

//        builder.query();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        int from = (pageNo - 1) * pageSize;
        builder.from(from).size(pageSize);

        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            // 对排序字符按照:进行切割
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                // 得到排序字段及排序方式 综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]
                String orderFiled = "";
                switch (split[0]) {
                    case "1":
                        orderFiled = "hotScore";
                        break;
                    case "2":
                        orderFiled = "playStatNum";
                        break;
                    case "3":
                        orderFiled = "createTime";
                        break;
                }

                SortOrder sortOrder = "asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc;
                String finalOrderFiled = orderFiled;
                builder.sort(s -> s.field(f -> f.field(finalOrderFiled).order(sortOrder)));
            }
        }
        if (StringUtils.isNotBlank(albumIndexQuery.getKeyword())) {
            builder.highlight(h -> h.fields("albumTitle", f -> f.preTags("<font style='color:red'>").postTags("</font>")));
        }
        builder.source(s -> s.filter(f -> f.excludes("isFinished", "category1Id", "category2Id", "category3Id", "hotScore", "attributeValueIndexList.attributeId", "attributeValueIndexList.valueId")));
        return builder.build();
    }

    @Override
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();
        HitsMetadata<AlbumInfoIndex> hits = searchResponse.hits();
        long total = hits.total().value();
        Integer pageSize = albumIndexQuery.getPageSize();
        long totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;

        vo.setTotal(total);
        vo.setTotalPages(totalPages);
        vo.setPageNo(albumIndexQuery.getPageNo());
        vo.setPageSize(pageSize);

        List<Hit<AlbumInfoIndex>> hitList = hits.hits();
        if (CollectionUtil.isNotEmpty(hitList)) {
            List<AlbumInfoIndexVo> list = hitList.stream()
                    .map(hit -> {
                        AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
                        Map<String, List<String>> highlight = hit.highlight();
                        if (CollectionUtil.isNotEmpty(highlight)) {
                            String albumTitleHighlight = highlight.get("albumTitle").get(0);
                            albumInfoIndexVo.setAlbumTitle(albumTitleHighlight);
                        }
                        return albumInfoIndexVo;
                    }).collect(Collectors.toList());
            vo.setList(list);
        }
        return vo;
    }
}

