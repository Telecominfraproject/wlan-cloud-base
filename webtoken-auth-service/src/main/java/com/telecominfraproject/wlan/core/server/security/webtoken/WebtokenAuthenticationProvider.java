package com.telecominfraproject.wlan.core.server.security.webtoken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.RestOperations;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.webtoken.IntrospectWebTokenResult;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * Class that calls external API to introspect the JWT token, and if it is valid,
 * then populates userdetails in the authentication object.
 *
 */
public class WebtokenAuthenticationProvider implements AuthenticationProvider, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(WebtokenAuthenticationProvider.class);
    private final RestOperations restTemplate;
    private final String introspectTokenApiEndPoint;
    private final String introspectTokenApiClientToken;

    private final HttpHeaders headers = new HttpHeaders();

    public WebtokenAuthenticationProvider(RestOperations restTemplate, String introspectTokenApiProtocol, String introspectTokenApiHost, String introspectTokenApiClientToken) {
        this.restTemplate = restTemplate;
        this.introspectTokenApiEndPoint = introspectTokenApiProtocol +"://" + introspectTokenApiHost
                + "/management/v1/oauth2/introspecttoken";
        this.introspectTokenApiClientToken = introspectTokenApiClientToken;

        LOG.info("configured WebtokenAuthenticationProvider with {}", this.introspectTokenApiEndPoint);

        headers.setAccept(Arrays.asList(new MediaType[] {new MediaType("application", "json", StandardCharsets.UTF_8)}));
        headers.setContentType(new MediaType("application", "x-www-form-urlencoded"));
        headers.set("Authorization", "Bearer "+this.introspectTokenApiClientToken);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String token = ((WebtokenJWTToken) authentication).getJwt();
        String fullUrl = ((WebtokenJWTToken) authentication).getFullUrl();
        String requestMethod = ((WebtokenJWTToken) authentication).getRequestMethod();

        LOG.debug("Trying to authenticate {} with webtoken: {}", fullUrl, token);

        ResponseEntity<String> responseEntityStr;
        String introspectionResponseStr = "";
        try {

            //logger.trace("Decoded (by spring) token: " + org.springframework.security.jwt.JwtHelper.decode(token));

            //tokenTypeHint=tip.wlan&requestUrl={fullUrl}&requestMethod={requestMethod}&token={token}
            String bodyFormatStr = "tokenTypeHint=tip.wlan&requestUrl=%s&requestMethod=%s&token=%s";
            String body = String.format(bodyFormatStr, URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.name()), requestMethod, URLEncoder.encode(token, StandardCharsets.UTF_8.name()));

            //call out to the authentication endpoint to verify supplied token and url
            HttpEntity<String> request = new HttpEntity<>(body, headers);


            responseEntityStr = restTemplate.postForEntity(
                    introspectTokenApiEndPoint, request, String.class);

            LOG.debug("Response from token introspection {}", responseEntityStr);

            introspectionResponseStr = responseEntityStr.getBody();
        } catch (Exception e) {
            LOG.error("Exception thrown while calling token introspection endpoint", e);
            throw new WebtokenAuthTokenException("Authentication error occured");
        }


        if(responseEntityStr.getStatusCode().equals(HttpStatus.OK)){

            IntrospectWebTokenResult introspectTokenResult;
            try{
                introspectTokenResult = BaseJsonModel.fromString(introspectionResponseStr, IntrospectWebTokenResult.class);
            } catch (Exception e) {
                LOG.error("Exception thrown while parsing result of the token introspection endpoint", e);
                throw new WebtokenAuthTokenException("Authentication error occured");
            }

            //curl -k -X POST "https://${tip.wlan.introspectTokenApi.host}/management/v1/oauth2/introspecttoken?requestUrl=ORIGINAL_API_URL_WITH_REQUEST_PARAMETERS&amp;token=BEARER_TOKEN&amp;requestMethod=GET&amp;tokenTypeHint=tip.wlan" -H "Accept: application/json" -H "Content-Type: application/x-www-form-urlencoded" -H "Authorization: Bearer ${tip.wlan.introspectTokenApi.clientToken}"
            /*
                {
                    "active": true,
                    "errorCode": 200,
                    "customerId": "42" //this is optional field
                }
             */

            WebtokenJWTToken tokenAuth = ((WebtokenJWTToken) authentication);
            tokenAuth.setAuthenticated(introspectTokenResult.isActive());
            WebtokenUserDetails userDetails = new WebtokenUserDetails(introspectTokenResult);
            tokenAuth.setPrincipal(userDetails);
            tokenAuth.setDetails(userDetails);

            if(introspectTokenResult.getErrorCode()!=HttpStatus.OK.value()){
                throw new WebtokenAuthTokenException("Authentication error occured", introspectTokenResult.getErrorCode());
            }

        } else {
            LOG.error("Unexpected object when introspecting token {}", responseEntityStr);
            throw new WebtokenAuthTokenException("Authentication error occured");
        }


        return authentication;
    }

    public boolean supports(Class<?> authentication) {
        return WebtokenJWTToken.class.isAssignableFrom(authentication);
    }

    public void afterPropertiesSet() throws Exception {
        if ((introspectTokenApiEndPoint == null) || (introspectTokenApiClientToken == null)) {
            throw new ConfigurationException("introspectTokenApiEndPoint or introspectTokenApiClientToken is not set for WebtokenAuthenticationProvider");
        }
    }

}
