package com.example.tracking_order.common.aspect;

import com.example.tracking_order.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut that matches all repositories, services and Web REST endpoints.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) && within(com.example.tracking_order..*)")
    public void controllerPointcut() {
    }

    @Pointcut("within(@org.springframework.stereotype.Service *) && within(com.example.tracking_order..*)")
    public void servicePointcut() {
    }

    /**
     * Log method entry, arguments, return value and execution time for all controllers.
     */
    @Around("controllerPointcut()")
    public Object logControllerAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String userId = getCurrentUserId();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        Map<String, Object> params = new HashMap<>();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                String paramName = parameterNames[i].toLowerCase();
                if (paramName.contains("password") || paramName.contains("pwd") || 
                    paramName.contains("token") || paramName.contains("secret") || 
                    paramName.contains("authorization")) {
                    params.put(parameterNames[i], "****");
                } else {
                    params.put(parameterNames[i], args[i]);
                }
            }
        }

        if ("login".equals(joinPoint.getSignature().getName())) {
            String email = extractEmailFromParams(params);
            log.info("REQUEST method={} path={} userId={} email={}",
                    request.getMethod(), request.getRequestURI(), userId, email);
        } else {
            log.info("REQUEST method={} path={} userId={} params={}",
                    request.getMethod(), request.getRequestURI(), userId, params);
        }

        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("RESPONSE status=500 durationMs={} error={}", duration, e.getMessage());
            
            if ("login".equals(joinPoint.getSignature().getName())) {
                String email = extractEmailFromParams(params);
                log.warn("LOGIN_FAILED email={} reason={}", email, e.getMessage());
            }
            throw e;
        }

        long duration = System.currentTimeMillis() - start;
        
        String resultInfo = "result=success";
        String status = "200";
        if (result instanceof com.example.tracking_order.common.response.ApiResponse apiResponse) {
            Object data = apiResponse.getData();
            if (data instanceof Collection<?> collection) {
                resultInfo = "rows=" + collection.size();
            } else if (data instanceof com.example.tracking_order.common.response.PageData pageData) {
                resultInfo = "rows=" + (pageData.getItems() != null ? pageData.getItems().size() : 0);
            }
            if (!apiResponse.isSuccess()) {
                status = "400";
                resultInfo = "result=failed";
            }
        }

        if ("login".equals(joinPoint.getSignature().getName())) {
            log.info("RESPONSE status={} durationMs={}", status, duration);
            if ("200".equals(status)) {
                String email = extractEmailFromParams(params);
                String maskedEmail = maskEmail(email);
                
                String loggedInUserId = "Unknown";
                if (result instanceof com.example.tracking_order.common.response.ApiResponse apiResp) {
                    Object data = apiResp.getData();
                    if (data instanceof com.example.tracking_order.modules.auth.dto.LoginResponse loginResp) {
                        if (loginResp.getUser() != null) {
                            loggedInUserId = String.valueOf(loginResp.getUser().getId());
                        }
                    }
                }
                log.info("LOGIN_SUCCESS userId={} email={}", loggedInUserId, maskedEmail);
            }
        } else {
            log.info("RESPONSE status={} {} durationMs={}", status, resultInfo, duration);
        }

        return result;
    }

    private String extractEmailFromParams(Map<String, Object> params) {
        Object req = params.get("request");
        if (req instanceof com.example.tracking_order.modules.auth.dto.LoginRequest loginReq) {
            return loginReq.getEmail();
        }
        return "Unknown";
    }

    private String maskEmail(String email) {
        if (email == null || "Unknown".equals(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 3) {
            return name + "***@" + parts[1];
        }
        return name.substring(0, 3) + "***@" + parts[1];
    }

    /**
     * Log execution time for methods annotated with @LogExecutionTime or services.
     */
    @Around("@annotation(com.example.tracking_order.common.annotation.LogExecutionTime) || servicePointcut()")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object proceed = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - start;

        String methodName = joinPoint.getSignature().getName();
        if (executionTime > 300) {
            if ("login".equals(methodName)) {
                log.warn("AUTH_LOGIN_SLOW duration={}ms", executionTime);
            } else {
                log.warn("SLOW PERFORMANCE Method: {}() duration={}ms", methodName, executionTime);
            }
        } else {
            log.debug("PERFORMANCE Method: {}() duration={}ms", methodName, executionTime);
        }
        return proceed;
    }

    /**
     * Log exceptions in controller and service layers.
     */
    @AfterThrowing(pointcut = "controllerPointcut() || servicePointcut()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        log.error("EXCEPTION Method: {}.{}() | Message: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return String.valueOf(userDetails.getId());
        }
        return "Anonymous";
    }
}
