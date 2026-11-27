package com.example.onlinecourseai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课件学科识别器：根据课件标题、知识点、摘要判定所属科目。
 *
 * 设计说明：
 * 1. 采用"关键词加权打分"而非固定映射，同一科目命中词越多分越高，避免单个歧义词误判；
 * 2. 标题权重最高（教师命题最直接），知识点次之，摘要再次，用于兜底；
 * 3. 无任何命中时归入"综合"，不强行编造科目；
 * 4. 判定过程保留命中词与得分，前端可展示"为什么判成这个科目"，避免黑盒。
 */
@Component
public class CourseSubjectClassifier {

    /** 单次识别结果：科目、得分、命中关键词 */
    public static class Result {
        private final String subject;
        private final int score;
        private final List<String> hits;

        public Result(String subject, int score, List<String> hits) {
            this.subject = subject;
            this.score = score;
            this.hits = hits;
        }

        public String getSubject() { return subject; }
        public int getScore() { return score; }
        public List<String> getHits() { return hits; }
    }

    /** 各科目的识别词表：词越具体越靠前，避免"函数""实验"这类跨科词单独定调 */
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("数学", List.of(
                "勾股", "定理", "方程", "几何", "三角", "概率", "导数", "向量", "数列",
                "不等式", "因式分解", "相似", "全等", "有理数", "无理数", "对数",
                "指数函数", "二次函数", "坐标系", "圆", "平行线", "中位数", "方差",
                "排列组合", "微积分", "矩阵", "集合", "极值", "面积", "周长", "体积"));

        RULES.put("物理", List.of(
                "牛顿", "加速度", "受力", "动量", "电路", "电流", "电压", "电阻", "电功率",
                "磁场", "电磁感应", "折射", "反射", "透镜", "浮力", "压强", "密度",
                "机械能", "做功", "重力", "摩擦", "匀速", "自由落体", "内能", "比热容",
                "振动", "声波", "万有引力", "杠杆", "滑轮", "功率"));

        RULES.put("化学", List.of(
                "化学", "氧气", "制取", "反应", "元素", "分子", "原子", "离子", "溶液",
                "酸碱", "氧化", "还原", "燃烧", "电解", "化合物", "摩尔", "沉淀",
                "金属", "非金属", "催化剂", "化合价", "化学式", "方程式", "高锰酸钾",
                "过氧化氢", "二氧化碳", "氢气", "碳酸", "盐"));

        RULES.put("生物", List.of(
                "光合", "细胞", "遗传", "DNA", "基因", "生态", "呼吸作用", "酶",
                "染色体", "蛋白质", "种群", "群落", "免疫", "神经", "消化", "血液循环",
                "植物", "动物", "细胞分裂", "染色体", "叶绿体", "蒸腾", "食物链",
                "生物", "组织", "器官", "有丝分裂", "减数分裂", "变异"));

        RULES.put("信息技术", List.of(
                "算法", "排序", "查找", "二分", "快速排序", "冒泡", "插入排序",
                "数组", "链表", "数据结构", "递归", "栈", "队列", "二叉树", "哈希",
                "编程", "计算机", "数据库", "循环", "变量", "时间复杂度", "空间复杂度",
                "指针", "代码", "程序", "函数调用", "遍历", "贪心", "动态规划", "图论"));

        RULES.put("历史", List.of(
                "共产党", "历史", "朝代", "革命", "战争", "辛亥", "变法", "改革",
                "古代", "近代", "起义", "王朝", "抗战", "建国", "运动", "条约",
                "统一", "封建社会", "新文化", "五四", "长征", "解放", "殖民", "文艺复兴"));

        RULES.put("地理", List.of(
                "气候", "地形", "板块", "洋流", "人口", "城市", "经纬", "大气环流",
                "季风", "河流", "山脉", "水土流失", "时区", "降水", "地貌", "等高线"));

        RULES.put("语文", List.of(
                "文言文", "诗词", "修辞", "散文", "小说", "作文", "阅读", "古诗",
                "成语", "病句", "标点", "文学", "议论文", "记叙文", "说明文", "诗歌"));

        RULES.put("英语", List.of(
                "grammar", "vocabulary", "tense", "reading", "writing", "english",
                "时态", "单词", "语法", "从句", "音标", "听力", "阅读理解", "完形填空"));
    }

    /** 科目 → 课程号前缀 */
    private static final Map<String, String> CODE_PREFIX = Map.of(
            "数学", "MATH", "物理", "PHYS", "化学", "CHEM", "生物", "BIOL",
            "信息技术", "IT", "历史", "HIST", "地理", "GEOG",
            "语文", "CHIN", "英语", "ENGL", "综合", "GEN");

    /** 权重：标题命中 5 分，知识点 3 分，摘要 1 分 */
    private static final int W_TITLE = 5;
    private static final int W_POINT = 3;
    private static final int W_SUMMARY = 1;

    public Result classify(String title, String knowledgePoint, String summary) {
        String t = safe(title).toLowerCase();
        String k = safe(knowledgePoint).toLowerCase();
        String s = safe(summary).toLowerCase();

        String bestSubject = "综合";
        int bestScore = 0;
        List<String> bestHits = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : RULES.entrySet()) {
            int score = 0;
            List<String> hits = new ArrayList<>();
            for (String kw : entry.getValue()) {
                String word = kw.toLowerCase();
                if (t.contains(word)) { score += W_TITLE; hits.add(kw); continue; }
                if (k.contains(word)) { score += W_POINT; hits.add(kw); continue; }
                if (s.contains(word)) { score += W_SUMMARY; hits.add(kw); }
            }
            if (score > bestScore) {
                bestScore = score;
                bestSubject = entry.getKey();
                bestHits = hits;
            }
        }
        return new Result(bestSubject, bestScore, bestHits);
    }

    public String codePrefix(String subject) {
        return CODE_PREFIX.getOrDefault(subject, "GEN");
    }

    private String safe(String v) { return v == null ? "" : v; }
}
