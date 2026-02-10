package az.fitnest.identity.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Normalizes incoming request URIs by collapsing multiple slashes ("//")
 * into a single slash. This helps when some clients or reverse proxies
 * accidentally introduce duplicate slashes, so that Spring MVC, Swagger,
 * and security mappings keep working as expected.
 */
public class NormalizeSlashFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String originalUri = request.getRequestURI();
        String normalizedPath = originalUri.replaceAll("/{2,}", "/");

        // If nothing to normalize, continue as-is
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

