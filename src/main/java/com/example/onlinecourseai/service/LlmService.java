package com.example.onlinecourseai.service;

/**
 * 大模型对话服务接口。
 * 两个实现：
 *   - RemoteLlmService：真实调用豆包大模型 API（需要配置 llm.api-key）
 *   - MockLlmService：无 Key 时返回内置示例课件，保证本地可演示
 */
public interface LlmService {

    /**
     * 与大模型进行一次对话
     *
     * @param systemPrompt 系统提示词（约束输出格式）
     * @param userPrompt   用户输入（知识点内容）
     * @return 模型返回的文本内容
     */
    String chat(String systemPrompt, String userPrompt);
}
