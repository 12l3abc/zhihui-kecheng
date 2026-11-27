package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.dto.SharedCoursewareVO;
import com.example.onlinecourseai.entity.CoursewareShare;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课件共享表 Mapper
 */
@Mapper
public interface CoursewareShareMapper {

    /** 共享广场卡片字段：一次查询补齐标题、作者、收藏状态和题目数量 */
    String CARD_SELECT = "SELECT s.id AS id, s.animation_id AS animation_id, " +
            "s.teacher_id AS owner_id, u.real_name AS owner_name, " +
            "a.title AS title, a.summary AS summary, a.knowledge_point AS knowledge_point, " +
            "s.subject AS subject, s.note AS note, s.status AS status, " +
            "s.view_count AS view_count, s.favorite_count AS favorite_count, " +
            "s.published_at AS published_at, " +
            "CASE WHEN f.id IS NULL THEN 0 ELSE 1 END AS favorited, " +
            "(SELECT COUNT(*) FROM question q WHERE q.animation_id = a.id) AS question_count " +
            "FROM courseware_share s " +
            "JOIN animation a ON a.id = s.animation_id " +
            "JOIN sys_user u ON u.id = s.teacher_id " +
            "LEFT JOIN courseware_favorite f ON f.share_id = s.id AND f.teacher_id = #{teacherId} ";

    @Select("SELECT * FROM courseware_share WHERE animation_id = #{animationId}")
    CoursewareShare findByAnimationId(Long animationId);

    @Select("SELECT * FROM courseware_share WHERE animation_id = #{animationId} AND status = 1")
    CoursewareShare findActiveByAnimationId(Long animationId);

    @Select("SELECT * FROM courseware_share WHERE id = #{id}")
    CoursewareShare findById(Long id);

    @Insert("INSERT INTO courseware_share(animation_id, teacher_id, subject, note, status, " +
            "view_count, favorite_count, published_at, updated_at) " +
            "VALUES(#{animationId}, #{teacherId}, #{subject}, #{note}, 1, 0, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CoursewareShare share);

    /** 重新发布或更新共享信息 */
    @Update("UPDATE courseware_share SET teacher_id = #{teacherId}, subject = #{subject}, " +
            "note = #{note}, status = 1, published_at = NOW(), updated_at = NOW() " +
            "WHERE animation_id = #{animationId}")
    int republish(CoursewareShare share);

    @Update("UPDATE courseware_share SET status = 0, updated_at = NOW() " +
            "WHERE animation_id = #{animationId} AND teacher_id = #{teacherId}")
    int unpublish(@Param("animationId") Long animationId, @Param("teacherId") Long teacherId);

    @Update("UPDATE courseware_share SET view_count = view_count + 1 WHERE id = #{id} AND status = 1")
    int incrementView(Long id);

    @Update("UPDATE courseware_share SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    int incrementFavorite(Long id);

    @Update("UPDATE courseware_share SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = #{id}")
    int decrementFavorite(Long id);

    @Select(CARD_SELECT + "WHERE s.id = #{shareId}")
    SharedCoursewareVO findCardById(@Param("shareId") Long shareId, @Param("teacherId") Long teacherId);

    @Select(CARD_SELECT + "WHERE s.status = 1 AND a.status = 'SUCCESS' " +
            "AND (s.subject = #{subject} OR #{subject} = '') " +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.knowledge_point LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.summary LIKE CONCAT('%', #{keyword}, '%') OR #{keyword} = '') " +
            "ORDER BY s.published_at DESC")
    List<SharedCoursewareVO> findPlaza(@Param("teacherId") Long teacherId,
                                       @Param("subject") String subject,
                                       @Param("keyword") String keyword);

    @Select(CARD_SELECT + "WHERE s.status = 1 AND a.status = 'SUCCESS' AND f.id IS NOT NULL " +
            "AND (s.subject = #{subject} OR #{subject} = '') " +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.knowledge_point LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.summary LIKE CONCAT('%', #{keyword}, '%') OR #{keyword} = '') " +
            "ORDER BY s.published_at DESC")
    List<SharedCoursewareVO> findFavorites(@Param("teacherId") Long teacherId,
                                           @Param("subject") String subject,
                                           @Param("keyword") String keyword);

    @Select(CARD_SELECT + "WHERE s.teacher_id = #{teacherId} " +
            "ORDER BY s.updated_at DESC")
    List<SharedCoursewareVO> findMine(@Param("teacherId") Long teacherId);
}
