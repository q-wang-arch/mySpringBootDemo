package org.example.springbootdemo.auth;

/**
 * 请求级身份上下文。
 *
 * <p>认证拦截器在 preHandle 中写入身份，afterCompletion 中清理，业务代码可随时读取当前调用者。
 *
 * <p><b>注意</b>：这里用的是 ThreadLocal，只覆盖处理该请求的线程。贷后分析在
 * {@code agentExecutor} 线程池中异步执行，异步线程里 {@link #get()} 返回 null，
 * 若将来需要在异步步骤中记录操作人，应显式把用户名作为参数传入，而不是依赖本上下文。
 */
public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        HOLDER.set(principal);
    }

    /** 当前请求调用者，未认证时为 null */
    public static AuthPrincipal get() {
        return HOLDER.get();
    }

    /** 请求结束必须调用，避免线程池复用线程时身份串号 */
    public static void clear() {
        HOLDER.remove();
    }
}
