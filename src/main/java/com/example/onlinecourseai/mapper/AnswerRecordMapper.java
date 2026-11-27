package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.AnswerRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 作答记录表 Mapper
 */
@Mapper
public interface AnswerRecordMapper {

    @Insert("INSERT INTO answer_record(task_id, question_id, student_id, answer_text, is_correct, score, submit_time) " +
            "VALUES(#{taskId}, #{questionId}, #{studentId}, #{answerText}, #{isCorrect}, #{score}, #{submitTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnswerRecord record);

    /** 某学生某任务的全部作答记录 */
    @Select("SELECT * FROM answer_record WHERE task_id = #{taskId} AND student_id = #{studentId}")
    List<AnswerRecord> findByTaskAndStudent(Long taskId, Long studentId);

    /** 教师打回重做：清空该生本次全部作答记录 */
    @Delete("DELETE FROM answer_record WHERE task_id = #{taskId} AND student_id = #{studentId}")
    int deleteByTaskAndStudent(Long taskId, Long studentId);

    /** 某任务的提交人数（按学生去重） */
    @Select("SELECT COUNT(DISTINCT student_id) FROM answer_record WHERE task_id = #{taskId}")
    int countSubmissionByTaskId(Long taskId);

    /** 教师批阅时修改学生答案：回写作答文本与客观判分结果 */
    @Update("UPDATE answer_record SET answer_text = #{answerText}, is_correct = #{isCorrect}, score = #{score} " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId} AND question_id = #{questionId}")
    int updateAnswer(AnswerRecord record);
}
