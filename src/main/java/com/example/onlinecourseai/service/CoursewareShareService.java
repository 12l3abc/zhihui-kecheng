package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.SharedCoursewareVO;

import java.util.List;

/**
 * 课件共享：发布到共享广场、收藏、复用。
 */
public interface CoursewareShareService {

    /** 发布/更新自己的课件共享信息 */
    SharedCoursewareVO publish(Long teacherId, Long animationId, String subject, String note);

    /** 取消发布，仅下架共享记录，不删除原课件 */
    void unpublish(Long teacherId, Long animationId);

    /** 我发布过的共享记录（含已下架） */
    List<SharedCoursewareVO> listMine(Long teacherId);

    /** 共享广场：已发布课件，可按学科和关键词筛选 */
    List<SharedCoursewareVO> listPlaza(Long teacherId, String subject, String keyword);

    /** 我收藏的共享课件 */
    List<SharedCoursewareVO> listFavorites(Long teacherId, String subject, String keyword);

    /** 查看共享详情前校验并取卡片信息 */
    SharedCoursewareVO getPublished(Long shareId, Long teacherId);

    /** 浏览计数 +1 */
    void markViewed(Long shareId);

    /** 收藏 / 取消收藏，返回操作后的收藏状态 */
    boolean setFavorite(Long teacherId, Long shareId, boolean favorite);

    /** 复制一份共享课件到当前教师名下，返回新课件 ID */
    Long copyToMy(Long teacherId, Long shareId);
}
