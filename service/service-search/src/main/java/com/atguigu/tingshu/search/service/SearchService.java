package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);

    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery);

    AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery);

    List<Map<String, Object>> getTop6AlbumByCategory1(Long category1Id);

    void saveSuggestDoc(AlbumInfoIndex albumInfoIndex);

    List<String> completeSuggest(String keyword);

    Collection<String> parseSuggestResult(String s, Map<String, List<Suggestion<SuggestIndex>>> suggestMap);

    void updateLatelyAlbumRanking();

    List<AlbumInfoIndex> getRankingList(Long category1Id, String dimension);
}
