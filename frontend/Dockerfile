# syntax=docker/dockerfile:1
#
# 贷后智能体 - 前端镜像
# Vite 构建静态资源 -> nginx 托管（同时反向代理 /api 到后端）

# ============================================================
# 阶段一：构建静态资源
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /build

# 国内拉 npm 包慢时可覆盖：
#   docker build --build-arg NPM_REGISTRY=https://registry.npmmirror.com .
ARG NPM_REGISTRY=https://registry.npmjs.org

# 先只拷依赖清单，利用缓存：package.json 与 lock 没变就不重复装依赖
COPY package.json package-lock.json ./
RUN npm config set registry "$NPM_REGISTRY" \
    && npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ============================================================
# 阶段二：nginx 托管
#
# 这里刻意不注入 VITE_API_TOKEN。
# Vite 的环境变量是「构建时」被替换进 bundle 的常量：一旦打进去，
#   1) 镜像就被绑定到某个环境的令牌，换环境必须重新构建；
#   2) 令牌会随静态资源下发到浏览器，在开发者工具里一览无余。
# 改由 nginx 在转发 /api/ 时代为附加 Authorization 头
# （见 nginx.conf.template），令牌只存在于容器环境变量里。
# ============================================================
FROM nginx:1.27-alpine

RUN rm -f /etc/nginx/conf.d/default.conf

# nginx 官方镜像的 entrypoint 会用 envsubst 渲染
# /etc/nginx/templates/*.template 到 /etc/nginx/conf.d/。
# 它只替换「环境中已定义」的变量名，所以配置里的 $host、$uri
# 这类 nginx 内置变量不会被误替换成空值。
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /build/dist /usr/share/nginx/html

# 这两个值由 docker-compose 注入；此处给默认值是为了让
# `docker run` 直接跑时模板也能渲染出合法配置（envsubst 只替换已定义的变量）
ENV BACKEND_UPSTREAM=backend:8080
ENV APP_TOKEN=""

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
