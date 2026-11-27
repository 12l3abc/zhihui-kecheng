package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 豆包大模型（火山方舟）真实调用实现。
 * 使用 JDK 自带 HttpClient 调用 OpenAI 兼容的 chat/completions 接口。
 */
@Service
public class RemoteLlmService implements LlmService {

    @Value("${llm.api-url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
    private String apiUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:doubao-seed-1-6-250615}")
    private String model;

    @Value("${llm.max-tokens:16000}")
    private int maxTokens;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public RemoteLlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            // 1. 组装请求体（OpenAI 兼容格式）
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.7);
            // 课件 HTML 篇幅较长（300~480 行），不设置上限会被默认值截断导致 JSON 解析失败
            body.put("max_tokens", maxTokens);
            // Doubao-Seed 2.x 默认深度思考较慢，关闭思考可大幅提速（1.x 模型无此参数，自动跳过）
            if (model != null && model.contains("seed-2")) {
                body.set("thinking", objectMapper.createObjectNode().put("type", "disabled"));
            }
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            // 2. 发送 HTTP 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(300))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // 3. 处理非 200 响应
            if (response.statusCode() != 200) {
                throw new BizException("豆包接口调用失败（HTTP " + response.statusCode() + "）：" + response.body());
            }

            // 4. 解析 choices[0].message.content
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new BizException("豆包返回内容为空");
            }
            return content.asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("调用豆包大模型失败：" + e.getMessage());
        }
    }
}
