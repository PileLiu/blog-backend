package com.llpy.textservice.service;

import com.llpy.model.Result;
import com.llpy.textservice.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.llpy.textservice.entity.dto.ArticleDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author HXB
 * @since 2024-06-08
 */
public interface ArticleService extends IService<Article> {

    Result<?> uploadImg(MultipartFile file);

    Result<?> addArticle(ArticleDto articleDto, Long userId);
}
