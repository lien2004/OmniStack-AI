package com.linglong.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglong.mcp.annotation.MCPParam;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 高德天气查询工具
 * 提供实时天气、天气预报查询功能
 * 文档：https://lbs.amap.com/api/webservice/guide/api/weatherinfo
 */
@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);
    private static final String AMAP_WEATHER_API = "https://restapi.amap.com/v3/weather/weatherInfo";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${amap.weather.key:3d0220c9baf05ed52827423171feb1d2}")
    private String amapKey;

    public WeatherTool(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * 查询实时天气
     *
     * @param city     城市名称或城市编码（如：北京 或 110000）
     * @param request  工具执行请求
     * @return 天气查询结果
     */
    @MCPTool(
            name = "query_weather_live",
            description = "查询指定城市的实时天气信息（温度、湿度、风向、风力等）",
            category = "weather"
    )
    @MCPParam(name = "city", description = "城市名称或城市编码，如：北京、上海、广州", type = "string", required = true)
    public ToolExecutionResult queryWeatherLive(String city, ToolExecutionRequest request) {
        if (city == null || city.isEmpty()) {
            return ToolExecutionResult.error("城市名称不能为空");
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(AMAP_WEATHER_API)
                    .queryParam("key", amapKey)
                    .queryParam("city", city)
                    .queryParam("extensions", "base")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            log.info("查询实时天气: city={}, uri={}", city, uri);

            String response = restTemplate.getForObject(uri, String.class);
            if (response == null) {
                return ToolExecutionResult.error("天气API返回空响应");
            }

            return parseWeatherResponse(response, city, "实时天气");

        } catch (Exception e) {
            log.error("查询实时天气失败: city={}", city, e);
            return ToolExecutionResult.error("查询天气失败: " + e.getMessage());
        }
    }

    /**
     * 查询天气预报
     *
     * @param city     城市名称或城市编码
     * @param request  工具执行请求
     * @return 天气预报结果
     */
    @MCPTool(
            name = "query_weather_forecast",
            description = "查询指定城市的未来天气预报（未来3天）",
            category = "weather"
    )
    @MCPParam(name = "city", description = "城市名称或城市编码，如：北京、上海、广州", type = "string", required = true)
    public ToolExecutionResult queryWeatherForecast(String city, ToolExecutionRequest request) {
        if (city == null || city.isEmpty()) {
            return ToolExecutionResult.error("城市名称不能为空");
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(AMAP_WEATHER_API)
                    .queryParam("key", amapKey)
                    .queryParam("city", city)
                    .queryParam("extensions", "all")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            log.info("查询天气预报: city={}, uri={}", city, uri);

            String response = restTemplate.getForObject(uri, String.class);
            if (response == null) {
                return ToolExecutionResult.error("天气API返回空响应");
            }

            return parseWeatherResponse(response, city, "天气预报");

        } catch (Exception e) {
            log.error("查询天气预报失败: city={}", city, e);
            return ToolExecutionResult.error("查询天气预报失败: " + e.getMessage());
        }
    }

    /**
     * 解析高德天气API响应
     */
    private ToolExecutionResult parseWeatherResponse(String response, String city, String queryType) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            String info = root.path("info").asText();
            String infocode = root.path("infocode").asText();

            if (!"1".equals(status)) {
                return ToolExecutionResult.error("天气API错误: " + info + " (code: " + infocode + ")");
            }

            JsonNode lives = root.path("lives");
            JsonNode forecasts = root.path("forecasts");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("city", city);
            result.put("queryType", queryType);
            result.put("reportTime", root.path("reporttime").asText(""));

            if (lives.isArray() && lives.size() > 0) {
                JsonNode live = lives.get(0);
                Map<String, Object> liveData = new LinkedHashMap<>();
                liveData.put("province", live.path("province").asText());
                liveData.put("city", live.path("city").asText());
                liveData.put("weather", live.path("weather").asText());
                liveData.put("temperature", live.path("temperature").asText() + "°C");
                liveData.put("windDirection", live.path("winddirection").asText());
                liveData.put("windPower", live.path("windpower").asText());
                liveData.put("humidity", live.path("humidity").asText() + "%");
                liveData.put("reportTime", live.path("reporttime").asText());
                result.put("liveWeather", liveData);
            }

            if (forecasts.isArray() && forecasts.size() > 0) {
                JsonNode forecast = forecasts.get(0);
                result.put("province", forecast.path("province").asText());
                result.put("cityName", forecast.path("city").asText());
                result.put("adcode", forecast.path("adcode").asText());
                result.put("reportTime", forecast.path("reporttime").asText());

                List<Map<String, Object>> castList = new ArrayList<>();
                JsonNode casts = forecast.path("casts");
                if (casts.isArray()) {
                    for (JsonNode cast : casts) {
                        Map<String, Object> dayCast = new LinkedHashMap<>();
                        dayCast.put("date", cast.path("date").asText());
                        dayCast.put("week", formatWeek(cast.path("week").asInt()));
                        dayCast.put("dayWeather", cast.path("dayweather").asText());
                        dayCast.put("nightWeather", cast.path("nightweather").asText());
                        dayCast.put("dayTemp", cast.path("daytemp").asText() + "°C");
                        dayCast.put("nightTemp", cast.path("nighttemp").asText() + "°C");
                        dayCast.put("dayWind", cast.path("daywind").asText());
                        dayCast.put("nightWind", cast.path("nightwind").asText());
                        dayCast.put("dayPower", cast.path("daypower").asText());
                        dayCast.put("nightPower", cast.path("nightpower").asText());
                        castList.add(dayCast);
                    }
                }
                result.put("forecast", castList);
            }

            return ToolExecutionResult.success(queryType + "查询成功", result);

        } catch (Exception e) {
            log.error("解析天气响应失败", e);
            return ToolExecutionResult.error("解析天气数据失败: " + e.getMessage());
        }
    }

    /**
     * 格式化星期
     */
    private String formatWeek(int week) {
        return switch (week) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            case 7 -> "星期日";
            default -> "未知";
        };
    }

    /**
     * 批量查询多个城市天气
     *
     * @param cities  城市列表，逗号分隔
     * @param request 工具执行请求
     * @return 批量天气结果
     */
    @MCPTool(
            name = "query_weather_batch",
            description = "批量查询多个城市的实时天气",
            category = "weather"
    )
    @MCPParam(name = "cities", description = "城市列表，逗号分隔，如：北京,上海,广州", type = "string", required = true)
    public ToolExecutionResult queryWeatherBatch(String cities, ToolExecutionRequest request) {
        if (cities == null || cities.isEmpty()) {
            return ToolExecutionResult.error("城市列表不能为空");
        }

        String[] cityArray = cities.split("[,，]");
        List<Map<String, Object>> results = new ArrayList<>();

        for (String city : cityArray) {
            city = city.trim();
            if (city.isEmpty()) continue;

            try {
                URI uri = UriComponentsBuilder.fromUriString(AMAP_WEATHER_API)
                        .queryParam("key", amapKey)
                        .queryParam("city", city)
                        .queryParam("extensions", "base")
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUri();

                String response = restTemplate.getForObject(uri, String.class);
                if (response != null) {
                    JsonNode root = objectMapper.readTree(response);
                    if ("1".equals(root.path("status").asText())) {
                        JsonNode lives = root.path("lives");
                        if (lives.isArray() && lives.size() > 0) {
                            JsonNode live = lives.get(0);
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("city", live.path("city").asText());
                            item.put("weather", live.path("weather").asText());
                            item.put("temperature", live.path("temperature").asText() + "°C");
                            item.put("windDirection", live.path("winddirection").asText());
                            item.put("humidity", live.path("humidity").asText() + "%");
                            results.add(item);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("查询城市天气失败: {}", city, e);
                Map<String, Object> errorItem = new LinkedHashMap<>();
                errorItem.put("city", city);
                errorItem.put("error", e.getMessage());
                results.add(errorItem);
            }
        }

        if (results.isEmpty()) {
            return ToolExecutionResult.error("所有城市查询均失败");
        }

        return ToolExecutionResult.success("批量天气查询完成", Map.of("results", results));
    }
}
