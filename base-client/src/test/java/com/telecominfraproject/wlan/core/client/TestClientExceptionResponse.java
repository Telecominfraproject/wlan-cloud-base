/**
 * 
 */
package com.telecominfraproject.wlan.core.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import com.telecominfraproject.wlan.core.model.json.JsonSerializedException;

/**
 * @author yongli
 *
 */
public class TestClientExceptionResponse implements ClientHttpResponse {

    private final String body;
    private HttpHeaders headers;

    /**
     * see
     * com.whizcontrol.core.server.webconfig.CommonControllerAdvice.generateException(HttpServletRequest,
     * Exception)
     * 
     * @param exception
     */
    public TestClientExceptionResponse(RuntimeException exception) {
        JsonSerializedException jse = new JsonSerializedException();
        jse.setExType(exception.getClass().getSimpleName());
        jse.setError(exception.getLocalizedMessage());
        jse.setPath("/test");
        jse.setTimestamp(System.currentTimeMillis());
        body = jse.toString();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
    }

    public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(body.getBytes());
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    @Override
    public String getStatusText() throws IOException {
        return "";
    }

    @Override
    public void close() {
    }

}
