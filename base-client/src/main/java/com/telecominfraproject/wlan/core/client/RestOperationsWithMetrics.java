package com.telecominfraproject.wlan.core.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;

public class RestOperationsWithMetrics implements RestOperations{
    final RestOperations delegate;
    
    private final TagList tags = CloudMetricsTags.commonTags;

    private final Counter getsExecuted;
    private final Counter postsExecuted;
    private final Counter putsExecuted;
    private final Counter deletesExecuted;
    private final Counter exchangesExecuted;
    private final Counter headsExecuted;
    private final Counter optionsExecuted;
    private final Counter execsExecuted;
    private final Counter patchesExecuted;

    private final Counter getsErrors;
    private final Counter postsErrors;
    private final Counter putsErrors;
    private final Counter deletesErrors;
    private final Counter exchangesErrors;
    private final Counter headsErrors;
    private final Counter optionsErrors;
    private final Counter execsErrors;
    private final Counter patchesErrors;

    private final Timer getsTimer;
    private final Timer postsTimer;
    private final Timer putsTimer;
    private final Timer deletesTimer;
    private final Timer exchangesTimer;
    private final Timer headsTimer;
    private final Timer optionsTimer;
    private final Timer execsTimer;
    private final Timer patchesTimer;

    
    public RestOperationsWithMetrics(RestOperations delegate, String metricsId){
        this.delegate = delegate;
        //Monitors.registerObject("RestTemplate-"+metricsId, this);
        
        getsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-getsExecuted").withTags(tags).build());
        postsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-postsExecuted").withTags(tags).build());
        putsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-putsExecuted").withTags(tags).build());
        deletesExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-deletesExecuted").withTags(tags).build());
        exchangesExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-exchangesExecuted").withTags(tags).build());
        headsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-headsExecuted").withTags(tags).build());
        optionsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-optionsExecuted").withTags(tags).build());
        execsExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-execsExecuted").withTags(tags).build());
        patchesExecuted = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-patchesExecuted").withTags(tags).build());

        getsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-getsErrors").withTags(tags).build());
        postsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-postsErrors").withTags(tags).build());
        putsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-putsErrors").withTags(tags).build());
        deletesErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-deletesErrors").withTags(tags).build());
        exchangesErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-exchangesErrors").withTags(tags).build());
        headsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-headsErrors").withTags(tags).build());
        optionsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-optionsErrors").withTags(tags).build());
        execsErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-execsErrors").withTags(tags).build());
        patchesErrors = new BasicCounter(MonitorConfig.builder("RestTemplate-"+metricsId+"-patchesErrors").withTags(tags).build());

        getsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-getsTimer").withTags(tags).build());
        postsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-postsTimer").withTags(tags).build());
        putsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-putsTimer").withTags(tags).build());
        deletesTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-deletesTimer").withTags(tags).build());
        exchangesTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-exchangesTimer").withTags(tags).build());
        headsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-headsTimer").withTags(tags).build());
        optionsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-optionsTimer").withTags(tags).build());
        execsTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-execsTimer").withTags(tags).build());
        patchesTimer = new BasicTimer(MonitorConfig.builder("RestTemplate-"+metricsId+"-patchesTimer").withTags(tags).build());

        DefaultMonitorRegistry.getInstance().register(getsExecuted);
        DefaultMonitorRegistry.getInstance().register(postsExecuted);
        DefaultMonitorRegistry.getInstance().register(putsExecuted);
        DefaultMonitorRegistry.getInstance().register(deletesExecuted);
        DefaultMonitorRegistry.getInstance().register(exchangesExecuted);
        DefaultMonitorRegistry.getInstance().register(headsExecuted);
        DefaultMonitorRegistry.getInstance().register(optionsExecuted);
        DefaultMonitorRegistry.getInstance().register(execsExecuted);

        DefaultMonitorRegistry.getInstance().register(getsErrors);
        DefaultMonitorRegistry.getInstance().register(postsErrors);
        DefaultMonitorRegistry.getInstance().register(putsErrors);
        DefaultMonitorRegistry.getInstance().register(deletesErrors);
        DefaultMonitorRegistry.getInstance().register(exchangesErrors);
        DefaultMonitorRegistry.getInstance().register(headsErrors);
        DefaultMonitorRegistry.getInstance().register(optionsErrors);
        DefaultMonitorRegistry.getInstance().register(execsErrors);

        DefaultMonitorRegistry.getInstance().register(getsTimer);
        DefaultMonitorRegistry.getInstance().register(postsTimer);
        DefaultMonitorRegistry.getInstance().register(putsTimer);
        DefaultMonitorRegistry.getInstance().register(deletesTimer);
        DefaultMonitorRegistry.getInstance().register(exchangesTimer);
        DefaultMonitorRegistry.getInstance().register(headsTimer);
        DefaultMonitorRegistry.getInstance().register(optionsTimer);
        DefaultMonitorRegistry.getInstance().register(execsTimer);
        
    }

    public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;
        
