package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.CoursewareFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 共享课件收藏 Mapper
 */
@Mapper
public interface CoursewareFavoriteMapper {

    @Select("SELECT COUNT(*) FROM courseware_favorite WHERE teacher_id = #{teacherId} AND share_id = #{shareId}")
    int countByTeacherAndShare(@Param("teacherId") Long teacherId, @Param("shareId") Long shareId);

    /** INSERT IGNORE 保证重复收藏不会产生第二条记录 */
    @Insert("INSERT IGNORE INTO courseware_favorite(teacher_id, share_id, created_at) " +
            "VALUES(#{teacherId}, #{shareId}, NOW())")
    int insert(CoursewareFavorite favorite);

    @Delete("DELETE FROM courseware_favorite WHERE teacher_id = #{teacherId} AND share_id = #{shareId}")
    int delete(@Param("teacherId") Long teacherId, @Param("shareId") Long shareId);
}
