package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

public interface VodService {

    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    void deleteMedia(String mediaFileId);

    String startReviewMediaTask(String mediaFileId);

    String getReviewMediaTaskResult(String reviewTaskId);
}
