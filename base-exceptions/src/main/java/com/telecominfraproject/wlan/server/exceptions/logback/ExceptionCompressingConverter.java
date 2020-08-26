package com.telecominfraproject.wlan.server.exceptions.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

/**
 * @author dtop
 *
 * Example of logback.xml:
 <configuration>

  <conversionRule conversionWord="filteredStack" 
                  converterClass="com.telecominfraproject.wlan.server.exceptions.logback.ExceptionCompressingConverter" />
        
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern> [%thread] - %msg%n%filteredStack%nopex</pattern>
    </encoder>
  </appender>

  <root level="DEBUG">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
 */
public class ExceptionCompressingConverter extends ClassicConverter {
    
    private static final String[] topLevelPackages;
    
    static {

        String vendorTopLevelPackagesStr = System.getProperty("tip.wlan.vendorTopLevelPackages", "");
        
        List<String> pkgs = new ArrayList<>();
        pkgs.add("com.telecominfraproject.wlan");
        
        //add vendor packages
        if(vendorTopLevelPackagesStr!=null) {
            String[] vendorPkgs = vendorTopLevelPackagesStr.split(",");
            for(int i=0; i< vendorPkgs.length; i++) {
                if(vendorPkgs[i].trim().isEmpty()) {
                    continue;
                }
                
                pkgs.add(vendorPkgs[i]);
            }
        }
        
        topLevelPackages = pkgs.toArray(new String[0]);
    }


	@Override
	public String convert(ILoggingEvent event) {

		StringBuilder strb = new StringBuilder(256);
		
		IThrowableProxy itp = event.getThrowableProxy();
		if(itp!=null) {
			strb.append(itp.getClassName()).append(": ").append(itp.getMessage()).append('\n');
			
			StackTraceElementProxy[] stepArr = itp.getStackTraceElementProxyArray();
			String stackTraceClassName;
			if(stepArr!=null) {
				for(StackTraceElementProxy step: stepArr) {
					stackTraceClassName = step.getStackTraceElement().getClassName();
					
					for(int i = 0; i< topLevelPackages.length; i++) {
					    
    					if(stackTraceClassName.startsWith(topLevelPackages[i])) {
    						if(!(stackTraceClassName.contains("$$FastClassBySpringCGLIB$$") ||								
    								stackTraceClassName.contains("$$EnhancerBySpringCGLIB$$") ||
    								stackTraceClassName.contains("ServletFilters$1") ||
    								stackTraceClassName.contains("StaticCorsFilter") 								
    								)) {
    							strb.append(step.getSTEAsString()).append('\n');
    						}
    						
    						//we processed the top-level package of interest, can exit now
    						break;
    					}
    					
					}
				}
			}
		}
		
		return strb.toString();
	}

}