        try{
            T ret = delegate.getForObject(url, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;
        
        try{
            T ret = delegate.getForObject(url, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.getForObject(url, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.getForEntity(url, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.getForEntity(url, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
        getsExecuted.increment();
        Stopwatch s = getsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.getForEntity(url, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                getsErrors.increment();
            }
        }
    }

    public HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException {
        headsExecuted.increment();
        Stopwatch s = headsTimer.start();
        boolean success = false;

        try{
            HttpHeaders ret = delegate.headForHeaders(url, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                headsErrors.increment();
            }
        }
    }

    public HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
        headsExecuted.increment();
        Stopwatch s = headsTimer.start();
        boolean success = false;

        try{
            HttpHeaders ret = delegate.headForHeaders(url, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                headsErrors.increment();
            }
        }
    }

    public HttpHeaders headForHeaders(URI url) throws RestClientException {
        headsExecuted.increment();
        Stopwatch s = headsTimer.start();
        boolean success = false;

        try{
            HttpHeaders ret = delegate.headForHeaders(url);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                headsErrors.increment();
            }
        }
    }

    public URI postForLocation(String url, Object request, Object... uriVariables) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            URI ret = delegate.postForLocation(url, request, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public URI postForLocation(String url, Object request, Map<String, ?> uriVariables) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            URI ret = delegate.postForLocation(url, request, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public URI postForLocation(URI url, Object request) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            URI ret = delegate.postForLocation(url, request);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.postForObject(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> T postForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.postForObject(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> T postForObject(URI url, Object request, Class<T> responseType) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.postForObject(url, request, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
            Object... uriVariables) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.postForEntity(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
            Map<String, ?> uriVariables) throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.postForEntity(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> postForEntity(URI url, Object request, Class<T> responseType)
            throws RestClientException {
        postsExecuted.increment();
        Stopwatch s = postsTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.postForEntity(url, request, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                postsErrors.increment();
            }
        }
    }

    public void put(String url, Object request, Object... uriVariables) throws RestClientException {
        putsExecuted.increment();
        Stopwatch s = putsTimer.start();
        boolean success = false;

        try{
            delegate.put(url, request, uriVariables);
            success = true;
        }finally{
            s.stop();
            if(!success){
                putsErrors.increment();
            }
        }
    }

    public void put(String url, Object request, Map<String, ?> uriVariables) throws RestClientException {
        putsExecuted.increment();
        Stopwatch s = putsTimer.start();
        boolean success = false;

        try{
            delegate.put(url, request, uriVariables);
            success = true;
        }finally{
            s.stop();
            if(!success){
                putsErrors.increment();
            }
        }
    }

    public void put(URI url, Object request) throws RestClientException {
        putsExecuted.increment();
        Stopwatch s = putsTimer.start();
        boolean success = false;

        try{
            delegate.put(url, request);
            success = true;
        }finally{
            s.stop();
            if(!success){
                putsErrors.increment();
            }
        }
    }

    public void delete(String url, Object... uriVariables) throws RestClientException {
        deletesExecuted.increment();
        Stopwatch s = deletesTimer.start();
        boolean success = false;

        try{
            delegate.delete(url, uriVariables);
            success = true;
        }finally{
            s.stop();
            if(!success){
                deletesErrors.increment();
            }
        }
    }

    public void delete(String url, Map<String, ?> uriVariables) throws RestClientException {
        deletesExecuted.increment();
        Stopwatch s = deletesTimer.start();
        boolean success = false;

        try{
            delegate.delete(url, uriVariables);
            success = true;
        }finally{
            s.stop();
            if(!success){
                deletesErrors.increment();
            }
        }
    }

    public void delete(URI url) throws RestClientException {
        deletesExecuted.increment();
        Stopwatch s = deletesTimer.start();
        boolean success = false;

        try{
            delegate.delete(url);
            success = true;
        }finally{
            s.stop();
            if(!success){
                deletesErrors.increment();
            }
        }
    }

    public Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
        optionsExecuted.increment();
        Stopwatch s = optionsTimer.start();
        boolean success = false;

        try{
            Set<HttpMethod> ret = delegate.optionsForAllow(url, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                optionsErrors.increment();
            }
        }
    }

    public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
        optionsExecuted.increment();
        Stopwatch s = optionsTimer.start();
        boolean success = false;

        try{
            Set<HttpMethod> ret = delegate.optionsForAllow(url, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                optionsErrors.increment();
            }
        }
    }

    public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
        optionsExecuted.increment();
        Stopwatch s = optionsTimer.start();
        boolean success = false;

        try{
            Set<HttpMethod> ret = delegate.optionsForAllow(url);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                optionsErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
            Class<T> responseType, Object... uriVariables) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
            Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
            Class<T> responseType) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType) throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(url, method, requestEntity, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType)
            throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(requestEntity, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
            throws RestClientException {
        exchangesExecuted.increment();
        Stopwatch s = exchangesTimer.start();
        boolean success = false;

        try{
            ResponseEntity<T> ret = delegate.exchange(requestEntity, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                exchangesErrors.increment();
            }
        }
    }

    public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
            ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(url, method, requestCallback, responseExtractor, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
            ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(url, method, requestCallback, responseExtractor, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T execute(URI url, HttpMethod method, RequestCallback requestCallback,
            ResponseExtractor<T> responseExtractor) throws RestClientException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(url, method, requestCallback, responseExtractor);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    @Override
    public <T> T patchForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        patchesExecuted.increment();
        Stopwatch s = patchesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.patchForObject(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                patchesErrors.increment();
            }
        }
    }

    @Override
    public <T> T patchForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        patchesExecuted.increment();
        Stopwatch s = patchesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.patchForObject(url, request, responseType, uriVariables);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                patchesErrors.increment();
            }
        }
    }

    @Override
    public <T> T patchForObject(URI url, Object request, Class<T> responseType) throws RestClientException {
        patchesExecuted.increment();
        Stopwatch s = patchesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.patchForObject(url, request, responseType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                patchesErrors.increment();
            }
        }
    }
    
}