package com.llpy.textservice.entity.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 日记vo
 *
 * @author LLPY
 * @date 2023/11/09
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "DiaryDto对象", description = "日记返回和添加对象")
public class DiaryVo {
    private Long diaryId;

    private Long userId;

    //用户名称
    private String nickname;

    private String userImg;

    private String diaryTitle;

    private String diaryText;

    //是否公开
    private int isOpen;

    private String createTime;
}
