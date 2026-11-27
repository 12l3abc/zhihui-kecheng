package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.service.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Mock 兜底实现：不依赖任何 API Key 和网络。
 * 返回一段内置的"二分查找"示例课件（完整 HTML + CSS 动画 + 可点击交互 + 练习题），
 * 输出格式与真实模型完全一致（固定 JSON），保证答辩现场随时可演示。
 */
@Service
public class MockLlmService implements LlmService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // 重点知识提炼场景：摘要提示词含"分点列出"（课件生成提示词不含该词，避免误判）
        if (systemPrompt != null && systemPrompt.contains("分点列出")) {
            return MOCK_SUMMARY;
        }
        // 课件生成场景：返回内置课件 JSON
        return buildCoursewareJson();
    }

    /** 内置重点知识提炼结果（二分查找） */
    private static final String MOCK_SUMMARY = """
            1. 二分查找仅适用于有序数组，这是使用前必须确认的前提条件；
            2. 每次取中间元素与目标值比较，一次比较可排除一半区间，时间复杂度为 O(log n)；
            3. 用 low、high 两个指针标记当前查找区间，mid = (low + high) / 2；
            4. 相等则查找成功；小于目标值收缩左边界，大于目标值收缩右边界；
            5. 空间复杂度为 O(1)，仅需 low、high、mid 三个变量。
            """;

    private String buildCoursewareJson() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("title", "二分查找算法动画课件");
            root.put("summary",
                    "二分查找（折半查找）是一种在有序数组中查找目标元素的高效算法，每次比较都将查找区间缩小一半，"
                            + "时间复杂度为 O(log n)。算法维护左右两个指针标记当前查找区间，每次取中间元素与目标值比较："
                            + "相等则查找成功；小于目标值则收缩左边界；大于目标值则收缩右边界。"
                            + "本课件通过动画演示在数组 [3, 8, 15, 23, 37, 45, 52, 66, 89] 中查找 23 的完整过程，并配有配套练习。");
            root.put("contentHtml", buildContentHtml());

            ArrayNode questions = root.putArray("questions");

            ObjectNode q1 = questions.addObject();
            q1.put("questionText", "二分查找能够正确工作的前提条件是什么？");
            ArrayNode q1Opts = q1.putArray("options");
            q1Opts.add("A. 数组中的元素互不相同");
            q1Opts.add("B. 数组必须是有序的");
            q1Opts.add("C. 数组长度必须是 2 的幂");
            q1Opts.add("D. 数组中必须包含目标值");
            q1.put("answer", "B");
            q1.put("questionType", "objective");
            q1.put("score", 10);

            ObjectNode q2 = questions.addObject();
            q2.put("questionText", "在 100 万个有序元素中查找一个数，二分查找最多需要比较约多少次？");
            ArrayNode q2Opts = q2.putArray("options");
            q2Opts.add("A. 约 10000 次");
            q2Opts.add("B. 约 1000 次");
            q2Opts.add("C. 约 20 次");
            q2Opts.add("D. 约 100 次");
            q2.put("answer", "C");
            q2.put("questionType", "objective");
            q2.put("score", 10);

            // 主观题：结合各省市历年考试高频考点命题（数据结构与算法常见简答题）
            ObjectNode q3 = questions.addObject();
            q3.put("questionText", "结合所学知识，分析二分查找与顺序查找相比的主要优势，并说明二分查找的适用前提。");
            q3.putArray("options");
            q3.put("answer", "①二分查找每次比较可排除一半查找区间，时间复杂度为 O(log n)，远优于顺序查找的 O(n)；②适用前提是数据必须有序且支持随机访问（如数组）；③数据规模越大，二分查找的效率优势越明显。");
            q3.put("questionType", "subjective");
            q3.put("score", 20);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Mock 课件生成失败", e);
        }
    }

    private String buildContentHtml() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <title>二分查找动画课件</title>
                <style>
                  body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; margin: 16px; background: #f5f9ff; color: #1a1a2e; }
                  h1 { color: #2563eb; font-size: 24px; margin-bottom: 4px; }
                  .sub { color: #64748b; font-size: 13px; margin-bottom: 16px; }
                  .card { background: #ffffff; border-radius: 12px; padding: 16px 18px; margin: 12px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
                  .card h3 { color: #1e40af; margin: 0 0 8px 0; font-size: 16px; }
                  .card p { line-height: 1.8; font-size: 14px; margin: 6px 0; }
                  .tag { display: inline-block; background: #dbeafe; color: #1d4ed8; border-radius: 6px; padding: 2px 10px; font-size: 12px; margin-right: 6px; }
                  .arr { display: flex; gap: 6px; margin: 14px 0; flex-wrap: wrap; }
                  .cell { width: 52px; height: 52px; display: flex; align-items: center; justify-content: center;
                          border-radius: 8px; background: #e0e7ff; font-size: 18px; font-weight: 600;
                          transition: all 0.3s ease; color: #1e293b; }
                  .cell.low { background: #fde68a; border: 2px solid #f59e0b; }
                  .cell.high { background: #fecaca; border: 2px solid #ef4444; }
                  .cell.mid { background: #86efac; transform: scale(1.12);
                              animation: pulse 0.9s infinite; border: 2px solid #16a34a; }
                  .cell.found { background: #22c55e; color: #ffffff; transform: scale(1.12); }
                  @keyframes pulse {
                    0%, 100% { box-shadow: 0 0 0 0 rgba(34,197,94,0.45); }
                    50% { box-shadow: 0 0 0 10px rgba(34,197,94,0); }
                  }
                  .info { font-size: 14px; margin: 10px 0; color: #334155; }
                  .btn { padding: 10px 24px; border: none; border-radius: 999px; background: #2563eb; color: #fff;
                         cursor: pointer; font-size: 14px; font-weight: 500; margin-right: 0; }
                  .btn:hover { background: #1d4ed8; }
                  .btn.ghost { background: #e2e8f0; color: #334155; }
                  .btn.ghost:hover { background: #cbd5e1; }
                  .quiz label { display: block; padding: 8px 12px; margin: 6px 0; border: 1px solid #e2e8f0;
                                border-radius: 8px; cursor: pointer; font-size: 14px; transition: background 0.2s; }
                  .quiz label:hover { background: #eff6ff; }
                  .result { margin-top: 12px; font-size: 14px; font-weight: 600; }
                  .ok { color: #16a34a; }
                  .bad { color: #dc2626; }
                  ol { line-height: 1.9; font-size: 14px; }
                </style>
                </head>
                <body>
                <h1>二分查找算法动画课件</h1>
                <div class="sub">知识点：数据结构与算法 · 二分查找（折半查找）</div>

                <div class="card">
                  <h3>一、什么是二分查找</h3>
                  <p>二分查找（Binary Search），也叫折半查找，是一种在<strong>有序数组</strong>中查找目标元素的算法。
                     它每次把待查找区间一分为二，只保留可能包含目标值的那一半继续查找，因此效率远高于逐个比较的线性查找。</p>
                  <p>它之所以高效，核心思想是<strong>"一次比较，排除一半"</strong>：因为数组有序，只要比较中间元素与目标值的大小，
                     就能确定目标值只会出现在左半区或右半区，另一半可以彻底不再考虑。</p>
                  <p>生活中类似的思想很常见，比如猜数字游戏：朋友心里想一个 1~100 的数，你每次报一个数，对方说"大了"或"小了"，
                     你用折半策略最多 7 次就能猜中——这正是二分查找的思路。</p>
                </div>

                <div class="card">
                  <h3>二、算法步骤</h3>
                  <ol>
                    <li>初始化左指针 low 指向数组第一个元素（下标 0），右指针 high 指向最后一个元素（下标 n-1）。</li>
                    <li>计算中间位置 mid = (low + high) / 2（向下取整）。</li>
                    <li>比较 arr[mid] 与目标值 target：
                        若相等，查找成功，返回 mid；若 arr[mid] &lt; target，说明目标在右半区，令 low = mid + 1；
                        若 arr[mid] &gt; target，说明目标在左半区，令 high = mid - 1。</li>
                    <li>重复步骤 2、3，直到 low &gt; high，说明区间为空，目标不存在，查找失败。</li>
                  </ol>
                </div>

                <div class="card">
                  <h3>三、动画演示：在数组中查找 23</h3>
                  <p>数组：[3, 8, 15, 23, 37, 45, 52, 66, 89]，目标值：23。<span class="tag">黄色=左边界</span><span class="tag">红色=右边界</span><span class="tag">绿色=中间值</span></p>
                  <div id="arrBox" class="arr"></div>
                  <p class="info">当前查找区间：[<span id="lowShow">0</span>, <span id="highShow">8</span>]</p>
                  <div style="position:fixed;right:24px;bottom:22px;display:flex;gap:10px;z-index:99">
                    <button class="btn" onclick="nextStep()">下一步</button>
                    <button class="btn ghost" onclick="resetDemo()">重置</button>
                  </div>
                  <p id="stepInfo" class="info">点击"下一步"，观察二分查找如何逐步缩小查找区间。</p>
                </div>

                <div class="card">
                  <h3>四、复杂度分析</h3>
                  <p><strong>时间复杂度：</strong>每次比较将区间缩小一半，最多比较 log₂(n) 次，因此时间复杂度为 O(log n)，
                     即便在百万级数据中也只需约 20 次比较。</p>
                  <p><strong>空间复杂度：</strong>仅使用 low、high、mid 三个额外变量，空间复杂度为 O(1)。</p>
                  <p><strong>适用前提：</strong>数据必须有序，且适合随机访问（数组）。这是二分查找使用前必须确认的条件。</p>
                </div>

                <div class="card quiz">
                  <h3>课堂练习</h3>
                  <p>下列哪个是二分查找能够正确工作的前提条件？</p>
                  <label><input type="radio" name="q1" value="A"> A. 数组中的元素互不相同</label>
                  <label><input type="radio" name="q1" value="B"> B. 数组必须是有序的</label>
                  <label><input type="radio" name="q1" value="C"> C. 数组长度必须是 2 的幂</label>
                  <label><input type="radio" name="q1" value="D"> D. 数组中必须包含目标值</label>
                  <button class="btn" onclick="checkAnswer()">提交答案</button>
                  <p id="quizResult" class="result"></p>
                </div>

                <div class="card quiz">
                  <h3>思考与探究</h3>
                  <p>结合所学知识，分析二分查找与顺序查找相比的主要优势，并说明二分查找的适用前提。</p>
                  <p>参考答案：①二分查找每次比较可排除一半查找区间，时间复杂度为 O(log n)，远优于顺序查找的 O(n)；②适用前提是数据必须有序且支持随机访问（如数组）；③数据规模越大，二分查找的效率优势越明显。</p>
                </div>

                <script>
                  var arr = [3, 8, 15, 23, 37, 45, 52, 66, 89];
                  var target = 23;
                  var low = 0, high = arr.length - 1, mid = -1, count = 0, finished = false;

                  function render() {
                    var box = document.getElementById('arrBox');
                    box.innerHTML = '';
                    for (var i = 0; i < arr.length; i++) {
                      var d = document.createElement('div');
                      d.className = 'cell';
                      d.id = 'cell' + i;
                      d.textContent = arr[i];
                      box.appendChild(d);
                    }
                    update();
                  }

                  function update() {
                    for (var i = 0; i < arr.length; i++) {
                      var d = document.getElementById('cell' + i);
                      d.className = 'cell';
                      if (i === low) { d.classList.add('low'); }
                      if (i === high) { d.classList.add('high'); }
                      if (i === mid) { d.classList.add('mid'); }
                    }
                    document.getElementById('lowShow').textContent = low;
                    document.getElementById('highShow').textContent = high;
                  }

                  function nextStep() {
                    if (finished) { return; }
                    if (low > high) {
                      document.getElementById('stepInfo').textContent = '查找区间已为空，目标值不存在，查找结束。';
                      finished = true;
                      return;
                    }
                    count++;
                    mid = Math.floor((low + high) / 2);
                    update();
                    if (arr[mid] === target) {
                      document.getElementById('cell' + mid).classList.remove('mid');
                      document.getElementById('cell' + mid).classList.add('found');
                      document.getElementById('stepInfo').textContent =
                        '第 ' + count + ' 次比较：arr[' + mid + '] = ' + arr[mid] + '，等于目标值 23，查找成功！共比较 ' + count + ' 次。';
                      finished = true;
                      return;
                    }
                    if (arr[mid] < target) {
                      document.getElementById('stepInfo').textContent =
                        '第 ' + count + ' 次比较：arr[' + mid + '] = ' + arr[mid] + ' &lt; 23，目标在右半区，收缩左边界为 ' + (mid + 1);
                      low = mid + 1;
                    } else {
                      document.getElementById('stepInfo').textContent =
                        '第 ' + count + ' 次比较：arr[' + mid + '] = ' + arr[mid] + ' &gt; 23，目标在左半区，收缩右边界为 ' + (mid - 1);
                      high = mid - 1;
                    }
                    mid = -1;
                    update();
                  }

                  function resetDemo() {
                    low = 0; high = arr.length - 1; mid = -1; count = 0; finished = false;
                    document.getElementById('stepInfo').textContent = '点击"下一步"，观察二分查找如何逐步缩小查找区间。';
                    render();
                  }

                  function checkAnswer() {
                    var radios = document.getElementsByName('q1');
                    var chosen = '';
                    for (var i = 0; i < radios.length; i++) {
                      if (radios[i].checked) { chosen = radios[i].value; break; }
                    }
                    var r = document.getElementById('quizResult');
                    if (chosen === '') {
                      r.textContent = '请先选择一个选项再提交。';
                      r.className = 'result bad';
                    } else if (chosen === 'B') {
                      r.textContent = '回答正确！二分查找要求数组有序，才能根据中间值判断目标在左半区还是右半区。';
                      r.className = 'result ok';
                    } else {
                      r.textContent = '回答错误。二分查找的关键前提是数组有序，这样才能每次排除一半区间。';
                      r.className = 'result bad';
                    }
                  }

                  render();
                </script>
                </body>
                </html>
                """;
    }
}
