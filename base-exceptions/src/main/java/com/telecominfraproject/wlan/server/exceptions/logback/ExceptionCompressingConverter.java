package com.telecominfraproject.wlan.server.exceptions.logback;

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
					if(stackTraceClassName.startsWith("com.telecominfraproject.wlan")) {
						if(!(stackTraceClassName.contains("$$FastClassBySpringCGLIB$$") ||								
								stackTraceClassName.contains("$$EnhancerBySpringCGLIB$$") ||
								stackTraceClassName.contains("ServletFilters$1") ||
								stackTraceClassName.contains("StaticCorsFilter") 								
								)) {
							strb.append(step.getSTEAsString()).append('\n');
						}
					}
				}
			}
		}
		
		return strb.toString();
	}

}
