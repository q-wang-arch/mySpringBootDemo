package org.example.springbootdemo.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应封装
 */
public class ApiResponse {

    private int code;
    private String message;
    private Object data;
    private String timestamp;

    private ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(200, message, data);
    }

    public static ApiResponse success(String message) {
        return new ApiResponse(200, message, null);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public static ApiResponse badRequest(String message) {
        return new ApiResponse(400, message, null);
    }

    public static ApiResponse serverError(String message) {
        return new ApiResponse(500, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
