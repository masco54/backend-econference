package com.econference.shared.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates the SLF4J MDC (Mapped Diagnostic Context) for every HTTP request.
 * All values added here are automatically included in the JSON log output via logback-spring.xml.
 *
 * <p>MDC keys populated:
 * <ul>
 *   <li>{@code requestId}  – random UUID per request (or X-Request-ID header if present)</li>
 *   <li>{@code method}     – HTTP method (GET, POST, …)</li>
 *   <li>{@code uri}        – request URI</li>
 *   <li>{@code userAgent}  – User-Agent header</li>
 *   <li>{@code clientIp}   – remote IP address</li>
 *   <li>{@code statusCode} – HTTP response status (filled after the chain completes)</li>
 *   <li>{@code durationMs} – total request duration in milliseconds</li>
 * </ul>
 *
 * Additional keys ({@code userId}, {@code userEmail}) are filled later by
 * {@code JwtAuthenticationFilter} once the user is authenticated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);

    public static final String MDC_REQUEST_ID  = "requestId";
    public static final String MDC_METHOD       = "method";
    public static final String MDC_URI          = "uri";
    public static final String MDC_USER_AGENT   = "userAgent";
    public static final String MDC_CLIENT_IP    = "clientIp";
    public static final String MDC_STATUS_CODE  = "statusCode";
    public static final String MDC_DURATION_MS  = "durationMs";
    public static final String MDC_USER_ID      = "userId";
    public static final String MDC_USER_EMAIL   = "userEmail";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        try {
            // Honour X-Request-ID from gateway/load balancer if present
            String requestId = httpReq.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_METHOD,     httpReq.getMethod());
            MDC.put(MDC_URI,        httpReq.getRequestURI());

            String userAgent = httpReq.getHeader("User-Agent");
            if (userAgent != null) {
                MDC.put(MDC_USER_AGENT, userAgent);
            }
            MDC.put(MDC_CLIENT_IP, resolveClientIp(httpReq));

            // Propagate requestId back to the caller
            httpResp.setHeader("X-Request-ID", requestId);

            chain.doFilter(request, response);

        } finally {
            MDC.put(MDC_STATUS_CODE, String.valueOf(httpResp.getStatus()));
            MDC.put(MDC_DURATION_MS, String.valueOf(System.currentTimeMillis() - startTime));
            log.info("HTTP request completed");
            MDC.clear();
        }
    }

    /** Resolve client IP honouring common reverse-proxy headers. */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first is the real client
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        return (ip != null && !ip.isBlank()) ? ip.trim() : request.getRemoteAddr();
    }
}

