package org.example.springbootdemo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springbootdemo.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 鉴权失败响应的统一写出。
 *
 * <p>响应体结构与业务接口保持一致（复用 {@link ApiResponse}），
 * 这样前端 axios 拦截器可以按同一套规则解包，不会遇到"有的接口返回数组、有的返回对象"的分裂情况。
 */
final class AuthResponses {

    /** ObjectMapper 是线程安全的，静态复用即可 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuthResponses() {
    }

    static void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (code == HttpServletResponse.SC_UNAUTHORIZED) {
            // RFC 6750：401 响应应告知客户端支持的认证方案
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        MAPPER.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
