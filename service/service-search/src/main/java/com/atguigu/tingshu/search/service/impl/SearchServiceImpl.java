package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
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

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

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

        //TODO 将发布专辑名称存入提词索引库中
        this.saveSuggestDoc(albumInfoIndex);
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
        //TODO 删除题词库索引库中文档
        suggestIndexRepository.deleteById(albumId.toString());
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

    @Override
    public List<Map<String, Object>> getTop6AlbumByCategory1(Long category1Id) {
        try {
            //1.远程调用"专辑服务"获取1级分类下置顶7个三级分类列表
            List<BaseCategory3> baseCategory3List = albumFeignClient.getTopBaseCategory3(category1Id).getData();
            Assert.notNull(baseCategory3List, "该分类{}下无三级分类", category1Id);
            //1.1 遍历得到所有三级分类ID集合
            List<Long> baseCategory3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
            //1.2 执行多关键字检索需要List<FieldValue> 故 转换类型
            List<FieldValue> fieldValueList = baseCategory3IdList.stream()
                    .map(category3Id -> FieldValue.of(category3Id)).collect(Collectors.toList());

            //1.3 后续解析结果方便获取三级分类对象 将三级集合转为Map Key:三级分类ID Value：三级分类对象
            Map<Long, BaseCategory3> baseCategory3Map = baseCategory3List
                    .stream()
                    .collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));

            //2.执行ES检索
            SearchResponse<AlbumInfoIndex> searchResponse =
                    elasticsearchClient.search(
                            s -> s.index(INDEX_NAME)
                                    .query(q -> q.terms(t -> t.field("category3Id").terms(t1 -> t1.value(fieldValueList))))
                                    .size(0)
                                    .aggregations(
                                            "category3_agg", a -> a.terms(t -> t.field("category3Id").size(20))
                                                    .aggregations("top6", a1 -> a1.topHits(t -> t.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc)))))
                                    ),
                            AlbumInfoIndex.class
                    );
            //3.解析ES响应结果，封装自定义结果List<Map<String, Object>>代表所有置顶三级分类热门专辑列表
            //3.1 获取ES响应中聚合结果对象，根据请求体参数聚合名称，获取三级分类聚合对象
            Aggregate category3_agg = searchResponse.aggregations().get("category3_agg");
            LongTermsAggregate category3Lterms = category3_agg.lterms();
            //3.2 获取三级分类聚合结果Buckets桶数组 遍历Bucket数组 每遍历一个Bucket处理一个置顶三级分类 Map
            List<LongTermsBucket> category3Buckets = category3Lterms.buckets().array();
            List<Map<String, Object>> list = category3Buckets.stream().map(category3Bucket -> {
                //3.3 获取三级分类ID
                long topCategory3Id = category3Bucket.key();
                //3.4 遍历当前三级分类聚合中子聚合得到热门专辑前6个列表
                Aggregate top6 = category3Bucket.aggregations().get("top6");
                List<AlbumInfoIndex> hotAlbumIndexList = top6.topHits().hits().hits()
                        .stream()
                        .map(hit -> {
                            String albumIndexStr = hit.source().toJson().toString();
                            AlbumInfoIndex hotAlbumInfoIndex = JSON.parseObject(albumIndexStr, AlbumInfoIndex.class);
                            return hotAlbumInfoIndex;
                        }).collect(Collectors.toList());
                //3.5 构建当前置顶三级分类热门专辑Map对象
                Map<String, Object> map = new HashMap<>();
                map.put("baseCategory3", baseCategory3Map.get(topCategory3Id));
                map.put("list", hotAlbumIndexList);
                return map;
            }).collect(Collectors.toList());
            return list;
        } catch (IOException e) {
            log.error("[搜索服务]置顶分类热门专辑异常：", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveSuggestDoc(AlbumInfoIndex albumInfoIndex) {
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        String albumTitle = albumInfoIndex.getAlbumTitle();
        suggestIndex.setTitle(albumTitle);
        suggestIndex.setKeyword(new Completion(new String[]{albumTitle}));
       String pinyin = PinyinUtil.getPinyin(albumTitle,"");
       suggestIndex.setKeywordPinyin(new Completion(new String[]{pinyin}));
       String firstLetter = PinyinUtil.getFirstLetter(albumTitle,"");
       suggestIndex.setKeywordSequence(new Completion(new String[]{firstLetter}));
        suggestIndexRepository.save(suggestIndex);
    }
}

