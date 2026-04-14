package com.linglong.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return "{}";
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败", e);
            return null;
        }
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("JSON转对象失败", e);
            return null;
        }
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("JSON转列表失败", e);
            return null;
        }
    }

    public static JSONObject parseObject(String json) {
        if (json == null || json.isEmpty()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("JSON解析失败", e);
            return new JSONObject();
        }
    }

    public static JSONArray parseArray(String json) {
        if (json == null || json.isEmpty()) {
            return new JSONArray();
        }
        try {
            return JSON.parseArray(json);
        } catch (Exception e) {
            log.error("JSON数组解析失败", e);
            return new JSONArray();
        }
    }

    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = toJsonString(obj);
            return parseObject(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("对象转Map失败", e);
            return null;
        }
    }
}
