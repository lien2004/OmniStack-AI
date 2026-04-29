package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * AI 图片生成服务
 * 支持文生图，调用智谱 GLM-Image API (images/generations 接口)
 * GLM-Image 返回图片 URL，服务端下载后转为 base64 返回给前端
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private static final String GLM_IMAGE_API_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";

    @Value("${zhipu.image.api-key:}")
    private String apiKey;

    @Value("${zhipu.image.model:glm-image}")
    private String model;

    @Value("${zhipu.image.size:1024x1024}")
    private String defaultSize;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ImageGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 生成图片（文生图）
     *
     * @param prompt 用户提示词
     * @param image  参考图片（当前 GLM-Image 仅支持文生图，此参数暂不使用）
     * @return 生成的图片信息列表（base64 数据 + 格式）
     */
    public List<GeneratedImage> generate(String prompt, MultipartFile image) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("智谱 GLM-Image API Key 未配置，请在 application.yml 中设置 zhipu.image.api-key");
        }

        try {
            // 构建 GLM-Image 请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", defaultSize);

            String bodyStr = objectMapper.writeValueAsString(body);
            log.info("GLM-Image 请求: model={}, size={}, prompt={}...", model, defaultSize,
                    prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GLM_IMAGE_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("GLM-Image API 返回错误, status={}, body={}", resp.statusCode(), resp.body());
                throw new RuntimeException("图片生成 API 返回错误: " + resp.statusCode() + " - " + resp.body());
            }

            return extractImagesFromResponse(resp.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片生成调用异常", e);
            throw new RuntimeException("图片生成调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 从 GLM-Image API 响应中提取图片 URL 并下载转为 base64
     * 响应格式: { "data": [{ "url": "https://..." }] }
     */
    @SuppressWarnings("unchecked")
    private List<GeneratedImage> extractImagesFromResponse(String responseBody) {
        List<GeneratedImage> images = new ArrayList<>();
        try {
            Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) resp.get("data");
            if (dataList == null || dataList.isEmpty()) {
                log.warn("GLM-Image API 响应中未找到 data: {}", responseBody);
                return images;
            }

            for (Map<String, Object> item : dataList) {
                String url = (String) item.get("url");
                if (url == null || url.isBlank()) {
                    log.warn("GLM-Image 响应项中缺少 url: {}", item);
                    continue;
                }
                log.info("GLM-Image 返回图片 URL: {}", url);

                // 下载图片并转为 base64
                GeneratedImage img = downloadImageAsBase64(url);
                if (img != null) {
                    images.add(img);
                }
            }

            log.info("从 GLM-Image 响应中提取到 {} 张图片", images.size());
            return images;
        } catch (Exception e) {
            log.error("解析 GLM-Image 响应失败: {}", responseBody, e);
            throw new RuntimeException("解析图片生成响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载图片 URL 并转为 base64 格式
     */
    private GeneratedImage downloadImageAsBase64(String imageUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                log.error("下载图片失败, url={}, status={}", imageUrl, resp.statusCode());
                return null;
            }

            byte[] imageBytes = resp.body();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // 从 Content-Type 或 URL 推断格式
            String format = "png";
            String contentType = resp.headers().firstValue("Content-Type").orElse("");
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                format = "jpeg";
            } else if (contentType.contains("webp")) {
                format = "webp";
            } else if (imageUrl.contains(".jpg") || imageUrl.contains(".jpeg")) {
                format = "jpeg";
            } else if (imageUrl.contains(".webp")) {
                format = "webp";
            }

            log.info("图片下载完成, 大小={}KB, format={}", imageBytes.length / 1024, format);
            return new GeneratedImage(format, base64);
        } catch (Exception e) {
            log.error("下载图片异常, url={}", imageUrl, e);
            return null;
        }
    }

    /**
     * 生成的图片数据对象
     */
    public static class GeneratedImage {
        private final String format;
        private final String base64Data;

        public GeneratedImage(String format, String base64Data) {
            this.format = format;
            this.base64Data = base64Data;
        }

        public String getFormat() {
            return format;
        }

        public String getBase64Data() {
            return base64Data;
        }

        public String getDataUrl() {
            return "data:image/" + format + ";base64," + base64Data;
        }
    }
}
