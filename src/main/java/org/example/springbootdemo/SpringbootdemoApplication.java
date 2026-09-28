package org.example.springbootdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringbootdemoApplication {

    public static void main(String[] args) {
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

}
