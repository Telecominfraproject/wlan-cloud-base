/**
 * 
 */
package com.telecominfraproject.wlan.core.client.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.client.models.HttpClientConfig;

/**
 * @author yongli
 *
 */
public class PasswordTools {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordTools.class);

    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption("k", "key", true, "Encryption key");
            options.addOption("o", "obfscate", true, "Obfscate clear password");
            options.addOption("d", "decode", true, "Decode password");
            options.addOption("h", "help", false, "Print help screen");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(PasswordTools.class.getSimpleName(), options);
                System.exit(0);
            }

            if (cmd.hasOption("o")) {
                String password = cmd.getOptionValue("o");
                String result = HttpClientConfig.obfStorePasswordValue(password, getEncKey(cmd));
                LOG.info("{}", result);
            } else if (cmd.hasOption("d")) {
                String password = cmd.getOptionValue("d");
                String result = HttpClientConfig.decodeStorePasswordValue(password, getEncKey(cmd));
                LOG.info("{}", result);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(PasswordTools.class.getSimpleName(), options);
            }
            System.exit(0);
        } catch (Exception e) {
            LOG.error("{}", e.getLocalizedMessage());
        }
    }

    private static String getEncKey(CommandLine cmd) {
        if (!cmd.hasOption("k")) {
            LOG.error("Missing dencryption key, use -h for usage!");
            System.exit(1);
        }
        return cmd.getOptionValue("k");
    }
}
