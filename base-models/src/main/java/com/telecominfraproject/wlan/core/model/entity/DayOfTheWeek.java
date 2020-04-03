package com.telecominfraproject.wlan.core.model.entity;

import java.util.Calendar;
import java.util.Objects;

public enum DayOfTheWeek {
    SUNDAY("0"),
    MONDAY("1"),
    TUESDAY("2"),
    WEDNESDAY("3"),
    THURSDAY("4"),
    FRIDAY("5"),
    SATURDAY("6");
    
    private String code;
    
    private DayOfTheWeek(String code)
    {
        this.code = code;        
    }

    public static DayOfTheWeek fromCode(String code)
    {
        for(DayOfTheWeek day : DayOfTheWeek.values())
        {
            if(Objects.equals(day.code, code))
            {
                return day;
            }
        }
        
        throw new IllegalArgumentException("Invalid day of the week code: " + code);
    }
    
    public static DayOfTheWeek fromCalendar(Calendar cal)
    {
        int dayOfTheWeek = cal.get(Calendar.DAY_OF_WEEK);
        return fromCode(String.valueOf(dayOfTheWeek - 1));
    }
}
