package com.telecominfraproject.wlan.core.server.container;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitProperties {
    private static final Logger LOG = LoggerFactory.getLogger(CommitProperties.class);

	private static final String PROP_FILE = "commit.properties";
	private final Properties prop = new Properties();
	private final String date;
	private final String commitId;
	private final String version;

	public CommitProperties() {
        loadStream(prop);
		this.date = prop.getProperty("date", "");
		this.commitId = prop.getProperty("commitId", "");
		this.version = prop.getProperty("projectVersion", "");
	}

	public String getCommitDate() {
		return date;
	}

	public String getCommitID() {
		return commitId;
	}

	public String getProjectVersion() {
		return version;
	}

	private void loadStream(final Properties prop) {
		try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROP_FILE)) {
				prop.load(inputStream);
		} catch (final Exception e) {
			LOG.info("Properties file not present");			
		}
	}
}
