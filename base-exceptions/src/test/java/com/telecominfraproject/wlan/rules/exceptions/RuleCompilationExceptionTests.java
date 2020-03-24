package com.telecominfraproject.wlan.rules.exceptions;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.rules.exceptions.RulesCompilationException;

public class RuleCompilationExceptionTests {

    private static final Logger LOG = LoggerFactory.getLogger(RuleCompilationExceptionTests.class);

    /**
     * New log4j should dump the exception stack trace
     */
    @Test
    public void testLogger() {
        try {
            throw new RulesCompilationException("Test ...");
        } catch (RuntimeException exp) {
            LOG.error("Got exp {}", exp.getLocalizedMessage(), exp);
        }
    }

}
