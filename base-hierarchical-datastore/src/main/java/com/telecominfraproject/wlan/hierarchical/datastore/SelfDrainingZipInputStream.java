/**
 * 
 */
package com.telecominfraproject.wlan.hierarchical.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementing <a href=
 * "https://github.com/aws/aws-sdk-java/issues/1111">https://github.com/aws/aws-sdk-java/issues/1111</a>
 * in response to NAAS-9238
 * 
 * @author ekeddy
 *
 */
public class SelfDrainingZipInputStream extends ZipInputStream {
    private static final Logger LOG = LoggerFactory.getLogger(SelfDrainingZipInputStream.class);

    public SelfDrainingZipInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // Drain before closing to keep S3 client happy
        while (getNextEntry() != null) {
        }
        LOG.debug("Draining inputstream");
        // drain the InputStream
        while (in.read() >= 0) {
        }
        super.close();
    }
}
