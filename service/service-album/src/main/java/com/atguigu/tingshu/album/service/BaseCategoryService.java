package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 查询所有分类（1、2、3级分类）
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    List<BaseAttribute> getAttributeByCategory1Id(Long category1Id);

    BaseCategoryView getCategoryView(Long category3Id);

    List<BaseCategory3> getTop7Category3(Long category1Id);

    JSONObject getBaseCategoryListByCategory1Id(Long category1Id);
}
