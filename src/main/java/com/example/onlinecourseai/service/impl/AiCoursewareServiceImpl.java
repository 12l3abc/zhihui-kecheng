package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.CoursewareVO;
import com.example.onlinecourseai.dto.QuestionVO;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.AnswerRecord;
import com.example.onlinecourseai.entity.Grade;
import com.example.onlinecourseai.entity.Question;
import com.example.onlinecourseai.entity.Task;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.mapper.AnswerRecordMapper;
import com.example.onlinecourseai.mapper.CoursewareShareMapper;
import com.example.onlinecourseai.mapper.GradeMapper;
import com.example.onlinecourseai.mapper.QuestionMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.service.AiCoursewareService;
import com.example.onlinecourseai.service.CourseSubjectClassifier;
import com.example.onlinecourseai.service.LlmService;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 课件生成业务：组装提示词 → 调用大模型（有 Key 走豆包，无 Key 走 Mock）→
 * 解析固定 JSON → 存入 animation/question 表 → 返回课件
 */
@Service
public class AiCoursewareServiceImpl implements AiCoursewareService {

    /** 系统提示词：约束模型输出固定 JSON 格式的课件 */
    private static final String SYSTEM_PROMPT = """
            你是一名教学动画设计师兼前端工程师，为"智绘课成"平台制作可直接在浏览器播放的动画课件。
            用户只给你一个知识点（可能只有一句话）。你要先像设计师一样定视觉方案，再写代码。
            输出只有一个 JSON 对象，不要任何解释文字或 Markdown 代码块标记：
            {
              "title": "课件标题（不超过 16 字）",
              "summary": "重点知识提炼，150 字左右，分点",
              "contentHtml": "完整的单文件 HTML 页面（含 <!DOCTYPE html>、<style>、<script>）",
              "questions": [ { "questionText": "题干", "options": ["A. ...","B. ...","C. ...","D. ..."], "answer": "B", "questionType": "objective", "score": 10 } ]
            }

            【一、视觉基线（硬性）】
            1. 先判断学科，从下面三套里选一套，整份课件只能用这一套配色：
               A 理科（数学/物理/信息技术）：背景 #F6F4EE，卡片 #FFFFFF，主色 #0F5C4B，强调色 #C9A227，正文 #1A1A18，次要 #6B6B66，分隔线 #E4DFD3
               B 生化（化学/生物/医学）：背景 #F4F6F2，卡片 #FFFFFF，主色 #2E6B45，强调色 #D08A2C，正文 #17201A，次要 #63705F，分隔线 #DCE4DC
               C 文史（历史/语文/地理/政治）：背景 #F7F3EC，卡片 #FFFFFF，主色 #9C3B2E，强调色 #B08A4F，正文 #1F1714，次要 #6E635B，分隔线 #E7DECF
            2. 所有颜色必须写成 CSS 变量（:root{--bg;--card;--ink;--muted;--primary;--accent;--line}），全篇不得出现第二主色。
            3. 字体 -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif；标题 34~44px/700，正文 15~16px、行高 1.85，正文行宽不超过 72 字符。
            4. 圆角 16~20px；阴影只用一层极轻的 0 8px 24px rgba(0,0,0,.06)。
            5. 严禁：紫色/靛蓝系、彩虹渐变、玻璃拟态、霓虹描边、emoji 当图标、外链占位图片（页面必须能离线打开）。
            6. 图标一律用 inline SVG 线描（stroke-width 1.6），颜色取主色。
            7. 深色填充块上的数值/标签必须用白色或同明度的高对比色，禁止深字压深底；演示区高度按内容自适应，不留大片空白。

            【二、页面结构（按顺序，缺一不可）】
            1. 头部：不超过 14 字的主标题 + 一行副标题（说明学什么，不要口号式文案）。
            2. 过程演示区（整份课件的核心）：先想清楚这个知识点的"过程"长什么样，再选可视化主体，不要一律套卡片或套代码面板。
               - 计算机/算法：数据结构可视化（数组、链表、树、图、栈队列）。
               - 数学：坐标曲线描点、几何图形平移/旋转/折叠、公式项消去。
               - 物理/化学/生物：装置剖面、液面/活塞/电路状态、分子或细胞结构拼装、反应进程推进。
               - 英语/语文：字母或单词块逐位拼合、词义场景、句子成分生长、事件时间轴。
               - 史地/政：地图路径生长、年代轴推进、势力范围色块变化。
               必须能一步一步推进：6~12 步，每一步真实改变主体的状态（位移、交换、生长、形变、连线、高亮），并配一行旁白说清"这一步在做什么"。
               第 1 步必须让可视化主体已经出现在画面里（可以是未高亮的初始形态），绝不能只放标题和一片空白画布；后续步骤再逐个高亮、拼合、推进；重置后回到同样的可见初始态。
               单词类知识点：第 1 步就要把全部字母块按拼写顺序排好显示（描边/浅色态）并给出词义，从第 2 步起再逐个高亮；不得第 1 步只剩音标芯片或一个孤立按钮。
               过程要用元素的连续运动来表达（字母块逐位滑入拼合、曲线逐点描绘、液面上升、节点连线延伸），不要只靠换文字和换颜色。
               第 1 步就要把基准/关键对象用强调色标出来，不能一上来整屏都是灰的。
               控制：播放/暂停、上一步、下一步、重置；显示当前步序号（如 03 / 09）。
               要求是"过程动画"，不是淡入：位移和尺寸变化用 transition 400~600ms cubic-bezier(.22,.9,.3,1)；关键节点用 @keyframes 让元素真的动起来（滑入、合并、旋转、生长、连线延伸），不许只换文字或只换颜色。
               重置必须真的回到初始状态，不能只是清空文字。
               非计算机学科严禁出现代码面板、深色代码块、等宽伪代码行或"第 x 行…"式旁白；代码只属于真正在讲代码的知识点。
            3. 要点区：3~4 张卡片，每张含序号、一个 SVG 或数据小图、标题、一句结论；点击可展开 2~3 行细节。
            4. 对比 / 易错区：做成两栏对比或"正确做法 vs 常见错误"，不要再堆一段文字。
            5. 结尾：一句总结条 + 练习区。

            【三、动效基线】
            - 入场：标题 translateY(20px)→0 配 opacity，卡片 stagger 80~120ms，缓动 ease-out-expo 感；首屏动画总时长不超过 900ms。
            - 交互反馈：hover 抬升 3~4px 并加深阴影，按下 scale(.98)。
            - 连续性：每个演示区至少 2 处连续关键帧动画（元素滑入/合并/生长/旋转/连线延伸），用 @keyframes 或 requestAnimationFrame 实现；只做 opacity 淡入不算动画。
            - 步骤推进时，新加入的主体元素必须用 @keyframes 做滑入、放大或描绘（例如字母块 translateY(24px)+scale(.9)→原位的 450ms 动画），禁止整块内容瞬间替换成另一套内容。
            - 禁止把无限循环的 pulse/闪烁当主要动效；禁止所有元素同时淡入；禁止动画阻塞阅读。

            【四、版式硬约束】
            1. 主容器最大宽度 1080px，卡片内部内容必须用满 85% 以上宽度，禁止内容只占左半页、右半页大片空白。
            2. 过程演示区一律两栏：左侧视觉主体占 62%，右侧当前步骤标题、说明和步骤列表占 38%；宽度小于 768px 时改纵向。
            3. 要点卡片的图标必须与标题同一行、左对齐，不能漂到卡片右上角，也不能单独占一行。
            3.1 右侧步骤列表必须完整显示全部 6~12 步，不得被容器高度裁掉：行高控制在 32~38px、字号 13px；放不下时把列表移到演示区下方横向排列或减少每步字数，禁止用 overflow:hidden 把步骤截断成半行。
            4. 时间轴/流程类知识必须把年份或序号做成轴，节点配图形或色块；节点超过 6 个优先横向排布，纵向时年份列必须留足宽度，标签完整显示、不得被裁切或竖排。
            5. 化学装置、物理示意图必须用 rect/line/path 拼装并标注部件名（如分液漏斗、导管、集气瓶），不要画成简笔插画。
            6. 卡片高度由内容决定，内边距 20~24px，标题与正文间距 8px；不允许出现一句话配一大片空白的卡片。
            7. 练习选项行高 44~52px、选项间距 12px，不要用超高行距把选项拉满整屏。

            【五、工程约束】
            1. 单文件 HTML，内嵌 style 与 script，中文。
            2. 篇幅 300~480 行，信息密度要够，不要为省行数砍掉演示区。
            3. 不引入任何外部依赖：不写 CDN 链接、不引用网络图片，离线双击即可运行。
            4. 不用 !important，不写代码注释。
            5. 脚本在 DOMContentLoaded 后执行，并用 try/catch 包裹动画初始化，任何异常都不得白屏。
            6. 宽度小于 768px 时演示区改纵向、字号降一档。

            【六、练习区】
            1. 页面末尾同时包含客观题（"随堂小练习"）和主观题（"思考与探究"），主观题参考答案以"参考答案："开头。
            2. 答案一律不得明文展示：客观题点击选项判对错，答错后才提示正确项；主观题答案默认隐藏，提供"查看参考答案"按钮。
            3. 交互控制按钮（播放/上一步/下一步/重置）统一放在页面右下角：容器 position:fixed;right:24px;bottom:22px;display:flex;gap:10px;z-index:99，
               按钮为胶囊形 border-radius:999px;padding:10px 22px，不得散落在正文里；顺序从左到右固定为：重置、上一步、播放/暂停、下一步。

            【七、声音与朗读（全学科通用）】
            1. 禁止引入任何外链音频、音频文件或第三方 TTS 服务；页面里不要写 &lt;audio&gt; 标签，也不要自己写 WebAudio / new Audio()。声音由渲染层统一注入，重复写会出双声。
            2. 需要朗读的元素加 data-say="要读的文本"：英文单词块整体加 data-say="apple"，每个字母块加 data-say="a" 这样的单字母；语文生字、词语、文言句子同样加 data-say。
            3. 音标本身不要加 data-say（TTS 读不出音标），把 data-say 给音标对应的那个单词；需要强制英式读音时加 data-accent="uk"，否则跟随右下角的 US/UK 开关。
            4. 不要在版面上写"点击朗读""发音"之类的说明文字去占位置，声音是隐形辅助；按钮点击、步骤切换的音效也由渲染层统一生成，不要自己写。
            5. 单词类课件必须在分步演示里安排"读字母 → 读整词 → 读例句"的听音节点，每个节点都要有可见的字母/单词块承载 data-say。

            【八、计算机/信息技术学科专用版式（仅该学科启用）】
            （只有知识点本身属于算法、数据结构、编程语言、网络、数据库等计算机内容时才启用本段；英语、语文、数学、理化生、史地政一律不适用，整份页面不得出现任何代码面板、伪代码行、终端样式或"第几行代码"的旁白。判断不了学科时，优先按【二】里的学科形态做视觉演示。）
            1. 演示区必须做成"算法工作台"三栏：左 30% 代码面板，中 42% 数据结构可视化，右 28% 状态面板；高度 360~440px。
            2. 代码面板：宽度 300~340px；深色底 #12201A，等宽字体 Consolas/"JetBrains Mono"/monospace，字号 13px、行高 1.9，带行号，只显示 8~12 行核心伪代码；当前执行行加左侧 3px 强调色竖条与半透明底色，其余行文字用 #9FB3A8；代码单行不得换行、不得被裁切，每行不超过 32 个字符，伪代码要主动缩写（如 quickSort(a,l,r)）。
            3. 数据结构可视化：数组元素宽 56~64px、高 64~72px、间距 8px，整体宽度占满可视化区 90% 以上，不得缩成中间一小条，可视化区上下留白不超过 24px；**画出来的元素不超过 8 个**，知识点数组更长时只画当前搜索区间内的元素并在两端用"…"表示省略，状态面板的 left/right 必须与画面的元素数量一致，不允许出现画了 7 个却声明区间 [0,10] 的情况；元素上方标下标（从 0 开始），下方用三角箭头标指针（i / j / k）；基准元素用强调色实心+白字，已归位用主色实心+白字，未处理区只描边，三者对比必须一眼可辨。
            4. 状态面板：KV 列表展示 i、j、pivot、depth、比较次数、交换次数；本步发生变化的数值用强调色加粗。
            5. 每一步的旁白必须带上对应代码行，例如"第 5 行 · while (a[j] > pivot)：j 左移"。
            6. 步骤数硬上限 12 步：把同一轮里连续的指针移动合并为"扫描一步"，宁可合并也不能拆成二三十步。
            7. 树、图、链表类知识：节点用圆角矩形或圆，连线用 path，已访问/当前/未访问用三种明确状态区分，禁止只画一排方块。
            8. 数值统一用等宽数字（font-variant-numeric: tabular-nums），避免数字宽度跳动。
            9. 页头整体左对齐，标题不要居中；步骤序号显示在演示区标题行右侧，不要放到页面底部。
            10. 三栏必须用 CSS Grid 严格分栏：grid-template-columns:30% 42% 28%; 各栏 min-width:0、overflow:hidden；禁止用 absolute/fixed 让任何一栏越出自己的格子，数组绝不能压到代码面板下面。
            11. 代码每行必须完整显示，必要时降到 12px 或缩短伪代码（如 quickSort(a,l,r)），不得出现被裁切的半行字符；正文区与代码面板之间保留 16px 间距。
            12. 步骤徽标固定在演示区标题行右端（与"核心代码"同一行），不得压在代码面板上；指针标记必须落在对应元素正下方，不能偏移。
            13. 脚本必须语法自洽：练习判定等逻辑用命名函数 + 普通 for 循环实现，禁止三层以上嵌套的箭头函数回调；箭头函数使用 {} 函数体时花括号必须成对闭合；所有 getElementById 的 id 必须在 HTML 中存在，动画初始化不得因异常中断导致可视化空白。
            14. 数组元素的状态类必须互斥：pivot / sorted / range / out 同一元素只能取一个；基准元素不能同时挂 range 类，否则背景会被覆盖成白底白字。若需要同时表达"在当前区间内"和"是基准"，用独立的外描边表达，不要叠加背景类；.arr-elem.pivot 的规则必须声明在 .arr-elem.range 之后，保证强调色实心白字最终生效。
            15. 核心要点卡片必须"标题 + 1~2 行说明（不少于 18 字）"，不允许只放标题和一个序号；演示区内任何内容都不得被裁切，状态面板的当前步骤说明必须完整可见，高度不够时减少代码行数或压缩间距，不得用固定 height + overflow:hidden 截断文字。
            16. 可视化区内容靠上对齐（flex-start），数组顶部与栏目顶部间距不超过 20px；禁止用 justify-content:center 把内容吊在中间留下整片空白。元素少于 8 个时用尺寸上限（宽 64~72px、高 72px），让画面饱满；剩余空间放图例或当前指针说明，不要空着。
            17. 数组宽度必须按栏宽反算：元素总宽 = n×元素宽 + (n-1)×间距，必须 ≤ 可视化栏宽 - 32px，算不下就减小元素宽度或减少展示元素，严禁最右侧元素被栏边界裁掉。
            18. steps 数组长度是硬门槛：必须在 6~12 个之间，写完后先数 steps.length，超过 12 就把相邻指针移动合并成一步再重写，绝不提交四五十步的版本；步骤徽标要显示"01 / 实际步数"。
            19. 数组容器与元素必须逐字照抄下面这段 CSS，不许自行改写，只允许改颜色变量：
                .array-area{display:flex;flex-wrap:wrap;gap:8px;align-items:flex-end;justify-content:flex-start;max-width:100%;padding:8px 4px 0;overflow:hidden}
                .arr-elem{flex:0 0 auto;width:clamp(36px,calc((100% - 64px)/7),66px);height:66px;border-radius:10px;border:2px solid var(--primary);background:#fff;color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:20px;font-weight:700;position:relative;font-variant-numeric:tabular-nums}
                .arr-elem .idx{position:absolute;top:-20px;left:0;right:0;text-align:center;font-size:12px;color:var(--muted);font-weight:400}
                容器外面再包一层 .viz-body{max-width:100%;overflow:hidden;padding:0 12px}，用它把数组严格关在可视化栏内，任何元素都不得越过栏边界进入状态面板。
            20. 状态面板只允许放 KV 行（≤7 行），禁止在状态面板或代码面板内再塞"当前步骤说明"卡片、提示条；每一步的旁白一律放在算法工作台下方独立一行（左侧 3px 强调色竖条 + 一行文字），这样工作台的固定高度不会被内部卡片撑破、也不会被 overflow 截断。
            21. 工作台高度写死为 400px 且 overflow:hidden 时，代码 10 行 + 数组 7 元素 + KV 7 行是上限；只要任一项超出就必须减行，不允许靠裁切换取空间。

            【questions 要求】
            1. 至少 2 道客观题（options 用 "A. xxx" 格式，answer 填字母）；
            2. 至少 1 道主观题（options 为空数组，answer 写参考答案要点）；
            3. 命题贴合中考/高考/学业水平考试的高频问法；
            4. 题目必须与知识点紧密相关。
            """;

