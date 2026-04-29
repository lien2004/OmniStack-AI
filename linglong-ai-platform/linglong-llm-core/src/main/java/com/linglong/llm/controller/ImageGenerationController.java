package com.linglong.llm.controller;

import com.linglong.llm.service.ImageGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 图片生成控制器
 * 支持文生图，基于智谱 GLM-Image API
 */
@Tag(name = "AI 图片生成", description = "文生图，调用智谱 GLM-Image API")
@RestController
@RequestMapping("/ai/image")
public class ImageGenerationController {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationController.class);

    @Resource
    private ImageGenerationService imageGenerationService;

    @Value("${zhipu.image.model:glm-image}")
    private String model;

    /**
     * 生成图片
     *
     * @param prompt 提示词/描述
     * @param image  参考图片（可选，GLM-Image 当前仅支持文生图，此参数暂不使用）
     * @return 生成的图片 base64 列表
     */
    @Operation(summary = "生成图片", description = "文生图，输入文字描述生成图片")
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(
            @Parameter(description = "图片描述/提示词", required = true)
            @RequestParam("prompt") String prompt,
            @Parameter(description = "参考图片（图生图时上传）")
            @RequestParam(value = "image", required = false) MultipartFile image) {

        log.info("收到图片生成请求, prompt={}, hasImage={}", prompt, image != null && !image.isEmpty());

        try {
            List<ImageGenerationService.GeneratedImage> images = imageGenerationService.generate(prompt, image);

            List<Map<String, String>> imageList = images.stream()
                    .map(img -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("format", img.getFormat());
                        map.put("base64", img.getBase64Data());
                        map.put("dataUrl", img.getDataUrl());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("success", true);
            result.put("message", "图片生成成功");
            result.put("data", imageList);
            result.put("count", imageList.size());

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("图片生成配置错误: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.ok(error);
        } catch (Exception e) {
            log.error("图片生成失败", e);
            String msg = e.getMessage();
            // 将 503/model not found 等上游错误翻译为用户友好的中文提示
            if (msg != null && (msg.contains("503") || msg.contains("model not found"))) {
                msg = "图片生成模型暂时不可用（503），智谱 GLM-Image 服务可能正在维护，请稍后重试";
            } else if (msg != null && msg.contains("429")) {
                msg = "图片生成请求过于频繁，请稍后重试";
            } else if (msg != null && msg.contains("401")) {
                msg = "API Key 无效或已过期，请联系管理员更新密钥";
            } else if (msg != null && msg.contains("403")) {
                msg = "无权访问该模型，请检查 API Key 权限";
            } else if (msg == null || msg.isBlank()) {
                msg = "图片生成失败，请稍后重试";
            } else {
                msg = "图片生成失败: " + msg;
            }
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("success", false);
            error.put("message", msg);
            return ResponseEntity.ok(error);
        }
    }

    /**
     * 健康检查 / 配置状态
     */
    @Operation(summary = "图片生成服务状态")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("success", true);
        result.put("message", "AI 图片生成服务就绪");
        result.put("model", model);
        return ResponseEntity.ok(result);
    }
}
