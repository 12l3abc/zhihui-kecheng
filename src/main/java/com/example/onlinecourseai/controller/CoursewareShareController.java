package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.CoursewareVO;
import com.example.onlinecourseai.dto.SharedCoursewareVO;
import com.example.onlinecourseai.service.AiCoursewareService;
import com.example.onlinecourseai.service.CoursewareShareService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 教师端课件共享：发布 / 广场 / 收藏 / 复用
 */
@RestController
@RequestMapping("/api/share")
public class CoursewareShareController {

    private final CoursewareShareService shareService;
    private final AiCoursewareService aiCoursewareService;

    public CoursewareShareController(CoursewareShareService shareService,
                                     AiCoursewareService aiCoursewareService) {
        this.shareService = shareService;
        this.aiCoursewareService = aiCoursewareService;
    }

    /** 发布或更新共享信息 */
    @PostMapping("/publish/{animationId}")
    public Result<SharedCoursewareVO> publish(@PathVariable Long animationId,
                                              @RequestBody(required = false) Map<String, String> body,
                                              HttpSession session) {
        checkTeacher(session);
        Long teacherId = (Long) session.getAttribute("userId");
        Map<String, String> b = body == null ? Map.of() : body;
        return Result.success(shareService.publish(
                teacherId, animationId, b.get("subject"), b.get("note")));
    }

    /** 取消共享（只下架，不删除课件） */
    @DeleteMapping("/publish/{animationId}")
    public Result<Void> unpublish(@PathVariable Long animationId, HttpSession session) {
        checkTeacher(session);
        shareService.unpublish((Long) session.getAttribute("userId"), animationId);
        return Result.success();
    }

    /** 我发布的共享记录 */
    @GetMapping("/my")
    public Result<List<SharedCoursewareVO>> my(HttpSession session) {
        checkTeacher(session);
        return Result.success(shareService.listMine((Long) session.getAttribute("userId")));
    }

    /** 共享广场 */
    @GetMapping("/plaza")
    public Result<List<SharedCoursewareVO>> plaza(@RequestParam(required = false) String subject,
                                                  @RequestParam(required = false) String keyword,
                                                  HttpSession session) {
        checkTeacher(session);
        return Result.success(shareService.listPlaza(
                (Long) session.getAttribute("userId"), subject, keyword));
    }

    /** 我的收藏 */
    @GetMapping("/favorites")
    public Result<List<SharedCoursewareVO>> favorites(@RequestParam(required = false) String subject,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpSession session) {
        checkTeacher(session);
        return Result.success(shareService.listFavorites(
                (Long) session.getAttribute("userId"), subject, keyword));
    }

    /** 预览共享课件：先校验有效性并累加浏览数，再返回完整课件 */
    @GetMapping("/{shareId}/courseware")
    public Result<CoursewareVO> courseware(@PathVariable Long shareId, HttpSession session) {
        checkTeacher(session);
        Long teacherId = (Long) session.getAttribute("userId");
        SharedCoursewareVO card = shareService.getPublished(shareId, teacherId);
        shareService.markViewed(shareId);
        return Result.success(aiCoursewareService.getCoursewareDetail(card.getAnimationId()));
    }

    @PostMapping("/{shareId}/favorite")
    public Result<Boolean> favorite(@PathVariable Long shareId, HttpSession session) {
        checkTeacher(session);
        return Result.success(shareService.setFavorite(
                (Long) session.getAttribute("userId"), shareId, true));
    }

    @DeleteMapping("/{shareId}/favorite")
    public Result<Boolean> unfavorite(@PathVariable Long shareId, HttpSession session) {
        checkTeacher(session);
        return Result.success(shareService.setFavorite(
                (Long) session.getAttribute("userId"), shareId, false));
    }

    /** 使用共享课件：复制一份到当前教师名下，避免改动原作者内容 */
    @PostMapping("/{shareId}/use")
    public Result<CoursewareVO> use(@PathVariable Long shareId, HttpSession session) {
        checkTeacher(session);
        Long teacherId = (Long) session.getAttribute("userId");
        Long newId = shareService.copyToMy(teacherId, shareId);
        return Result.success(aiCoursewareService.getCoursewareDetail(newId));
    }

    private void checkTeacher(HttpSession session) {
        if (!"teacher".equals(session.getAttribute("role"))) {
            throw new BizException("只有教师可以使用共享课件");
        }
    }
}
