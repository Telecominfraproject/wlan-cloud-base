/**
 * 
 */
package com.telecominfraproject.wlan.core.model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.telecominfraproject.wlan.core.model.service.CloudServiceConstants;
import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.core.model.utils.SystemAndEnvPropertyResolver;

/**
 * @author yongli
 *
 */
@Configuration
public class ServiceInstanceInformation {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceInstanceInformation.class);

    public static final String DEPLOYMENT_TYPE_UNKNOWN = "unknown";
    public static final String DEPLOYMENT_TYPE_DEV = "dev";
    public static final String DEPLOYMENT_TYPE_PROD = "prod";

    @Autowired
    private Environment enviornment;

    /**
     * Deployment Id for the current service instance.
     */
    Integer deploymentId = null;

    /**
     * Deployment type: dev, prod or UNKNOWN
     */
    String deploymentType = null;

    /**
     * Return the current deployment identifier. It picks up from environment
     * "deployment". If not set, the default deployment id is returned.
     * 
     * @return deployment Id
     */
    public int getDeploymentId() {
        if (null == this.deploymentId) {
            synchronized (this) {
                if (null == this.deploymentId) {
                    this.deploymentId = setupDeploymentId();
                }
            }
        }
        return this.deploymentId.intValue();
    }

    static Integer setupDeploymentId() {
        Integer result = CloudServiceConstants.DEFAULT_DEPLOYMENT_ID;
        String deploymentIdStr = SystemAndEnvPropertyResolver.getPropertyAsString("deployment", "");
        try {
            if (deploymentIdStr.isEmpty()) {
                result = CloudServiceConstants.DEFAULT_DEPLOYMENT_ID;
            } else {
                result = Integer.parseInt(deploymentIdStr);
            }
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse deploymentId {}, use default value {}", deploymentIdStr,
                    CloudServiceConstants.DEFAULT_DEPLOYMENT_ID);
            result = CloudServiceConstants.DEFAULT_DEPLOYMENT_ID;
        }
        return result;
    }

    /**
     * Return the deployment type set in EC2 environment
     * 
     * @return
     */
    public String getDeploymentType() {
        if (null == this.deploymentType) {
            synchronized (this) {
                if (null == this.deploymentType) {
                    deploymentType = enviornment.getProperty("tip.wlan.deploy.type", DEPLOYMENT_TYPE_UNKNOWN);
                }
            }
        }
        return deploymentType;
    }

    /**
     * Check if this is production environment
     * 
     * @return
     */
    public boolean isProductionDeployment() {
        return DEPLOYMENT_TYPE_PROD.equals(getDeploymentType());
    }

    /**
     * Check if this is development environment.
     * 
     * @return
     */
    public boolean isDevelopmentDeployment() {
        return DEPLOYMENT_TYPE_DEV.equals(getDeploymentType());
    }
}
