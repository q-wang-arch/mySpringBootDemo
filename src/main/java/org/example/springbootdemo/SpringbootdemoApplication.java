package org.example.springbootdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringbootdemoApplication {

    public static void main(String[] args) {
        clearBlankActiveProfile();
        ConfigurableApplicationContext context = SpringApplication.run(SpringbootdemoApplication.class, args);
        if (context.isActive()) {
            System.out.println("====================================");
            System.out.println("  Spring Boot 启动成功！");
            System.out.println("  Hello接口: http://localhost:8080/api/hello");
            System.out.println("  用户列表: http://localhost:8080/api/user/list");
            System.out.println("  数据库: MySQL (springbootdemo)");
            System.out.println("====================================");
        }
    }

    /**
     * 清除「值为空」的 spring.profiles.active。
     *
     * <p>IDEA 运行配置的 Active profiles 输入框里只要残留一个空格（看起来是空的），
     * IDEA 就会生成 {@code -Dspring.profiles.active= }，Spring Boot 2.7 会直接把空 profile
     * 判为非法并中止启动：
     * <pre>
     * java.lang.IllegalArgumentException: Invalid profile []: must contain text
     * </pre>
     * 这里把这种「看似配了、实则为空」的值提前清掉，让应用按默认配置正常启动。
     */
    private static void clearBlankActiveProfile() {
        String key = "spring.profiles.active";
        String value = System.getProperty(key);
        if (value != null && value.trim().isEmpty()) {
            System.clearProperty(key);
            System.out.println("[boot] 检测到空的 -Dspring.profiles.active，已忽略；"
                    + "如需彻底消除，请清空 Run Configuration 中的 Active profiles 输入框");
        }
    }

}
