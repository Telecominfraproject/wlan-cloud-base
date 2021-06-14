package com.telecominfraproject.wlan.core.server.webconfig;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.telecominfraproject.wlan.core.model.json.JsonSerializedException;

/**
 * @author dtoptygin
 */
//@ControllerAdvice(basePackages = "com.telecominfraproject")
public class CommonControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(CommonControllerAdvice.class);

    private static Pattern securityRepacementRegexPattern = Pattern.compile("[\n|\r|\t]");
    
    /**
     * Custom exception handler, it will be applied to all methods (both sync
     * and async) on all controllers
     */
    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonSerializedException customExceptionHandler(HttpServletRequest request, Exception e) {

        return generateException(request, e);
    }

    public static JsonSerializedException generateException(HttpServletRequest request, Exception e) {
        // based on
        // org.springframework.web.filter.AbstractRequestLoggingFilter.createMessage(HttpServletRequest,
        // String, String)
        StringBuilder msg = new StringBuilder();
        // Replace pattern-breaking characters
        msg.append(securityRepacementRegexPattern.matcher(request.getRequestURI()).replaceAll( "_"));

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }
        String requestPath = Encode.forHtml(msg.toString());

        // now continue building the request details for logging
        String client = request.getRemoteAddr();
        if (StringUtils.hasLength(client)) {
            msg.append(";client=").append(client);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            msg.append(";session=").append(session.getId());
        }
        String user = request.getRemoteUser();
        if (user != null) {
            // Replace pattern-breaking characters
            msg.append(";user=").append(securityRepacementRegexPattern.matcher(user).replaceAll( "_"));
        }

        String requestDetails = msg.toString();

        LOG.error("Error in controller when handling 'uri={}'[{}]", requestDetails, request.getMethod(), e);

        JsonSerializedException jse = new JsonSerializedException();
        jse.setExType(e.getClass().getSimpleName());
        jse.setError(e.getLocalizedMessage());
        jse.setPath(requestPath);
        jse.setTimestamp(System.currentTimeMillis());

        return jse;
    }

}
