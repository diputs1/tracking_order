package com.example.tracking_order.common.filter;

import com.example.tracking_order.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IN_PROGRESS = "__IN_PROGRESS__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.idempotency.enabled:true}")
    private boolean enabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !uri.equals("/api/v1/cart/checkout") && !uri.equals("/api/v1/orders/quick-checkout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyHeader = request.getHeader("Idempotency-Key");
        if (idempotencyHeader == null || idempotencyHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request);
        String redisKey = buildRedisKey(wrappedRequest);
        String cachedBody = redisTemplate.opsForValue().get(redisKey);

        if (cachedBody != null && !IN_PROGRESS.equals(cachedBody)) {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(cachedBody);
            return;
        }

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, IN_PROGRESS, Duration.ofHours(ttlHours));
        if (Boolean.FALSE.equals(acquired)) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("IDEMPOTENCY_IN_PROGRESS", "Request is already being processed", null));
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        byte[] body = wrappedResponse.getContentAsByteArray();
        if (wrappedResponse.getStatus() < 500 && body.length > 0) {
            redisTemplate.opsForValue().set(redisKey, new String(body, StandardCharsets.UTF_8),
                    Duration.ofHours(ttlHours));
        } else {
            redisTemplate.delete(redisKey);
        }
        wrappedResponse.copyBodyToResponse();
    }

    private String buildRedisKey(CachedBodyRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "anonymous";
        return "idempotency:" + user + ":" + hash(request.getCachedBody());
    }

    private String hash(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static final class CachedBodyRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] cachedBody;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        private byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
