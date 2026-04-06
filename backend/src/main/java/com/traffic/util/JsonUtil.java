package com.traffic.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JsonUtil {

    public static String readRequestBody(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String getStringValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx = json.indexOf(":", idx);
        if (idx < 0) return null;
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx < json.length() && json.charAt(idx) == '"') {
            idx++;
            int end = json.indexOf('"', idx);
            if (end < 0) return null;
            return json.substring(idx, end);
        }
        return null;
    }

    public static double getDoubleValue(String json, String key, double defaultValue) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        idx = json.indexOf(":", idx);
        if (idx < 0) return defaultValue;
        idx++;
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == '"')) idx++;
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try {
            return Double.parseDouble(json.substring(idx, end));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanValue(String json, String key, boolean defaultValue) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        idx = json.indexOf(":", idx);
        if (idx < 0) return defaultValue;
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (json.startsWith("true", idx)) return true;
        if (json.startsWith("false", idx)) return false;
        return defaultValue;
    }

    public static String errorJson(String message) {
        return "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
    }

    public static String statusJson(String status) {
        return "{\"status\":\"" + status + "\"}";
    }
}
