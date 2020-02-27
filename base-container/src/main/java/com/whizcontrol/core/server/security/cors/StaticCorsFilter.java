package com.whizcontrol.core.server.security.cors;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * @author dtoptygin
 *
 */
@Component
public class StaticCorsFilter implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        //http://stackoverflow.com/questions/21696592/disable-spring-security-for-options-http-method
            
        setCorsHeaders(response);
        
        chain.doFilter(req, res);
    }
    
    public static void setCorsHeaders(HttpServletResponse response){
        //TODO: tighten the rules around Allow-Origin
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, PATCH, PUT, HEAD, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "10");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with,Content-Type,content-type,Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");        
    }

    public void init(FilterConfig filterConfig) {}

    public void destroy() {}

}
