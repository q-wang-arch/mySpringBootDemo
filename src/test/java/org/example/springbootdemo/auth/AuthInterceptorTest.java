package org.example.springbootdemo.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springbootdemo.config.AuthProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 鉴权拦截器的行为验证。
 *
 * <p>刻意不使用 Spring 上下文：{@code AlertService} 在 {@code @PostConstruct} 中会查询
 * {@code alert_rule} 表并建立规则缓存，导致任何全量上下文测试都必须依赖真实数据库。
 * 这里直接构造拦截器 + MockHttpServletRequest/Response，把"认证 401 / 授权 403"的判定逻辑
 * 独立验证，不引入数据库依赖。
 */
class AuthInterceptorTest {

    private static final String ADMIN_TOKEN = "admin-token-for-test";
    private static final String APPROVER_TOKEN = "approver-token-for-test";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuthProperties props;
    private AuthInterceptor authInterceptor;
    private RoleInterceptor roleInterceptor;

    /** 模拟受保护接口：处置预警需要 RISK_ADMIN */
    @SuppressWarnings("unused")
    static class SampleController {

        @RequireRoles({Roles.RISK_ADMIN})
        public void handleAlert() {
        }

        /** 只认证、不限制角色的接口 */
        public void authenticatedOnly() {
        }
    }

    @BeforeEach
    void setUp() {
        props = new AuthProperties();
        props.setUsers(Arrays.asList(
                tokenUser("risk-admin", ADMIN_TOKEN, Roles.RISK_ADMIN),
                tokenUser("risk-approver", APPROVER_TOKEN, Roles.RISK_APPROVER),
                // 令牌为空白：应被跳过，且绝不能被当成"空令牌即可通过"
                tokenUser("api-client", "   ", Roles.API_CLIENT)
        ));
        props.init();
        authInterceptor = new AuthInterceptor(props);
        roleInterceptor = new RoleInterceptor(props);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ---------- 认证：401 ----------

    @Test
    @DisplayName("未携带令牌时返回 401，响应体结构与 ApiResponse 一致")
    void rejectsMissingToken() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/alert/A001/handle", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, handler("handleAlert"));

        assertFalse(pass, "无令牌必须被拦截");
        assertJsonError(response, 401);
    }

    @Test
    @DisplayName("令牌无效时返回 401")
    void rejectsUnknownToken() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/alert/A001/handle", "not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, handler("handleAlert"));

        assertFalse(pass);
        assertJsonError(response, 401);
    }

    @Test
    @DisplayName("未配置令牌的用户被跳过，空白令牌不能通过认证")
    void blankTokenNeverAuthenticates() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/agent/ingest", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, handler("authenticatedOnly"));

        assertFalse(pass, "空令牌必须被拒绝，不能退化成后门");
        assertJsonError(response, 401);
    }

    @Test
    @DisplayName("合法令牌通过认证，并把身份写入上下文")
    void authenticatesValidToken() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/agent/tasks", ADMIN_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, handler("authenticatedOnly"));

        assertTrue(pass);
        assertNotNull(AuthContext.get());
        assertEquals("risk-admin", AuthContext.get().getUsername());
        assertTrue(AuthContext.get().hasRole(Roles.RISK_ADMIN));
    }

    @Test
    @DisplayName("同时支持 X-Auth-Token 请求头（源头系统对接用）")
    void supportsApiTokenHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/agent/ingest");
        request.addHeader("X-Auth-Token", ADMIN_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(authInterceptor.preHandle(request, response, handler("authenticatedOnly")));
        assertEquals("risk-admin", AuthContext.get().getUsername());
    }

    @Test
    @DisplayName("Bearer 前缀大小写不敏感")
    void bearerPrefixIsCaseInsensitive() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/report/list");
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + ADMIN_TOKEN);

        assertTrue(authInterceptor.preHandle(request, new MockHttpServletResponse(), handler("authenticatedOnly")));
    }

    @Test
    @DisplayName("CORS 预检请求（OPTIONS）直接放行")
    void allowsPreflightRequest() throws Exception {
        MockHttpServletRequest request = request("OPTIONS", "/api/agent/ingest", null);

        assertTrue(authInterceptor.preHandle(request, new MockHttpServletResponse(), handler("authenticatedOnly")));
    }

    @Test
    @DisplayName("关闭总开关后不做任何校验（仅供本地开发）")
    void skipsWhenDisabled() throws Exception {
        props.setEnabled(false);
        props.init();
        // 重新构建拦截器以读取新的开关状态
        AuthInterceptor disabled = new AuthInterceptor(props);
        MockHttpServletRequest request = request("POST", "/api/alert/A001/handle", null);

        assertTrue(disabled.preHandle(request, new MockHttpServletResponse(), handler("handleAlert")));
        assertNull(AuthContext.get(), "关闭鉴权时不写入身份");
    }

    @Test
    @DisplayName("请求结束后清理身份上下文，避免线程复用导致身份串号")
    void clearsContextAfterCompletion() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/report/list", ADMIN_TOKEN);
        authInterceptor.preHandle(request, new MockHttpServletResponse(), handler("authenticatedOnly"));
        assertNotNull(AuthContext.get());

        authInterceptor.afterCompletion(request, new MockHttpServletResponse(), handler("authenticatedOnly"), null);

        assertNull(AuthContext.get());
    }

    // ---------- 授权：403 ----------

    @Test
    @DisplayName("身份有效但角色不足时返回 403，而不是 401")
    void forbidsWhenRoleMissing() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/alert/A001/handle", APPROVER_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 先认证，再授权——顺序与 WebMvcConfig 中的注册顺序一致
        assertTrue(authInterceptor.preHandle(request, response, handler("handleAlert")));
        boolean pass = roleInterceptor.preHandle(request, response, handler("handleAlert"));

        assertFalse(pass, "风险审批人不得处置预警");
        assertJsonError(response, 403);
    }

    @Test
    @DisplayName("角色满足时放行")
    void allowsWhenRoleMatched() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/alert/A001/handle", ADMIN_TOKEN);

        assertTrue(authInterceptor.preHandle(request, new MockHttpServletResponse(), handler("handleAlert")));
        assertTrue(roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("handleAlert")));
    }

    @Test
    @DisplayName("未声明 @RequireRoles 的接口只要求认证通过")
    void roleInterceptorSkipsAnnotatedFreeMethod() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/agent/tasks", APPROVER_TOKEN);

        assertTrue(authInterceptor.preHandle(request, new MockHttpServletResponse(), handler("authenticatedOnly")));
        assertTrue(roleInterceptor.preHandle(request, new MockHttpServletResponse(), handler("authenticatedOnly")));
    }

    // ---------- 辅助 ----------

    private static AuthProperties.TokenUser tokenUser(String username, String token, String... roles) {
        AuthProperties.TokenUser user = new AuthProperties.TokenUser();
        user.setUsername(username);
        user.setToken(token);
        user.setRoles(Arrays.asList(roles));
        return user;
    }

    private static MockHttpServletRequest request(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (token != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request;
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        Method method = SampleController.class.getMethod(methodName);
        return new HandlerMethod(new SampleController(), method);
    }

    private static void assertJsonError(MockHttpServletResponse response, int expectedCode) throws Exception {
        assertEquals(expectedCode, response.getStatus());
        JsonNode body = MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedCode, body.get("code").asInt());
        assertTrue(body.get("message").asText().length() > 0, "错误信息不能为空");
        if (expectedCode == 401) {
            assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        }
    }
}
