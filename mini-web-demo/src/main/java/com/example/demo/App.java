package com.example.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 极简网页服务：JDK 自带 HttpServer，零第三方依赖。
 *
 * 启动后会：
 *   1. 在控制台打印服务信息（CI/CD 日志里能看到）；
 *   2. 监听端口，访问 / 显示下面 MESSAGE 的内容。
 */
public class App {

    // ==================== 改这里就能改网页内容 ====================
    static final String APP_NAME = "mini-web-demo";
    static final String VERSION  = "v1.0.0";
    static final String MESSAGE  = "Hello CI/CD! 这是 Java 代码里写好的内容。";
    // =============================================================

    /** 进程启动时间，网页上会显示，方便区分流水线发布的版本。 */
    static final String START_TIME =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public static void main(String[] args) throws IOException {
        int port = parsePort(System.getenv("PORT"));

        // ---------- 控制台打印（流水线日志里能看到这些内容） ----------
        System.out.println("==================================================");
        System.out.println("应用名称 : " + APP_NAME);
        System.out.println("版本号   : " + VERSION);
        System.out.println("输出内容 : " + MESSAGE);
        System.out.println("启动时间 : " + START_TIME);
        System.out.println("Java 版本: " + System.getProperty("java.version"));
        System.out.println("==================================================");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/",        App::handleIndex);
        server.createContext("/version", App::handleVersion);
        server.createContext("/health",  App::handleHealth);
        server.setExecutor(null); // 单线程处理请求即可
        server.start();

        System.out.println("网页地址 : http://localhost:" + port + "/");
        System.out.println("版本接口 : http://localhost:" + port + "/version");
        System.out.println("健康检查 : http://localhost:" + port + "/health");
        System.out.println("服务已启动，按 Ctrl+C 停止。");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            System.out.println("服务已停止。");
        }));
    }

    private static int parsePort(String envPort) {
        if (envPort == null || envPort.trim().isEmpty()) {
            return 8080;
        }
        try {
            return Integer.parseInt(envPort.trim());
        } catch (NumberFormatException e) {
            System.out.println("PORT 值不合法，回退到 8080。");
            return 8080;
        }
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        System.out.println("[请求] " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                + " -> 返回网页(" + VERSION + ")");
        send(exchange, 200, "text/html; charset=utf-8", page());
    }

    private static void handleVersion(HttpExchange exchange) throws IOException {
        send(exchange, 200, "text/plain; charset=utf-8", VERSION);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        send(exchange, 200, "text/plain; charset=utf-8", "OK");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    /** 网页 HTML，内容全部来自上面的常量。 */
    static String page() {
        return "<!DOCTYPE html>\n"
             + "<html lang=\"zh-CN\">\n"
             + "<head>\n"
             + "  <meta charset=\"utf-8\">\n"
             + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
             + "  <title>" + APP_NAME + " " + VERSION + "</title>\n"
             + "  <style>\n"
             + "    body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
             + "background:#0f172a;color:#e2e8f0;font-family:system-ui,-apple-system,'Segoe UI',sans-serif}\n"
             + "    .card{background:#1e293b;border:1px solid #334155;border-radius:14px;padding:40px 48px;"
             + "box-shadow:0 12px 40px rgba(0,0,0,.45);text-align:center;max-width:90vw}\n"
             + "    .msg{font-size:28px;font-weight:700;color:#38bdf8;margin:0 0 24px}\n"
             + "    table{margin:0 auto;border-collapse:collapse;font-size:14px}\n"
             + "    td{padding:6px 10px;text-align:left}\n"
             + "    td.k{color:#94a3b8}\n"
             + "    .tag{display:inline-block;margin-top:24px;padding:4px 12px;border-radius:999px;"
             + "background:#064e3b;color:#6ee7b7;font-size:12px}\n"
             + "  </style>\n"
             + "</head>\n"
             + "<body>\n"
             + "  <div class=\"card\">\n"
             + "    <p class=\"msg\">" + MESSAGE + "</p>\n"
             + "    <table>\n"
             + "      <tr><td class=\"k\">应用</td><td>" + APP_NAME + "</td></tr>\n"
             + "      <tr><td class=\"k\">版本</td><td>" + VERSION + "</td></tr>\n"
             + "      <tr><td class=\"k\">进程启动时间</td><td>" + START_TIME + "</td></tr>\n"
             + "      <tr><td class=\"k\">页面访问时间</td><td>" + now() + "</td></tr>\n"
             + "    </table>\n"
             + "    <div class=\"tag\">服务运行成功</div>\n"
             + "  </div>\n"
             + "</body>\n"
             + "</html>\n";
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
