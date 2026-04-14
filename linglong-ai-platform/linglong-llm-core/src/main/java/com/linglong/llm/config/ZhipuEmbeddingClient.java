package com.linglong.llm.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智谱AI Embedding 客户端
 * 直接调用智谱AI v4 embedding 接口，绕过 Spring AI 0.8.x 硬编码的 /v1/embeddings 路径
 * 目标 URL: https://open.bigmodel.cn/api/paas/v4/embeddings
 */
public class ZhipuEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(ZhipuEmbeddingClient.class);

    private static final String EMBEDDING_URL = "https://open.bigmodel.cn/api/paas/v4/embeddings";

    private final String apiKey;
    private final String model;
    private final int dimensions;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ZhipuEmbeddingClient(String apiKey, String model) {
        this(apiKey, model, 1024);
    }

    public ZhipuEmbeddingClient(String apiKey, String model, int dimensions) {
        this.apiKey = apiKey;
        this.model = model;
        this.dimensions = dimensions;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            List<String> texts = request.getInstructions();
            List<Embedding> embeddings = new ArrayList<>();

            for (int i = 0; i < texts.size(); i++) {
                List<Double> vector = embed(texts.get(i));
                embeddings.add(new Embedding(vector, i));
            }

            return new EmbeddingResponse(embeddings);
        } catch (Exception e) {
            throw new RuntimeException("智谱AI Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Double> embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "input", text,
                    "dimensions", dimensions
            ));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(EMBEDDING_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }

            EmbeddingApiResponse apiResponse = objectMapper.readValue(resp.body(), EmbeddingApiResponse.class);
            if (apiResponse.data == null || apiResponse.data.isEmpty()) {
                throw new RuntimeException("响应中无 embedding 数据: " + resp.body());
            }

            log.debug("智谱AI embedding 成功，维度: {}", apiResponse.data.get(0).embedding.size());
            return apiResponse.data.get(0).embedding;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("智谱AI Embedding 请求异常: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Double> embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        List<List<Double>> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    // -------- 工具方法 --------

    // -------- 内部 DTO --------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingApiResponse {
        public List<EmbeddingData> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingData {
        public List<Double> embedding;
        public int index;

        @JsonProperty("object")
        public String object;
    }
}
