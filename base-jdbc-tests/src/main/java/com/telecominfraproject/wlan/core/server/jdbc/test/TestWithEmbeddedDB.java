package com.telecominfraproject.wlan.core.server.jdbc.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Be careful: This annotation will be overwritten by any
 * other @ActiveProfiles annotations.</b>
 * 
 * @author dtop
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ActiveProfiles(profiles = {
        // test against embedded database
        "use_embedded_db", "use_single_ds" })
public @interface TestWithEmbeddedDB {

}
