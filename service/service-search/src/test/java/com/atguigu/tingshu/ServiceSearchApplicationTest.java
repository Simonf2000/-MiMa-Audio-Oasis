package com.atguigu.tingshu;

import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/10/15:37
 * @Description:
 */
@Slf4j
@SpringBootTest
public class ServiceSearchApplicationTest {
    @Autowired
    private SearchService searchService;

    /**
     * 批量导入
     * 不严谨批量导入
     */
    @Test
    public void testBatchImport() {
        for (long i = 1; i < 1606; i++) {
            try {
                searchService.upperAlbum(i);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}
