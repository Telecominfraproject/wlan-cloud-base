package com.telecominfraproject.wlan.core.server.container;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CommitProperties {
	private static final String PROP_FILE = "commit.properties";
	private final InputStream inputStream;
	private final Properties prop = new Properties();

	public CommitProperties() {
		this.inputStream = getClass().getClassLoader().getResourceAsStream(PROP_FILE);
	}

	public String getCommitDate() {
		return getProperty("date");
	}

	public String getCommitID() {
		return getProperty("commitId");
	}

	public String getProjectVersion() {
		return getProperty("projectVersion");
	}

	private String getProperty(final String property) {
		String result = "";
		if (inputStream != null) {
			try {
				prop.load(inputStream);
				return prop.getProperty(property, "");
			} catch (IOException e) {

			}
		}
		return result;
	}
}
