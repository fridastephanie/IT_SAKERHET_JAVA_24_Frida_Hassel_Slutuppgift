package se.gritacademy.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final ConcurrentMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    private Bucket createApiBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofSeconds(60))))
                .build();
    }

    private Bucket createAuthBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofSeconds(60))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            setCorsHeaders(httpResponse);
            String ipAddress = httpRequest.getRemoteAddr();
            String requestUri = httpRequest.getRequestURI();
            if (requestUri.equals("/api/login") || requestUri.equals("/api/register")) {
                handleRateLimiting(ipAddress, "auth", httpRequest, httpResponse, chain);
            } else if (requestUri.startsWith("/api/messages")) {
                handleRateLimiting(ipAddress, "api", httpRequest, httpResponse, chain);
            } else {
                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            logger.error("Error during filtering: {}", e.getMessage());
            throw new ServletException("Error during filtering", e);
        }
    }

    /**
     * Sets CORS headers for the response
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept");
    }
    /**
     * Handles rate limiting for a given IP address
     */
    private void handleRateLimiting(String ipAddress, String bucketType, HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Bucket bucket = getRateLimiterBucket(ipAddress, bucketType);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        } else {
            handleRateLimitExceeded(bucketType, ipAddress, response);
        }
    }

    /**
     * Retrieves the appropriate rate limiter bucket based on type
     */
    private Bucket getRateLimiterBucket(String ipAddress, String bucketType) {
        if ("auth".equals(bucketType)) {
            return authBuckets.computeIfAbsent(ipAddress, k -> createAuthBucket());
        } else {
            return apiBuckets.computeIfAbsent(ipAddress, k -> createApiBucket());
        }
    }

    /**
     * Handles the case when rate limit is exceeded
     */
    private void handleRateLimitExceeded(String bucketType, String ipAddress, HttpServletResponse response) throws IOException {
        logger.warn("Rate limiter (\"{}\") blocked request from IP: {}", bucketType, ipAddress);
        response.setStatus(429);
        String message = "Too many requests, please try again later";
        if ("auth".equals(bucketType)) {
            message = "Too many login attempts, please try again later";
        } else if ("api".equals(bucketType)) {
            message = "Too many requests to the API, please try again later";
        }
        response.getWriter().write(message);
    }
}