package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.Question;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 题目表 Mapper
 */
@Mapper
public interface QuestionMapper {

    @Insert("INSERT INTO question(animation_id, question_text, options, answer, question_type, score) " +
            "VALUES(#{animationId}, #{questionText}, #{options}, #{answer}, #{questionType}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    @Select("SELECT * FROM question WHERE animation_id = #{animationId} ORDER BY id")
    List<Question> findByAnimationId(Long animationId);

    @Select("SELECT * FROM question WHERE id = #{id}")
    Question findById(Long id);

    /** 删除某课件下的全部题目（删除课件前先清理） */
    @Delete("DELETE FROM question WHERE animation_id = #{animationId}")
    int deleteByAnimationId(Long animationId);

    /** 某课件的题目总分（任务满分） */
    @Select("SELECT IFNULL(SUM(score), 0) FROM question WHERE animation_id = #{animationId}")
    int sumScoreByAnimationId(Long animationId);

    /** 某课件主观题满分之和（教师批阅主观分封顶用） */
    @Select("SELECT IFNULL(SUM(score), 0) FROM question WHERE animation_id = #{animationId} AND question_type = 'subjective'")
    int sumSubjectiveScoreByAnimationId(Long animationId);

    /** 更新题目答案（教师修改客观题答案用） */
    @Update("UPDATE question SET answer = #{answer} WHERE id = #{id}")
    int updateAnswer(Long id, String answer);

    /** 按课件 + 题干模糊匹配题目（兼容旧前端，取第一条） */
    @Select("SELECT * FROM question WHERE animation_id = #{animationId} " +
            "AND question_text LIKE CONCAT('%', #{questionText}, '%') " +
            "ORDER BY id LIMIT 1")
    Question findByText(Long animationId, String questionText);

    /** 按课件 + 题干模糊匹配客观题（取第一条） */
    @Select("SELECT * FROM question WHERE animation_id = #{animationId} " +
            "AND question_type = 'objective' AND question_text LIKE CONCAT('%', #{questionText}, '%') " +
            "ORDER BY id LIMIT 1")
    Question findObjectiveByText(Long animationId, String questionText);
}
