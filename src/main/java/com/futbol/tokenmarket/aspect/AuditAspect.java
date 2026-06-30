package com.futbol.tokenmarket.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
public class AuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public AuditAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {}

    @Around("restControllerMethods()")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        String user = resolveCurrentUser();
        String operation = pjp.getSignature().getDeclaringType().getSimpleName()
                + "#" + pjp.getSignature().getName();
        String params = formatArgs(pjp.getArgs());

        try {
            Object result = pjp.proceed();
            auditLog.info("user={} op={} params={} duration={}ms status=OK",
                    user, operation, params, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            auditLog.info("user={} op={} params={} duration={}ms status=ERROR error={}",
                    user, operation, params, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    private String resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || shouldSkip(arg)) {
                continue;
            }
            try {
                String json = objectMapper.writeValueAsString(arg);
                parts.add(redactPasswords(json));
            } catch (Exception e) {
                parts.add(arg.getClass().getSimpleName());
            }
        }
        return "[" + String.join(", ", parts) + "]";
    }

    private boolean shouldSkip(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof Authentication
                || arg instanceof Principal;
    }

    private String redactPasswords(String json) {
        return json.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
    }
}
