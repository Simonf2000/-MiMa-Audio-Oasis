package com.atguigu.tingshu.search;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/09/18:47
 * @Description:
 */
public interface AlbumInfoIndexRepository extends ElasticsearchRepository<AlbumInfoIndex, Long> {
}
