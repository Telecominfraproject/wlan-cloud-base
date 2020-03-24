package com.telecominfraproject.wlan.core.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;

import com.telecominfraproject.wlan.core.client.ExceptionPropagatingErrorHandler;

/**
 * Make sure we handle all our RuntimeException
 * 
 * @author yongli
 *
 */
public class ExceptionPropagatingErrorHandlerTests {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionPropagatingErrorHandlerTests.class);

    @Test
    public void testAllExceptions() {
        ExceptionPropagatingErrorHandler handler = new ExceptionPropagatingErrorHandler();
        Reflections reflections = new Reflections("com.telecominfraproject.wlan");
        for (Class<? extends RuntimeException> expClass : reflections.getSubTypesOf(RuntimeException.class)) {
            testHandling(handler, expClass);
        }
    }

    private void testHandling(ExceptionPropagatingErrorHandler handler, Class<? extends RuntimeException> expClass) {
        LOG.debug("Test exception {}", expClass.getName());

        try {
            RuntimeException exception = expClass.newInstance();

            ClientHttpResponse response = new TestClientExceptionResponse(exception);
            try {
                handler.handleError(response);
            } catch (Exception decoded) {
                assertEquals(exception.getClass().getName(), exception.getClass(), decoded.getClass());
            }
        } catch (InstantiationError | InstantiationException | IllegalAccessException exp) {
            LOG.error("Can't instantiate {}", expClass.getName());
        }
    }

}
