package com.telecominfraproject.wlan.core.model.equipment;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.serializers.Base64UrlByteArrayDeserializer;
import com.telecominfraproject.wlan.core.model.serializers.Base64UrlByteArraySerializer;

@JsonSerialize()
@JsonIgnoreProperties(ignoreUnknown = true)
public class MacAddress extends BaseJsonModel implements Comparable<MacAddress> 
{
    private static final long serialVersionUID = 5132506123062405860L;
    /**
     * Length of the raw value
     */
    public static final int VALUE_LENGTH = 6;

    private static final int GROUP_BIT = (0x1);
    private static final int GLOBE_BIT = (0x1 << 1);

    private byte[] address;

    public MacAddress() {
    }

    public MacAddress(String str) {
        address = stringToByteArray(str);
    }

    public MacAddress(byte[] address) {
        this.address = address;
    }

    public MacAddress(Long valueAsLong) {
        this(new byte[] { (byte) ((valueAsLong >> 40) & 0xff), (byte) ((valueAsLong >> 32) & 0xff),
                (byte) ((valueAsLong >> 24) & 0xff), (byte) ((valueAsLong >> 16) & 0xff),
                (byte) ((valueAsLong >> 8) & 0xff), (byte) (valueAsLong & 0xff) });
    }

    @JsonDeserialize(using = Base64UrlByteArrayDeserializer.class)
    public void setAddress(byte[] address) {
        this.address = address;
    }

    @JsonSerialize(using = Base64UrlByteArraySerializer.class)
    public byte[] getAddress() {
        return this.address;
    }

    @JsonIgnore
    public Long getAddressAsLong() 
    {
        if(address.length >= 6)
        {
            long mac = 0;
            for (var i = 0; i < 6; i++) {
                long t = (address[i] & 0xffL) << ((5 - i) * 8);
                mac |= t;
            }
            return mac;
        }
        
        return null;
    }

    public void setAddressAsString(String macAsString) {
        setAddress(stringToByteArray(macAsString));
    }
    
    public String getAddressAsString() {
        var sb = new StringBuilder(124);
        
        if(address != null)
        {
            for (byte single : address) {
                sb.append(String.format("%02x", single)).append(':');
            }

            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        
        return null;
    }

    @JsonIgnore
    public String getAsLowerCaseString() {
        var sb = new StringBuilder(124);
        for (byte single : address) {
            sb.append(String.format("%02x", single));
        }
        return sb.toString();
    }

    public static MacAddress valueOf(byte[] mac) {
        if(mac == null) return null;
        return new MacAddress(mac);
    }

    public static MacAddress valueOf(String mac) {
        if(mac == null) return null;
        return new MacAddress(mac);
    }

    public static MacAddress valueOf(long mac) {
        return new MacAddress(mac);
    }

    public static MacAddress valueOf(Object clientMacAddress) {
        if(clientMacAddress == null) {
            return null;
        }
        if(clientMacAddress instanceof MacAddress) {
            return (MacAddress) clientMacAddress;
        }
        if(clientMacAddress instanceof byte[]) {
            return valueOf((byte[]) clientMacAddress);
        }
        if(clientMacAddress instanceof Long) {
            return valueOf((long) clientMacAddress);
        }
        if(clientMacAddress instanceof String) {
            return valueOf((String) clientMacAddress);
        }
        throw new IllegalArgumentException("Argument type is not valid for MacAddress: "+clientMacAddress);
    }

    @Override
   public int hashCode() {
      final var prime = 31;
      var result = 1;
      result = prime * result + Arrays.hashCode(address);
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      MacAddress other = (MacAddress) obj;
      if (!Arrays.equals(address, other.address))
         return false;
      return true;
   }

    @Override
    public MacAddress clone() {
        MacAddress result = (MacAddress) super.clone();
        if (null != this.address) {
            result.address = new byte[address.length];
            System.arraycopy(address, 0, result.address, 0, address.length);
        }
        return result;
    }

    
    private static byte[] stringToByteArray(String str) {
        if (str == null || str.equals(""))
        {
            return null;
        }
        var ret = new byte[6];

        String octets[] = str.split(":");
        if(octets.length == 1 && octets[0].length() == str.length() && str.length()<=12) {
            // hex string without colon
            for(var i = 0; i< str.length(); i+=2) {
                Integer hex = Integer.parseInt(str.substring(i, i==str.length()-1?i+1:i+2), 16);               
                ret[i/2] = hex.byteValue();
            }
        }
        else if(octets.length>6) {
            throw new IllegalArgumentException("The value "+str+" is not a valid length for a MAC address; expecting 6 colon-separated bytes in hex");
        }
        else {
            try {
                for (var i = 0; i < octets.length; i++) {
                    Integer hex = Integer.parseInt(octets[i], 16);
                    ret[i] = hex.byteValue();
                }
            }
            catch(NumberFormatException e) {
                throw new IllegalArgumentException("The value "+str+" is not valid content for a MAC address; expecting 6 colon-separated bytes in hex");
            }
        }
        return ret;

    }
    
    /**
     * Extract OUI string from value output from {@link #getAsLowerCaseString()}
     * 
     * @param lowercaseValue
     * @return OUI
     */
    public static String ouiFromLowerCaseString(String lowercaseValue) {
        return lowercaseValue.substring(0, 6);
    }

    public static String ouiFromLowerCaseString(String lowercaseValue, boolean separator) 
    {
        if(!separator)
        {
            return ouiFromLowerCaseString(lowercaseValue);
        }
        else
        {
            String[] value = lowercaseValue.split(":");
            var sb = new StringBuilder(6);
            for(var i=0; i<3; i++)
            {
                sb.append(value[i].toLowerCase());
            }

            return sb.toString();
        }
    }
    
    public String toOuiString() {
        if(address == null || address.length == 0) {
            return null;
        }
        var sb = new StringBuilder(6);
        for (var i = 0; i< 3; i++) {
            sb.append(String.format("%02x", address[i]));
        }
        return sb.toString();
    }

    @Override
    public boolean hasUnsupportedValue() {
        return false;
    }

    @Override
    public int compareTo(MacAddress o) 
    {
        if(o != null)
        {
            return this.getAddressAsLong().compareTo(o.getAddressAsLong());
        }
        
        return -1;
    }

    /**
     * Test if this is a globally administered address
     * 
     * @return
     */
    @JsonIgnore
    public boolean isGlobalAddress() {
        if (address != null) {
            return ((address[0] & GLOBE_BIT) == 0);
        }
        return false;
    }

    /**
     * Test if this is a group address
     */
    @JsonIgnore
    public boolean isGroupAddress() {
        if (address != null) {
            return ((address[0] & GROUP_BIT) != 0);
        }
        return false;
    }

    
    public static String generateReadableMac(MacAddress addr)
    {
        if(addr != null)
        {
            return addr.getAddressAsString();
        }

        return null;
    }

    /**
     * Mac Str with colon: AA:BB:CC:DD:EE:FF
     * 
     * @return
     */
    public static Long convertMacStringToLongValue(String macStr) {
        
        if (macStr == null)
        {
            return null;
        }
        
        byte[] bval = stringToByteArray(macStr);

        if (bval != null && bval.length >= 6) {
            long mac = 0;
            for (var i = 0; i < 6; i++) {
                long t = (bval[i] & 0xffL) << ((5 - i) * 8);
                mac |= t;
            }
            return mac;
        }
        return null;
    }

}
