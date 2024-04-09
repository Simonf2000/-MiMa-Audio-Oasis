package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    /**
     * 查询所有分类（1、2、3级分类）
     * TODO :每次查询都需要查询DB，将来将读多写少数据放入缓存Redis中
     * @return
     */
    @Override
    public List<JSONObject> getBaseCategoryList() {
        //1.获取分类视图中所有分类数据 共计419条记录
        List<BaseCategoryView> list = baseCategoryViewMapper.selectList(null);
        //2.处理一级分类
        //2.1 构建一级分类JSON对象集合
        List<JSONObject> jsonObjectList = new ArrayList<>();

        //2.2 对所有分类集合进行分组：按照1分类ID进行分组
        Map<Long, List<BaseCategoryView>> category1Map = list
                .stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //2.3 构建一级分类JOSN对象
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            JSONObject jsonObject1 = new JSONObject();
            Long category1Id = entry1.getKey();
            String category1Name = entry1.getValue().get(0).getCategory1Name();
            //2.3.1 封装一级分类ID
            jsonObject1.put("categoryId", category1Id);
            //2.3.2 封装一级分类名称
            jsonObject1.put("categoryName", category1Name);
            //3.处理二级分类
            //3.1 对1级分类集合进行分组：按照2级分类ID进行分组-得到二级分类Map
            Map<Long, List<BaseCategoryView>> category2Map = entry1.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //3.2 创建二级分类JSON对象集合
            List<JSONObject> jsonObject2List = new ArrayList<>();
            //3.3 遍历二级分类Map 封装二级分类对象
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //3.3.1 构建二级分类JSON对象
                JSONObject jsonObject2 = new JSONObject();
                //3.3.2 封装二级分类ID
                Long category2Id = entry2.getKey();
                jsonObject2.put("categoryId", category2Id);
                //3.3.2 封装二级分类名称
                String category2Name = entry2.getValue().get(0).getCategory2Name();
                jsonObject2.put("categoryName", category2Name);
                //3.4 将二级分类对象加入到二级分类JOSN集合中
                jsonObject2List.add(jsonObject2);
                //4.处理三级分类
                //4.1 创建三级分类JSON对象集合
                List<JSONObject> jsonObject3List = new ArrayList<>();
                //4.2 遍历二级分类对象
                for (BaseCategoryView baseCategoryView : entry2.getValue()) {
                    //4.2.1 构建三级分类JSON对象
                    JSONObject jsonObject3 = new JSONObject();
                    //4.2.2 封装三级分类ID
                    jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                    //4.2.3 封装三级分类名称
                    jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                    //4.3 将三级分类JSON对象放入三级分类JSON集合中
                    jsonObject3List.add(jsonObject3);
                    //4.2 将三级分类JSON集合放入二级分类对象中“categoryChild”属性中
                    jsonObject2.put("categoryChild", jsonObject3List);
                }

                //3.5 将二级分类JOSN集合加入到一级分类对象中“categoryChild”属性中
                jsonObject1.put("categoryChild", jsonObject2List);
            }
            //2.4 将一级分类JOSN对象放入到一级分类JSON对象集合
            jsonObjectList.add(jsonObject1);
        }
        //5.返回所有一级分类JSON对象集合
        return jsonObjectList;
    }

    @Override
    public List<BaseAttribute> getAttributeByCategory1Id(Long category1Id) {
        return baseAttributeMapper.getAttributeByCategory1Id(category1Id);
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }
}
