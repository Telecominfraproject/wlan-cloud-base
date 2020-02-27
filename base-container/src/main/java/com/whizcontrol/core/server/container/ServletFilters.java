package com.whizcontrol.core.server.container;

import java.io.IOException;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.whizcontrol.core.server.security.WebSecurityConfig;

/**
 * @author dtop
 *
 */
@Component
public class ServletFilters {

    /**
     * see http://stackoverflow.com/questions/2255814/can-i-turn-off-the-httpsession-in-web-xml 
     */
    @Bean    
    public Filter disableHttpSessionFilter(){
        Filter disableSessionFilter = new Filter() {
            
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }
            
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                    ServletException {
                HttpServletRequestWrapper noSessionRequestWrapper = new NoSessionRequestWrapper((HttpServletRequest)request);
                chain.doFilter(noSessionRequestWrapper, response);
            }
            
            @Override
            public void destroy() {
            }
        };
        
        return disableSessionFilter;
    } 
       
    /**
     * see http://stackoverflow.com/questions/2876250/tomcat-cache-control
     */
    @Bean
    @Profile(value = {"auth0_and_digest_auth", "customer_digest_auth"}) //use only in portals
    public Filter cacheControlFilter(final Environment env){
         return new Filter() {
    
            private Set<String> pathsToProtect;
 
            public void doFilter(ServletRequest request, ServletResponse response,
                                 FilterChain chain) throws IOException, ServletException {
    
                if ("GET".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
                    //see if request is protected (and non-static), and only then set no-cache headers
                    if( pathsToProtect!=null && !pathsToProtect.isEmpty()){
                        
                        boolean addHeaders = false;
                        String reqPath = ((HttpServletRequest)request).getServletPath();
                        
                        for(String p: pathsToProtect){
                            if(reqPath.startsWith(p)){
                                addHeaders = true;
                                break;
                            }
                        }
                    
                        if(addHeaders){
                            HttpServletResponse resp = (HttpServletResponse) response;
                            resp.setHeader("Expires", "Tue, 03 Jul 2001 06:00:00 GMT");
                            resp.setDateHeader("Last-Modified", System.currentTimeMillis());
                            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
                            resp.setHeader("Pragma", "no-cache");
                        }
                    }
                }
    
                chain.doFilter(request, response);
            }
    
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                pathsToProtect = WebSecurityConfig.getProtectedPaths(env);
            }
    
            @Override
            public void destroy() {
                // do nothing
            }
    
        };
    }
    
    @Bean
    @Profile(value= "no_content_reponse") // used only in portals
    public Filter noConentReponseFilter(final Environment env) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                filterChain.doFilter(request, response);
                if (HttpStatus.OK.value() != response.getStatus()) {
                    return;
                }
                if (response.getContentType() == null|| response.getContentType().isEmpty()) {
                    response.setStatus(HttpStatus.NO_CONTENT.value());
                }
            }
        };
    }
    
    /**
     * NAAS-9922, work around for push state routing. return index.html for all 
     * @param env
     * @return
     */
    @Bean
    @Profile(value="app_push_state_routing") // used only in portals
    public Filter appForwardFilter(final Environment env) {
        final String appPrefix = env.getProperty("whizcontrol.pushStateRouting.prefix", "/app/");
        
        return new OncePerRequestFilter() {
            
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                if (request.getRequestURI().startsWith(appPrefix)) {
                    request.getRequestDispatcher("/index.html").forward(request, response);
                }
                else {
                    filterChain.doFilter(request, response);
                }
            }
        };
    }
}
