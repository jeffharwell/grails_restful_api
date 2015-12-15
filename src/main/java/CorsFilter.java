/*
 * Apparently this is necessary to deal with Angular AJAX pre-flight requests which use
 * request method of OPTIONS which grails steadfastly refuses to do anything with. This
 * code filters those requests and adds the appropriate CORs headers.
 *
 * See https://github.com/davidtinker/grails-cors/blob/master/README.md
 */

import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import java.io.IOException;
//import org.springframework.core.annotation.*;
//import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import javax.annotation.*; // that one was hard to find, support @Priority() annotation

@Priority(Integer.MIN_VALUE)
public class CorsFilter extends OncePerRequestFilter {

    public CorsFilter() { }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String origin = req.getHeader("Origin");

        boolean options = "OPTIONS".equals(req.getMethod());
        if (options) {
            if (origin == null) return;
            resp.addHeader("Access-Control-Allow-Headers", "origin, authorization, accept, content-type, x-requested-with");
            resp.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
            resp.addHeader("Access-Control-Max-Age", "3600");
        }

        resp.addHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        resp.addHeader("Access-Control-Allow-Credentials", "true");

        if (!options) chain.doFilter(req, resp);
    }
}
