package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 学生课件学习状态：是否已确认学完、已停留秒数、解锁所需最少秒数。
 */
@Data
public class StudyStatusVO {

    private Boolean studied;
    private Integer studySeconds;
    private Integer minStudySeconds;
}
