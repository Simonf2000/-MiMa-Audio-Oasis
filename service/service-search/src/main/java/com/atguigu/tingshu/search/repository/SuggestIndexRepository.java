package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/15/9:33
 * @Description:
 */
public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex, String> {
}
