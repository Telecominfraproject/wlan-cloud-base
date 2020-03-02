/**
 * 
 */
package com.telecominfraproject.wlan.core.model.service.bonjour;

import java.util.Arrays;
import java.util.List;

/**
 * See http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml
 * 
 * @author yongli
 *
 */
public enum BonjourService {
    /**
     * 
     */
    AirPlay("AirPlay", Arrays.asList("airplay")),
    /**
     * https://developer.apple.com/bonjour/printing-specification/bonjourprinting-1.2.pdf
     */
    AirPrint("Bonjour Printing", Arrays.asList("printer", "ipp", "ipps", "pdl-datastream")),
    /**
     * AirPort Base Station
     */
    AirPort("AirPort Base Station", Arrays.asList("airport")),
    /**
     * Apple File Sharing
     */
    AFP("Apple File Sharing", Arrays.asList("afpovertcp")),
    /**
     * AirTunes
     */
    AirTunes("Remote Audio Output Protocol (RAOP)", Arrays.asList("raop")),
    /**
     * Google Cast
     */
    GoogleCast("Google Cast", Arrays.asList("googlecast")),
    /**
     * Server Message Block over TCP/IP
     */
    Samba("Server Message Block over TCP/IP", Arrays.asList("smb")),
    /**
     * Windows Remote Desktop Protocol
     */
    RDP("Windows Remote Desktop", Arrays.asList("rdp")),
    /**
     * Secure File Transfer Protocol over SSH
     */
    SFTP("Secure File Transfer Protocol over SSH", Arrays.asList("sftp-ssh")),
    /**
     * SSH Remote Login Protocol
     */
    SSH("SSH Remote Login", Arrays.asList("ssh"));

    private final List<String> applicationNames;
    private final String description;

    BonjourService(String description, List<String> applicationNames) {
        this.description = description;
        this.applicationNames = applicationNames;
    }

    /**
     * @return the applicationNames
     */
    public List<String> getApplicationNames() {
        return this.applicationNames;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    public String getServiceName() {
        return name();
    }

}
