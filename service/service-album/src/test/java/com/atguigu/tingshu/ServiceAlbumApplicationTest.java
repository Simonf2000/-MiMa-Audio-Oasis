package com.atguigu.tingshu;

import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ServiceAlbumApplicationTest {

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Test
    public void test(){
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        System.out.println(baseCategoryViews);
    }

}
