package com.seinfo.protoA.util.mybatis;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class CamelMap extends HashMap<String, Object> {

    @Override
    public Object put(String key, Object value) {
        if (key == null) return null;

        String finalKey = isSnakeCase(key) ? toCamelCase(key) : key;
        return super.put(finalKey, value);
    }

    private boolean isSnakeCase(String key) {
        return key != null && key.contains("_");
    }

    private String toCamelCase(String input) {
        if (input == null || !input.contains("_")) return input;

        StringBuilder result = new StringBuilder();
        String[] parts = input.toLowerCase().split("_");
        result.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return result.toString();
    }
}
