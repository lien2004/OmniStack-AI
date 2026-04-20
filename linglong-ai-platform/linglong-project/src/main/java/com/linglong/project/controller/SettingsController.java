package com.linglong.project.controller;

import com.linglong.common.result.Result;
import com.linglong.project.entity.DatabaseConfig;
import com.linglong.project.entity.LlmProvider;
import com.linglong.project.entity.PlatformSettings;
import com.linglong.project.repository.DatabaseConfigRepository;
import com.linglong.project.repository.LlmProviderRepository;
import com.linglong.project.repository.PlatformSettingsRepository;
import com.linglong.project.service.DatabaseConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 平台设置控制器
 */
@Tag(name = "平台设置", description = "平台基本设置、LLM 厂商配置、数据库配置、安全设置")
@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final PlatformSettingsRepository platformSettingsRepository;
    private final LlmProviderRepository llmProviderRepository;
    private final DatabaseConfigRepository databaseConfigRepository;
    private final DatabaseConnectionService databaseConnectionService;

    // ==================== 平台基本设置 ====================
    
    @Operation(summary = "获取平台基本设置")
    @GetMapping("/platform")
    public Result<Map<String, Object>> getPlatformSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        // 基本设置
        platformSettingsRepository.findBySettingKey("general")
            .ifPresent(s -> settings.put("general", parseJson(s.getSettingValue())));
        
        if (!settings.containsKey("general")) {
            Map<String, Object> defaultGeneral = new HashMap<>();
            defaultGeneral.put("platformName", "灵龙AI智能平台");
            defaultGeneral.put("notification", true);
            defaultGeneral.put("autoSave", true);
            settings.put("general", defaultGeneral);
        }
        
        return Result.success(settings);
    }

    @Operation(summary = "保存平台基本设置")
    @PostMapping("/platform")
    public Result<Void> savePlatformSettings(@RequestBody Map<String, Object> settings) {
        // 保存基本设置
        if (settings.containsKey("general")) {
            saveSetting("general", "json", toJson(settings.get("general")));
        }
        
        return Result.success();
    }

    // ==================== LLM厂商配置 ====================
    
    @Operation(summary = "获取 LLM 厂商列表", description = "返回所有已启用的 LLM 厂商配置")
    @GetMapping("/llm/providers")
    public Result<List<LlmProvider>> getLlmProviders() {
        return Result.success(llmProviderRepository.findByEnabledTrue());
    }

    @Operation(summary = "保存 LLM 厂商配置")
    @PostMapping("/llm/providers")
    public Result<LlmProvider> saveLlmProvider(@RequestBody LlmProvider provider) {
        if (provider.getIsDefault() != null && provider.getIsDefault()) {
            // 取消其他默认
            llmProviderRepository.findByIsDefaultTrue().ifPresent(p -> {
                p.setIsDefault(false);
                llmProviderRepository.save(p);
            });
        }
        LlmProvider saved = llmProviderRepository.save(provider);
        return Result.success(saved);
    }

    @Operation(summary = "删除 LLM 厂商配置")
    @DeleteMapping("/llm/providers/{id}")
    public Result<Void> deleteLlmProvider(@PathVariable Long id) {
        llmProviderRepository.deleteById(id);
        return Result.success();
    }

    // ==================== 数据库配置 ====================
    
    @Operation(summary = "获取数据库配置列表")
    @GetMapping("/database")
    public Result<List<DatabaseConfig>> getDatabaseConfigs() {
        return Result.success(databaseConfigRepository.findByEnabledTrue());
    }

    @Operation(summary = "保存数据库配置")
    @PostMapping("/database")
    public Result<DatabaseConfig> saveDatabaseConfig(@RequestBody DatabaseConfig config) {
        DatabaseConfig saved = databaseConfigRepository.save(config);
        return Result.success(saved);
    }

    @Operation(summary = "删除数据库配置")
    @DeleteMapping("/database/{id}")
    public Result<Void> deleteDatabaseConfig(@PathVariable Long id) {
        databaseConfigRepository.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "测试数据库连接")
    @PostMapping("/database/test")
    public Result<Map<String, Object>> testDatabaseConnection(@RequestBody DatabaseConfig config) {
        Map<String, Object> result = databaseConnectionService.testConnection(
            config.getDbType(),
            config.getHost(),
            config.getPort(),
            config.getDatabase(),
            config.getUsername(),
            config.getPassword()
        );
        return Result.success(result);
    }

    @Operation(summary = "获取数据库详细信息")
    @GetMapping("/database/{id}/info")
    public Result<Map<String, Object>> getDatabaseInfo(@PathVariable Long id) {
        Optional<DatabaseConfig> configOpt = databaseConfigRepository.findById(id);
        if (configOpt.isEmpty()) {
            return Result.error("数据库配置不存在");
        }
        
        DatabaseConfig config = configOpt.get();
        Map<String, Object> info = databaseConnectionService.getDatabaseInfo(
            config.getDbType(),
            config.getHost(),
            config.getPort(),
            config.getDatabase(),
            config.getUsername(),
            config.getPassword()
        );
        return Result.success(info);
    }

    // ==================== 安全设置 ====================
    
    @Operation(summary = "获取安全设置")
    @GetMapping("/security")
    public Result<Map<String, Object>> getSecuritySettings() {
        Map<String, Object> settings = new HashMap<>();
        
        platformSettingsRepository.findBySettingKey("security")
            .ifPresent(s -> settings.putAll(parseJson(s.getSettingValue())));
        
        // 默认值
        settings.putIfAbsent("jwtEnabled", false);
        settings.putIfAbsent("tokenExpireHours", 24);
        settings.putIfAbsent("operationLogEnabled", false);
        
        return Result.success(settings);
    }

    @Operation(summary = "保存安全设置")
    @PostMapping("/security")
    public Result<Void> saveSecuritySettings(@RequestBody Map<String, Object> settings) {
        saveSetting("security", "json", toJson(settings));
        return Result.success();
    }

    // ==================== 私有方法 ====================
    
    private void saveSetting(String key, String type, String value) {
        PlatformSettings setting = platformSettingsRepository.findBySettingKey(key)
            .orElse(new PlatformSettings());
        setting.setSettingKey(key);
        setting.setSettingType(type);
        setting.setSettingValue(value);
        platformSettingsRepository.save(setting);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            // 简单解析，实际项目中应使用ObjectMapper
            return new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        // 简单序列化，实际项目中应使用ObjectMapper
        return obj.toString();
    }
}
