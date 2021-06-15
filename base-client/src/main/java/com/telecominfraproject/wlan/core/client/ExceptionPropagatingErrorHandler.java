package com.telecominfraproject.wlan.core.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import com.telecominfraproject.wlan.client.exceptions.ClientIncorrectServerException;
import com.telecominfraproject.wlan.client.exceptions.ClientRemoteConnectionException;
import com.telecominfraproject.wlan.core.model.json.JsonSerializedException;
import com.telecominfraproject.wlan.datastore.exceptions.DsConcurrentModificationException;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;
import com.telecominfraproject.wlan.datastore.exceptions.DsDuplicateEntityException;
import com.telecominfraproject.wlan.datastore.exceptions.DsEntityNotFoundException;
import com.telecominfraproject.wlan.datastore.exceptions.DsForeignKeyViolatedException;
import com.telecominfraproject.wlan.datastore.exceptions.DsInvalidNumberEntitiesReturned;
import com.telecominfraproject.wlan.models.exceptions.DataInstantiationException;
import com.telecominfraproject.wlan.rules.exceptions.RulesCompilationException;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;
import com.telecominfraproject.wlan.server.exceptions.SerializationException;

/**
 * @author dtoptygin
 *
 */
public class ExceptionPropagatingErrorHandler extends DefaultResponseErrorHandler implements ResponseErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionPropagatingErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        try {
            super.handleError(response);
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            // look in the response object, and if it contains any of the
            // exceptions we recognize - throw that exception instead of the
            // HttpServerErrorException
            String responseBody = e.getResponseBodyAsString();
            JsonSerializedException jse;
            try {
                jse = JsonSerializedException.fromString(responseBody,
                        JsonSerializedException.class);
            } catch (RuntimeException re) {
                LOG.error("cannot parse remote exception {} : {}", responseBody, re);
                // will re-throw original exception in this case
                throw e;
            }

            if (jse.getExType() == null) {
                jse.setExType("unknownType");
            }

            switch (jse.getExType()) {
            case "DsConcurrentModificationException":
                throw new DsConcurrentModificationException(jse.getError());
            case "DsDuplicateEntityException":
                throw new DsDuplicateEntityException(jse.getError());
            case "DsEntityNotFoundException":
                throw new DsEntityNotFoundException(jse.getError());
            case "DsDataValidationException":
                throw new DsDataValidationException(jse.getError());
            case "DsInvalidNumberEntitiesReturned":
                throw new DsInvalidNumberEntitiesReturned(jse.getError());
            case "DsForeignKeyViolatedException":
                throw new DsForeignKeyViolatedException(jse.getError());
            case "DataInstantiationException":
                throw new DataInstantiationException(jse.getError());
            case "RuntimeException":
                throw new RuntimeException(jse.getError());
            case "IllegalStateException":
                throw new IllegalStateException(jse.getError());
            case "IllegalArgumentException":
                throw new IllegalArgumentException(jse.getError());
            case "ConfigurationException":
                throw new ConfigurationException(jse.getError());
            case "SerializationException":
                throw new SerializationException(jse.getError());
            case "RulesCompilationException":
                throw new RulesCompilationException(jse.getError());
            case "TaskRejectedException":
                throw new TaskRejectedException(jse.getError());
            case "ClientRemoteConnectionException":
                throw new ClientRemoteConnectionException(jse.getError());
            case "ClientIncorrectServerException":
                throw new ClientIncorrectServerException(jse.getError());
            case "GenericErrorException":
                throw new GenericErrorException(jse.getError());
                // Spring Security Core Exception
            case "AccessDeniedException":
                throw new AccessDeniedException(jse.getError());
            default:
                LOG.error("do not know how to deal with {} in {}", jse.getExType(), responseBody);
                throw e;
            }
        }
    }
}
