/**
 * 
 */
package com.whizcontrol.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * @author ekeddy
 *
 */
public enum LEDColour {
	blue(1),
	green(2),
	red(3),
	yellow(4),
	purple(5),
	cyan(6),
	white(7),
	off(8),
	
	UNSUPPORTED(-1);
	
    @JsonCreator
    public static LEDColour getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, LEDColour.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(LEDColour value) {
        return (UNSUPPORTED.equals(value));
    }
    
	private final int id;
	LEDColour(int id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	

}