    /** 重点知识提炼提示词：约束模型输出简洁分点摘要 */
    private static final String SUMMARY_PROMPT = """
            你是一名资深教师。请根据下面的知识点内容，提炼出学生学习该知识点必须掌握的重点。
            要求：
            1. 用简洁的中文，150字左右；
            2. 分点列出 3~5 条重点，每条以序号开头；
            3. 直接输出文本内容，不要输出 JSON，不要使用 Markdown 代码块标记。
            """;

    private final RemoteLlmService remoteLlmService;
    private final MockLlmService mockLlmService;
    private final AnimationMapper animationMapper;
    private final QuestionMapper questionMapper;
    private final TaskMapper taskMapper;
    private final GradeMapper gradeMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final CoursewareShareMapper coursewareShareMapper;
    private final CourseSubjectClassifier subjectClassifier;
    private final ObjectMapper objectMapper;

    /** 宽松 JSON 解析器：允许字符串值内出现未转义的控制字符（真实模型常见输出） */
    private final ObjectMapper lenientMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    @Value("${llm.api-key:}")
    private String apiKey;

    public AiCoursewareServiceImpl(RemoteLlmService remoteLlmService,
                                   MockLlmService mockLlmService,
                                   AnimationMapper animationMapper,
                                   QuestionMapper questionMapper,
                                   TaskMapper taskMapper,
                                   GradeMapper gradeMapper,
                                   AnswerRecordMapper answerRecordMapper,
                                   CoursewareShareMapper coursewareShareMapper,
                                   CourseSubjectClassifier subjectClassifier,
                                   ObjectMapper objectMapper) {
        this.remoteLlmService = remoteLlmService;
        this.mockLlmService = mockLlmService;
        this.animationMapper = animationMapper;
        this.questionMapper = questionMapper;
        this.taskMapper = taskMapper;
        this.gradeMapper = gradeMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.coursewareShareMapper = coursewareShareMapper;
        this.subjectClassifier = subjectClassifier;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoursewareVO generateCourseware(Long teacherId, String knowledgePoint) {
        if (knowledgePoint == null || knowledgePoint.isBlank()) {
            throw new BizException("知识点不能为空");
        }

        // 1. 调用大模型（有 Key 走豆包，无 Key 走 Mock）
        LlmService llm = pickLlm();
        String content = llm.chat(SYSTEM_PROMPT, knowledgePoint);

        // 2. 解析并校验模型返回的固定 JSON
        JsonNode root = parseLlmResult(content);
        String title = root.path("title").asText("未命名课件");
        String summary = root.path("summary").asText("");
        String contentHtml = root.path("contentHtml").asText("");
        if (contentHtml.isBlank()) {
            throw new BizException("AI 未生成有效的课件内容，请重试");
        }
        // 2.1 渲染层兜底：模型偶发漏写容器约束或状态类冲突，这里用固定 CSS + 类名互斥修正
        // 注意：入库保存原文，只在返回给前端时注入，这样后续升级兜底规则能对存量课件立即生效
        String rawContentHtml = contentHtml;
        contentHtml = applyLayoutGuard(contentHtml);

        // 3. 存入课件表
        Animation animation = new Animation();
        animation.setTitle(title);
        animation.setKnowledgePoint(knowledgePoint);
        animation.setSummary(summary);
        animation.setContentHtml(rawContentHtml);
        animation.setStatus("SUCCESS");
        animation.setTeacherId(teacherId);
        animation.setCreatedAt(LocalDateTime.now());
        animationMapper.insert(animation);

        // 4. 存入题目表并组装返回
        List<QuestionVO> questionVOs = new ArrayList<>();
        JsonNode questions = root.path("questions");
        if (questions.isArray()) {
            for (JsonNode q : questions) {
                Question question = new Question();
                question.setAnimationId(animation.getId());
                question.setQuestionText(q.path("questionText").asText(""));
                question.setOptions(q.path("options").toString());
                question.setAnswer(q.path("answer").asText(""));
                question.setQuestionType(q.path("questionType").asText("objective"));
                question.setScore(q.path("score").asInt(10));
                questionMapper.insert(question);

                QuestionVO vo = new QuestionVO();
                vo.setId(question.getId());
                vo.setQuestionText(question.getQuestionText());
                vo.setOptions(question.getOptions());
                vo.setAnswer(question.getAnswer());
                vo.setQuestionType(question.getQuestionType());
                vo.setScore(question.getScore());
                questionVOs.add(vo);
            }
        }

        // 5. 返回课件
        CoursewareVO vo = new CoursewareVO();
        vo.setId(animation.getId());
        vo.setTitle(animation.getTitle());
        vo.setSummary(animation.getSummary());
        vo.setContentHtml(animation.getContentHtml());
        vo.setStatus(animation.getStatus());
        vo.setCreatedAt(animation.getCreatedAt());
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public List<CoursewareVO> getMyCoursewares(Long teacherId) {
        List<Animation> list = animationMapper.findByTeacherId(teacherId);
        List<CoursewareVO> result = new ArrayList<>();
        for (Animation a : list) {
            CoursewareVO vo = new CoursewareVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setCourseId(a.getCourseId());
            vo.setStatus(a.getStatus());
            vo.setCreatedAt(a.getCreatedAt());
            vo.setSubject(subjectClassifier.classify(a.getTitle(), a.getKnowledgePoint(), a.getSummary()).getSubject());
            // 列表页不携带完整 HTML 和题目，详情接口单独返回
            result.add(vo);
        }
        return result;
    }

    @Override
    public CoursewareVO getCoursewareDetail(Long coursewareId) {
        Animation a = animationMapper.findById(coursewareId);
        if (a == null) {
            throw new BizException("课件不存在");
        }
        CoursewareVO vo = new CoursewareVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setSummary(a.getSummary());
        vo.setCourseId(a.getCourseId());
        vo.setContentHtml(applyLayoutGuard(stripLayoutGuard(a.getContentHtml())));
        vo.setStatus(a.getStatus());
        vo.setCreatedAt(a.getCreatedAt());

        List<Question> questions = questionMapper.findByAnimationId(coursewareId);
        List<QuestionVO> questionVOs = new ArrayList<>();
        for (Question q : questions) {
            QuestionVO qv = new QuestionVO();
            qv.setId(q.getId());
            qv.setQuestionText(q.getQuestionText());
            qv.setOptions(q.getOptions());
            qv.setAnswer(q.getAnswer());
            qv.setQuestionType(q.getQuestionType());
            qv.setScore(q.getScore());
            questionVOs.add(qv);
        }
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public CoursewareVO getCoursewareDetailVisible(Long coursewareId, Long userId, String role) {
        Animation a = animationMapper.findById(coursewareId);
        if (a == null) {
            throw new BizException("课件不存在");
        }
        boolean allowed;
        if ("teacher".equals(role)) {
            // 教师仅可访问自己生成的课件；广场浏览走 /api/share 独立接口
            allowed = userId != null && userId.equals(a.getTeacherId());
        } else if ("student".equals(role)) {
            // 学生仅可访问本人所在班级任务所引用的课件
            allowed = userId != null
                    && taskMapper.countAccessibleByStudent(coursewareId, userId) > 0;
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new BizException("无权查看该课件");
        }
        return getCoursewareDetail(coursewareId);
    }

    @Override
    public String generateSummary(Long teacherId, Long coursewareId) {
        Animation animation = animationMapper.findById(coursewareId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if (!animation.getTeacherId().equals(teacherId)) {
            throw new BizException("只能提炼自己生成课件的重点");
        }
        if (animation.getKnowledgePoint() == null || animation.getKnowledgePoint().isBlank()) {
            throw new BizException("该课件缺少知识点描述，无法提炼");
        }

        // 调用大模型（有 Key 走豆包，无 Key 走 Mock）
        String summary = pickLlm().chat(SUMMARY_PROMPT, animation.getKnowledgePoint());
        String cleaned = summary == null ? "" : summary.trim();
        if (cleaned.isBlank()) {
            throw new BizException("AI 未生成有效摘要，请重试");
        }

        animationMapper.updateSummary(coursewareId, cleaned);
        return cleaned;
    }

    @Override
    public Animation updateCourseware(Long teacherId, Long coursewareId, String title, String summary, String contentHtml) {
        Animation animation = animationMapper.findById(coursewareId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if (!animation.getTeacherId().equals(teacherId)) {
            throw new BizException("只能修改自己生成的课件");
        }
        if (title == null || title.isBlank()) {
            throw new BizException("课件标题不能为空");
        }
        String newSummary = (summary == null || summary.isBlank()) ? animation.getSummary() : summary.trim();
        animationMapper.updateFields(coursewareId, title.trim(), newSummary);
        // 教师在编辑模式修改了课件正文文字时，整块回写 contentHtml；未传则不动正文
        if (contentHtml != null && !contentHtml.isBlank()) {
            animationMapper.updateContentHtml(coursewareId, contentHtml);
        }
        return animationMapper.findById(coursewareId);
    }

    @Override
    public void deleteCourseware(Long teacherId, Long coursewareId) {
        Animation animation = animationMapper.findById(coursewareId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if (!animation.getTeacherId().equals(teacherId)) {
            throw new BizException("只能删除自己生成的课件");
        }
        // 安全校验：已被教学任务引用的课件不允许删除（保证历史任务数据完整）
        if (taskMapper.countByAnimationId(coursewareId) > 0) {
            throw new BizException("该课件已被教学任务使用，无法删除");
        }
        // 共享中的课件必须先取消共享，避免共享广场出现失效记录
        if (coursewareShareMapper.findActiveByAnimationId(coursewareId) != null) {
            throw new BizException("该课件正在共享中，请先取消共享再删除");
        }
        // 先清理配套题目（外键约束），再删除课件
        questionMapper.deleteByAnimationId(coursewareId);
        animationMapper.deleteById(coursewareId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateQuizAnswer(Long teacherId, Long coursewareId, Long questionId,
                                    String questionText, String answer) {
        Animation animation = animationMapper.findById(coursewareId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if (!animation.getTeacherId().equals(teacherId)) {
            throw new BizException("只能修改自己生成的课件");
        }
        if (answer == null || answer.isBlank()) {
            throw new BizException("答案不能为空");
        }

        // 优先按题目 ID 定位，兼容旧前端按题干匹配
        Question question = null;
        if (questionId != null) {
            question = questionMapper.findById(questionId);
            if (question == null || !coursewareId.equals(question.getAnimationId())) {
                throw new BizException("题目不属于该课件，请刷新后重试");
            }
        } else {
            if (questionText == null || questionText.isBlank()) {
                throw new BizException("题目信息缺失，请刷新后重试");
            }
            question = questionMapper.findByText(coursewareId, questionText.trim());
        }
        if (question == null) {
            throw new BizException("未找到匹配的题目，请刷新后重试");
        }

        String finalAnswer;
        if ("subjective".equals(question.getQuestionType())) {
            finalAnswer = answer.trim().replaceFirst("^参考答案\\s*[:：]\\s*", "");
            if (finalAnswer.isBlank()) {
                throw new BizException("主观题参考答案不能为空");
            }
            if (finalAnswer.length() > 2000) {
                throw new BizException("主观题参考答案不能超过 2000 字");
            }
        } else {
            finalAnswer = normalizeObjectiveAnswer(answer, question.getOptions());
        }

        // question 表是教师端侧栏、学生端作答与判分的唯一数据源；
        // 不再改写 contentHtml 内的示例脚本，避免多题课件被批量替换。
        questionMapper.updateAnswer(question.getId(), finalAnswer);
        // 客观题答案变了，已提交任务必须按新答案重算，否则同一任务会出现新旧两套判分
        if ("objective".equals(question.getQuestionType())) {
            regradeSubmittedTasks(coursewareId);
        }
        return true;
    }

    /**
     * 客观题答案修改后，按新答案重算该课件所有已提交任务的逐题判分与总分。
     * 主观题得分（teacher_score）与批阅状态保持不变。
     */
    private void regradeSubmittedTasks(Long coursewareId) {
        List<Question> questions = questionMapper.findByAnimationId(coursewareId);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        for (Task task : taskMapper.findByAnimationId(coursewareId)) {
            for (Grade grade : gradeMapper.findByTaskId(task.getId())) {
                List<AnswerRecord> records =
                        answerRecordMapper.findByTaskAndStudent(task.getId(), grade.getStudentId());
                int objectiveScore = 0;
                for (AnswerRecord record : records) {
                    Question q = questionMap.get(record.getQuestionId());
                    if (q == null || !"objective".equals(q.getQuestionType())) {
                        continue;
                    }
                    String studentAnswer = record.getAnswerText() == null
                            ? "" : record.getAnswerText().trim();
                    boolean correct = q.getAnswer() != null
                            && q.getAnswer().equalsIgnoreCase(studentAnswer);
                    int score = correct ? (q.getScore() == null ? 0 : q.getScore()) : 0;
                    record.setIsCorrect(correct ? 1 : 0);
                    record.setScore(score);
                    answerRecordMapper.updateAnswer(record);
                    objectiveScore += score;
                }
                int teacherScore = grade.getTeacherScore() == null ? 0 : grade.getTeacherScore();
                grade.setTotalScore(objectiveScore + teacherScore);
                gradeMapper.update(grade);
            }
        }
    }

    /** 客观题答案统一存为选项字母（A/B/C/D），兼容前端传入完整选项文案 */
    private String normalizeObjectiveAnswer(String answer, String optionsJson) {
        List<String> options = new ArrayList<>();
        if (optionsJson != null && !optionsJson.isBlank()) {
            try {
                List<?> raw = objectMapper.readValue(optionsJson, List.class);
                for (Object item : raw) {
                    if (item != null) {
                        options.add(item.toString().trim());
                    }
                }
            } catch (Exception e) {
                throw new BizException("题目选项数据损坏，请重新生成课件");
            }
        }
        if (options.isEmpty()) {
            throw new BizException("该客观题没有选项，无法设置答案");
        }

        String trimmed = answer.trim();
        String upper = trimmed.toUpperCase();
        if (upper.length() == 1) {
            for (String option : options) {
                if (!option.isBlank() && String.valueOf(option.charAt(0)).equalsIgnoreCase(upper)) {
                    return upper;
                }
            }
            throw new BizException("答案必须是选项之一：" + String.join("、", options));
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(trimmed) || option.startsWith(trimmed)) {
                return String.valueOf(option.charAt(0)).toUpperCase();
            }
        }
        throw new BizException("答案必须是选项之一：" + String.join("、", options));
    }

    /** 有 Key 用豆包，无 Key 用 Mock */
    private LlmService pickLlm() {
        if (apiKey != null && !apiKey.isBlank()) {
            return remoteLlmService;
        }
        return mockLlmService;
    }

    // 渲染层兜底标记：存在即视为已注入，保证幂等
    private static final String GUARD_FLAG = "<!--cwl-guard-->";

    /**
     * 渲染层兜底。
     * 模型生成时偶发两类问题：数组容器不设宽度约束导致横向溢出压到相邻栏、状态类同时挂载导致背景被覆盖成白底白字。
     * 这里不依赖模型自觉，直接用固定 CSS + 类名互斥修正；对存量课件同样生效。
     */
    // 去掉旧的兜底注入，保证每次读取都用最新规则重新注入
    private String stripLayoutGuard(String html) {
        if (html == null || html.isBlank() || !html.contains(GUARD_FLAG)) {
            return html;
        }
        html = html.replaceAll(
                java.util.regex.Pattern.quote(GUARD_FLAG) + "[\\s\\S]*?</style>", "");
        html = html.replaceAll("<script id=\"cwl-guard-js\">[\\s\\S]*?</script>", "");
        // 兼容早期未带 id 的注入版本
        html = html.replaceAll("<script>\\(function\\(\\)\\{function fit\\(\\)[\\s\\S]*?</script>", "");
        return html;
    }

    private String applyLayoutGuard(String html) {
        if (html == null || html.isBlank() || html.contains(GUARD_FLAG)) {
            return html;
        }
        String css = GUARD_FLAG
                + "<style id=\"cwl-guard-style\">"
                + ".viz-body{max-width:100%!important;box-sizing:border-box!important;overflow:visible!important}"
                + ".array-area,.arr-wrap,.arr-body,.arr-row,.viz-array{max-width:100%!important;"
                + "box-sizing:border-box!important;overflow:visible!important}"
                + ".array-area{display:flex!important;gap:8px!important;align-items:flex-end!important;"
                + "justify-content:flex-start!important;padding-top:24px!important}"
                + ".array-area>*{flex:0 0 auto!important}"
                + ".arr-elem .idx{line-height:1!important;height:14px!important;top:-18px!important;"
                + "font-size:12px!important;z-index:2!important}"
                + ".arr-elem{width:clamp(34px,calc((100% - 64px)/7),66px)!important;box-sizing:border-box!important}"
                + ".arr-elem.range{background:#fff!important}"
                + ".arr-elem.pivot{background:var(--accent,#C9A227)!important;color:#fff!important;"
                + "border-color:var(--accent,#C9A227)!important}"
                + ".arr-elem.sorted{background:var(--primary,#0F5C4B)!important;color:#fff!important;"
                + "border-color:var(--primary,#0F5C4B)!important}"
                + "</style>";
        String js = "<script id=\"cwl-guard-js\">(function(){"
                + "function fit(){var ws=document.querySelectorAll('.arr-wrap,.array-area,.arr-body,.arr-row,.viz-array');"
                + "for(var i=0;i<ws.length;i++){var w=ws[i];w.style.maxWidth='100%';"
                + "if(w.scrollWidth>w.clientWidth+1){var k=w.clientWidth/w.scrollWidth;"
                + "w.style.transformOrigin='left top';w.style.transform='scale('+k+')';}else{w.style.transform='';}}}"
                + "function fix(){var e=document.querySelectorAll('.arr-elem');"
                + "for(var i=0;i<e.length;i++){var c=e[i].classList;"
                + "if(c.contains('pivot')){c.remove('range');c.remove('out');}"
                + "if(c.contains('sorted')){c.remove('range');c.remove('pivot');}}fit();}"
                + "function start(){fix();try{var t=null;new MutationObserver(function(){"
                + "if(t)return;t=requestAnimationFrame(function(){t=null;fix();});"
                + "}).observe(document.body,{subtree:true,childList:true});}catch(e){}}"
                + "if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',"
                + "function(){setTimeout(start,0);});}else{setTimeout(start,0);}})();</script>";
        if (html.contains("</head>")) {
            html = html.replaceFirst("</head>",
                    java.util.regex.Matcher.quoteReplacement(css + "</head>"));
        } else {
            html = css + html;
        }
        if (html.contains("</body>")) {
            html = html.replaceFirst("</body>",
                    java.util.regex.Matcher.quoteReplacement(js + "</body>"));
        } else {
            html = html + js;
        }
        return html;
    }

    /** 解析模型输出：去掉可能的 ```json 代码块标记后按 JSON 解析。
     *  真实模型输出的 JSON 字符串值里可能含裸换行（非严格 JSON），
     *  严格解析失败后用宽松模式（允许未转义控制字符）兜底。 */
    private JsonNode parseLlmResult(String content) {
        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                int lastFence = cleaned.lastIndexOf("```");
                if (firstNewline > 0 && lastFence > firstNewline) {
                    cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
                }
            }
            try {
                return objectMapper.readTree(cleaned);
            } catch (Exception e1) {
                // 模型在 contentHtml 等字段值里使用了真实换行，标准 JSON 解析会失败，宽松模式重试
                try {
                    return lenientMapper.readTree(cleaned);
                } catch (Exception e2) {
                    // HTML 里带未转义的英文双引号（如 onclick="..."）会让 JSON 提前收尾，先转义再解析
                    return lenientMapper.readTree(escapeEmbeddedQuotes(cleaned));
                }
            }
        } catch (Exception e) {
            String head = content == null ? "null" : content.substring(0, Math.min(160, content.length()));
            throw new BizException("AI 返回内容解析失败（长度 " + (content == null ? 0 : content.length())
                    + "）：" + e.getMessage() + "；开头：" + head);
        }
    }

    /** 只对 contentHtml 字段值内的裸双引号做转义，已转义的不动。 */
    private String escapeEmbeddedQuotes(String json) {
        int key = json.indexOf("\"contentHtml\"");
        if (key < 0) {
            return json;
        }
        int colon = json.indexOf(':', key);
        if (colon < 0) {
            return json;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return json;
        }
        int qKey = json.indexOf("\"questions\"", start);
        int end = qKey > 0 ? json.lastIndexOf('"', qKey - 1) : json.lastIndexOf('"');
        if (end <= start) {
            return json;
        }
        String region = json.substring(start + 1, end);
        StringBuilder sb = new StringBuilder(region.length() + 64);
        for (int i = 0; i < region.length(); i++) {
            char c = region.charAt(i);
            if (c == '"') {
                int slashes = 0;
                for (int j = i - 1; j >= 0 && region.charAt(j) == '\\'; j--) {
                    slashes++;
                }
                sb.append(slashes % 2 == 0 ? "\\\"" : "\"");
            } else {
                sb.append(c);
            }
        }
        return json.substring(0, start + 1) + sb + json.substring(end);
    }
}
