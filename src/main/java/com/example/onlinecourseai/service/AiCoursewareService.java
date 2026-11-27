package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.CoursewareVO;
import com.example.onlinecourseai.entity.Animation;

import java.util.List;

public interface AiCoursewareService {

    /**
     * 根据知识点调用大模型生成动画课件，并存入数据库
     *
     * @param teacherId      教师ID
     * @param knowledgePoint 知识点描述
     * @return 生成完成的课件
     */
    CoursewareVO generateCourseware(Long teacherId, String knowledgePoint);

    /** 某教师生成过的全部课件列表 */
    List<CoursewareVO> getMyCoursewares(Long teacherId);

    /** 根据课件ID查询详情（含配套题目）；内部信任调用方，不做归属校验 */
    CoursewareVO getCoursewareDetail(Long coursewareId);

    /** 根据课件ID查询详情（带可见性校验）：教师限本人课件，学生限本人班级任务引用；广场浏览走 /api/share 接口 */
    CoursewareVO getCoursewareDetailVisible(Long coursewareId, Long userId, String role);

    /** 调用大模型对课件生成重点知识提炼，并更新 animation.summary */
    String generateSummary(Long teacherId, Long coursewareId);

    /** 更新课件标题、摘要与整块 HTML（仅课件创建者可操作）；contentHtml 为空则不改正文 */
    Animation updateCourseware(Long teacherId, Long coursewareId, String title, String summary, String contentHtml);

    /** 删除课件及其配套题目（已被任务引用时拒绝） */
    void deleteCourseware(Long teacherId, Long coursewareId);

    /** 教师修改题目答案：客观题校验选项，主观题保存参考范文 */
    boolean updateQuizAnswer(Long teacherId, Long coursewareId, Long questionId, String questionText, String answer);
}
