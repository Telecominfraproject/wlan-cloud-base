package com.telecominfraproject.wlan.core.client;

import com.telecominfraproject.wlan.core.server.controller.ping.PingResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestOperations;

import java.net.URI;

import static org.mockito.Mockito.*;

public class PingClientTest {

    PingClient pingClient = mock(PingClient.class, CALLS_REAL_METHODS);

    RestOperations restTemplate = mock(RestOperationsWithMetrics.class);

    Environment env = mock(Environment.class);

    @Test
    public void test_isReachable() {
        String ip = "10.121.32.111";
        int port = 9096;

        PingResponse resp = new PingResponse(0,0, "OpensyncMgr",
                "tip-wlan-opensyncMgr");
        ResponseEntity<PingResponse> responseEntity = new ResponseEntity<>(resp, HttpStatus.OK);
        Mockito.when(restTemplate.getForEntity(any(URI.class), any(Class.class))).thenReturn(responseEntity);
        Mockito.when(env.getProperty("app.name")).thenReturn("OpensyncMgr");
        ReflectionTestUtils.setField(pingClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(pingClient, "environment", env);

        Assert.assertTrue(pingClient.isReachable(ip, port));
    }

    @Test
    public void test_NotReachable() {
        String ip = "10.121.32.111";
        int port = 9096;
        Mockito.when(restTemplate.getForEntity(any(URI.class), any(Class.class))).thenThrow(new RuntimeException());
        Mockito.when(env.getProperty("app.name")).thenReturn("OpensyncMgr");
        ReflectionTestUtils.setField(pingClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(pingClient, "environment", env);

        Assert.assertFalse(pingClient.isReachable(ip, port));
    }
}