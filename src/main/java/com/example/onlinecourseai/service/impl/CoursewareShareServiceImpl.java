package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.SharedCoursewareVO;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.CoursewareFavorite;
import com.example.onlinecourseai.entity.CoursewareShare;
import com.example.onlinecourseai.entity.Question;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.mapper.CoursewareFavoriteMapper;
import com.example.onlinecourseai.mapper.CoursewareShareMapper;
import com.example.onlinecourseai.mapper.QuestionMapper;
import com.example.onlinecourseai.service.CourseSubjectClassifier;
import com.example.onlinecourseai.service.CoursewareShareService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CoursewareShareServiceImpl implements CoursewareShareService {

    private static final Set<String> SUBJECTS = Set.of(
            "数学", "物理", "化学", "生物", "信息技术", "历史", "地理", "语文", "英语", "综合");

    private final CoursewareShareMapper shareMapper;
    private final CoursewareFavoriteMapper favoriteMapper;
    private final AnimationMapper animationMapper;
    private final QuestionMapper questionMapper;
    private final CourseSubjectClassifier classifier;

    public CoursewareShareServiceImpl(CoursewareShareMapper shareMapper,
                                      CoursewareFavoriteMapper favoriteMapper,
                                      AnimationMapper animationMapper,
                                      QuestionMapper questionMapper,
                                      CourseSubjectClassifier classifier) {
        this.shareMapper = shareMapper;
        this.favoriteMapper = favoriteMapper;
        this.animationMapper = animationMapper;
        this.questionMapper = questionMapper;
        this.classifier = classifier;
    }

    @Override
    public SharedCoursewareVO publish(Long teacherId, Long animationId, String subject, String note) {
        if (animationId == null) throw new BizException("课件 ID 不能为空");
        Animation animation = animationMapper.findById(animationId);
        if (animation == null) throw new BizException("课件不存在");
        if (!teacherId.equals(animation.getTeacherId())) {
            throw new BizException("只能共享自己生成的课件");
        }
        if (animation.getContentHtml() == null || animation.getContentHtml().isBlank()) {
            throw new BizException("课件内容为空，暂不能共享");
        }
        if (animation.getStatus() != null && !"SUCCESS".equals(animation.getStatus())) {
            throw new BizException("课件尚未生成完成，暂不能共享");
        }

        String finalSubject = normalizeSubject(subject, animation);
        String finalNote = clip(note, 500);

        CoursewareShare share = shareMapper.findByAnimationId(animationId);
        if (share == null) {
            share = new CoursewareShare();
            share.setAnimationId(animationId);
            share.setTeacherId(teacherId);
            share.setSubject(finalSubject);
            share.setNote(finalNote);
            shareMapper.insert(share);
        } else {
            share.setTeacherId(teacherId);
            share.setSubject(finalSubject);
            share.setNote(finalNote);
            shareMapper.republish(share);
        }

        SharedCoursewareVO vo = shareMapper.findCardById(share.getId(), teacherId);
        if (vo == null) throw new BizException("共享记录创建失败");
        vo.setMine(true);
        return vo;
    }

    @Override
    public void unpublish(Long teacherId, Long animationId) {
        Animation animation = animationMapper.findById(animationId);
        if (animation == null) throw new BizException("课件不存在");
        if (!teacherId.equals(animation.getTeacherId())) {
            throw new BizException("只能下架自己共享的课件");
        }
        CoursewareShare share = shareMapper.findByAnimationId(animationId);
        if (share == null) throw new BizException("该课件还没有共享记录");
        shareMapper.unpublish(animationId, teacherId);
    }

    @Override
    public List<SharedCoursewareVO> listMine(Long teacherId) {
        List<SharedCoursewareVO> list = shareMapper.findMine(teacherId);
        for (SharedCoursewareVO vo : list) {
            vo.setMine(true);
            vo.setFavorited(Boolean.TRUE.equals(vo.getFavorited()));
        }
        return list;
    }

    @Override
    public List<SharedCoursewareVO> listPlaza(Long teacherId, String subject, String keyword) {
        List<SharedCoursewareVO> list = shareMapper.findPlaza(
                teacherId, safe(subject), safe(keyword));
        return markMine(list, teacherId);
    }

    @Override
    public List<SharedCoursewareVO> listFavorites(Long teacherId, String subject, String keyword) {
        List<SharedCoursewareVO> list = shareMapper.findFavorites(
                teacherId, safe(subject), safe(keyword));
        return markMine(list, teacherId);
    }

    @Override
    public SharedCoursewareVO getPublished(Long shareId, Long teacherId) {
        SharedCoursewareVO vo = shareMapper.findCardById(shareId, teacherId);
        if (vo == null) throw new BizException("共享课件不存在");
        if (!Integer.valueOf(1).equals(vo.getStatus())) {
            throw new BizException("该共享课件已下架");
        }
        vo.setMine(teacherId.equals(vo.getOwnerId()));
        return vo;
    }

    @Override
    public void markViewed(Long shareId) {
        shareMapper.incrementView(shareId);
    }

    @Override
    public boolean setFavorite(Long teacherId, Long shareId, boolean favorite) {
        SharedCoursewareVO card = getPublished(shareId, teacherId);
        boolean exists = favoriteMapper.countByTeacherAndShare(teacherId, shareId) > 0;
        if (favorite) {
            if (!exists) {
                CoursewareFavorite record = new CoursewareFavorite();
                record.setTeacherId(teacherId);
                record.setShareId(shareId);
                int inserted = favoriteMapper.insert(record);
                if (inserted > 0) shareMapper.incrementFavorite(shareId);
            }
            return true;
        }
        if (exists) {
            favoriteMapper.delete(teacherId, shareId);
            shareMapper.decrementFavorite(shareId);
        }
        return false;
    }

    @Override
    @Transactional
    public Long copyToMy(Long teacherId, Long shareId) {
        SharedCoursewareVO card = getPublished(shareId, teacherId);
        Animation source = animationMapper.findById(card.getAnimationId());
        if (source == null) throw new BizException("原课件不存在");

        Animation copy = new Animation();
        copy.setTitle(source.getTitle());
        copy.setKnowledgePoint(source.getKnowledgePoint());
        copy.setSummary(source.getSummary());
        copy.setContentHtml(source.getContentHtml());
        copy.setStatus("SUCCESS");
        copy.setTeacherId(teacherId);
        copy.setCourseId(null);
        copy.setCreatedAt(LocalDateTime.now());
        animationMapper.insert(copy);
        if (copy.getId() == null) throw new BizException("课件复制失败");

        List<Question> questions = questionMapper.findByAnimationId(source.getId());
        for (Question q : questions) {
            Question q2 = new Question();
            q2.setAnimationId(copy.getId());
            q2.setQuestionText(q.getQuestionText());
            q2.setOptions(q.getOptions());
            q2.setAnswer(q.getAnswer());
            q2.setQuestionType(q.getQuestionType());
            q2.setScore(q.getScore());
            questionMapper.insert(q2);
        }
        return copy.getId();
    }

    /* ---------------- 内部工具 ---------------- */

    private List<SharedCoursewareVO> markMine(List<SharedCoursewareVO> list, Long teacherId) {
        for (SharedCoursewareVO vo : list) {
            vo.setMine(teacherId.equals(vo.getOwnerId()));
            vo.setFavorited(Boolean.TRUE.equals(vo.getFavorited()));
        }
        return list;
    }

    private String normalizeSubject(String subject, Animation animation) {
        String value = safe(subject);
        if (SUBJECTS.contains(value)) return value;
        return classifier.classify(
                animation.getTitle(), animation.getKnowledgePoint(), animation.getSummary()).getSubject();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String clip(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }
}
