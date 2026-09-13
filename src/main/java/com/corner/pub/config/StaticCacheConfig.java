package com.corner.pub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security otherwise sends Cache-Control: no-store on images and video,
 * so the phone re-downloads several megabytes on every visit.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticCacheConfig extends OncePerRequestFilter {

    private static final String LONG_CACHE = "public, max-age=2592000, immutable";
    private static final String MENU_CACHE = "public, max-age=120";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isStaticAsset(path)) {
            chain.doFilter(request, new CacheEnforcingResponse(response, LONG_CACHE));
            return;
        }
        if (isPublicMenuApi(path) && "GET".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, new CacheEnforcingResponse(response, MENU_CACHE));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isStaticAsset(String path) {
        return path.startsWith("/images/")
                || path.startsWith("/uploads/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/fonts/")
                || path.startsWith("/img/");
    }

    private boolean isPublicMenuApi(String path) {
        return path.equals("/api/menu")
                || path.startsWith("/api/menu/")
                || path.equals("/api/in_evidenza")
                || path.startsWith("/api/in_evidenza/")
                || path.equals("/api/promotions/attive")
                || path.startsWith("/api/promotions/");
    }

    private static final class CacheEnforcingResponse extends HttpServletResponseWrapper {
        private final String cacheControl;

        private CacheEnforcingResponse(HttpServletResponse response, String cacheControl) {
            super(response);
            this.cacheControl = cacheControl;
            response.setHeader("Cache-Control", cacheControl);
            response.setHeader("Pragma", "cache");
        }

        @Override
        public void setHeader(String name, String value) {
            if (isCacheHeader(name)) {
                super.setHeader("Cache-Control", cacheControl);
                super.setHeader("Pragma", "cache");
                return;
            }
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            if (isCacheHeader(name)) {
                super.setHeader("Cache-Control", cacheControl);
                super.setHeader("Pragma", "cache");
                return;
            }
            super.addHeader(name, value);
        }

        private boolean isCacheHeader(String name) {
            return "Cache-Control".equalsIgnoreCase(name) || "Pragma".equalsIgnoreCase(name);
        }
    }
}
