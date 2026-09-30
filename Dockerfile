# syntax=docker/dockerfile:1
#
# 贷后智能体 - 后端镜像
#
# 多阶段构建：用 Maven 镜像编译，只把产出的 jar 放进 JRE 运行镜像。
# 这样运行镜像里没有 Maven、没有源码、没有本机 .m2 仓库。

# ============================================================
# 阶段一：构建，产出可执行 jar
#
# 必须用 JDK 8 构建：pom.xml 里 java.version=1.8，
# 用高版本 JDK 编译出的字节码在 JRE 8 上会直接
# UnsupportedClassVersionError，容器启动即崩。
#
# 注意不要写成 maven:3.8-openjdk-8 —— 该标签早已下架：
# openjdk 官方镜像停更后，Maven 官方镜像的 JDK 8 变体全部迁到了
# eclipse-temurin-8 系列，用旧标签会报 manifest unknown 直接构建失败。
# ============================================================
FROM maven:3.9-eclipse-temurin-8 AS builder

WORKDIR /build

# 先只拷 pom.xml 单独拉依赖。只要 pom 没变，这一层就命中 Docker 缓存，
# 之后改 Java 代码重新构建时不必再下载整个依赖树 —— 这是构建提速的关键。
# pom 里已配置阿里云仓库，国内拉依赖不会太慢。
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============================================================
# 阶段二：运行
# ============================================================
FROM eclipse-temurin:8-jre-alpine

# 时区。默认 UTC 会让容器日志与数据库时间差 8 小时，
# 排查"报告生成时间对不上"这类问题时极具误导性。
# tzdata 装完把时区文件拷出来即卸载，不留在镜像里。
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 非 root 运行：容器被攻破时把影响面压到最小
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=builder --chown=app:app /build/target/*.jar /app/app.jar

USER app
EXPOSE 8080

# 健康检查打 /api/hello —— 该路径在 app.auth.exclude-paths 里放行，
# 不带令牌也返回 200，适合探活（换用受保护接口会一律 401 导致永远 unhealthy）
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/api/hello || exit 1

# MaxRAMPercentage 让 JVM 按容器内存限额的比例取堆。
# JDK 8 在 8u191 之前读不到 cgroup 限额，会按宿主机总内存取堆，
# 容器里极易被 OOM Killer 干掉；eclipse-temurin 的 8 已包含该修复。
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

# exec 让 java 取代 sh 成为 PID 1，容器的 SIGTERM 才能送达 JVM（优雅停机）。
# 若省掉 exec，信号会停在 sh 上，docker stop 只能等超时后强杀。
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
