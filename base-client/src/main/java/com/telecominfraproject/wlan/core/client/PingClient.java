package com.telecominfraproject.wlan.core.client;

import com.telecominfraproject.wlan.core.server.controller.ping.PingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 *  This class uses the ping URI to verify if the component is reachable
 *  author: Rahul Sharma
 */
@Component
public class PingClient extends BaseRemoteClient {

    private static final Logger LOG = LoggerFactory.getLogger(PingClient.class);
    private static final String pingURL = "https://%s:%s/ping";

    /**
     * Checks the reachability of a service (given it's IPAddress and Port)
     * @param ipAddr: Component/µService/Pod's IP Address
     * @param port: Port it's listening on
     * @return true is reachable, false otherwise
     */
    public boolean isReachable(String ipAddr, int port) {
        LOG.debug("Invoking Ping on IP:{} Port: {}", ipAddr, port);
        String appNameFromEnv = environment.getProperty("app.name");
        return isReachable(ipAddr, port, appNameFromEnv);
    }

    private URI getPingURI(String ipAddr, int port) throws URISyntaxException {
        return new URI(String.format(pingURL, ipAddr, port));
    }

    /**
     * Checks the reachability of a service (given it's IPAddress, Port and Name)
     * @param ipAddr: Component/µService/Pod's IP Address
     * @param port: Port it's listening on
     * @param appName: Expected Application Name in the Ping response
     * @return true is reachable, false otherwise
     */
    public boolean isReachable(String ipAddr, int port, String appName) {
        LOG.debug("Invoking Ping on IP:{} Port: {} with AppName {} ", ipAddr, port, appName);
        boolean reachable = false;
        try {
            ResponseEntity<PingResponse> responseEntity = restTemplate.getForEntity(
                    getPingURI(ipAddr, port), PingResponse.class);

            PingResponse resp = responseEntity.getBody();

            if (resp != null) {
                if (resp.getApplicationName().contains(appName)) {
                    LOG.debug("Ping successful for ipAddr {}, port {} from {} component", ipAddr, port, appName);
                    reachable = true;
                } else {
                    LOG.debug("Ping worked for ipAddr {}, port {} but component names isn't expected. " +
                            "Expected {} Obtained: {}", ipAddr, port, appName, resp.getApplicationName());
                }
            } else {
                LOG.debug("No ping response received from ipAddr {}, port {} and component: {}", ipAddr, port,
                        appName);
            }
        } catch (URISyntaxException ex) {
            LOG.error("Unable to create the PING URI for IPAddr {} and Port {}. Error: {} ", ipAddr, port, ex.getMessage());
        } catch (Exception ex) {
            LOG.error("General exception encountered when pinging IP {} and Port {}. Error: {} ", ipAddr, port, ex.getMessage());
        }
        return reachable;
    }
}
