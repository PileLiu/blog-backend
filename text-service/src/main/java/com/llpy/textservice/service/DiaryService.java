package com.llpy.textservice.service;


import com.llpy.model.Result;
import com.llpy.textservice.entity.vo.DiaryVo;

import java.util.List;

public interface DiaryService {
    Result addDiary(DiaryVo diaryVo, Long id);

    Result<List<DiaryVo>> getDiary();

    Result<List<DiaryVo>> getDiary(Long userId);

    Result<?> getDiaryBase(Long userId, Integer pageSize, Integer pageNum);

    Result<DiaryVo> getOneText(Long diaryId);

    Result deleteDiaryOne(Long diaryId);
}
