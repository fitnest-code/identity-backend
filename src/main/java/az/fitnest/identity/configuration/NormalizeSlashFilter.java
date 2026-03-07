package az.fitnest.identity.configuration;

import az.fitnest.identity.model.enums.UserStatus;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class NormalizeSlashFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String originalUri = request.getRequestURI();
        String normalizedPath = originalUri.replaceAll("/{2,}", "/");

        if (normalizedPath.equals(originalUri)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return normalizedPath;
            }

            @Override
            public String getServletPath() {
                String contextPath = request.getContextPath();
                if (normalizedPath.startsWith(contextPath)) {
                    return normalizedPath.substring(contextPath.length());
                }
                return normalizedPath;
            }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = new StringBuffer(request.getRequestURL().toString());
                int idx = url.indexOf(originalUri);
                if (idx != -1) {
                    url.replace(idx, idx + originalUri.length(), normalizedPath);
                }
                return url;
            }
        };

        chain.doFilter(wrappedRequest, response);
    }
}
