/**
 * 
 */
package com.whizcontrol.core.model.scheduler;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This schedule setting will never return any TimeWindows.
 * 
 * @author yongli
 *
 */
public class EmptySchedule extends ScheduleSetting {
    private static final long serialVersionUID = 5265706275298117395L;

    private static final Logger LOG = LoggerFactory.getLogger(EmptySchedule.class);

    public EmptySchedule() {
        super(null);
    }

    protected EmptySchedule(Duration testDuration) {
        this();
    }

    @Override
    public EmptySchedule clone() {
        return (EmptySchedule) super.clone();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.whizcontrol.scheduler.models.ScheduleSetting#getNextWindows(long)
     */
    @Override
    public ImmutableTimeWindow getNextWindows(long currentTime) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.whizcontrol.scheduler.models.ScheduleSetting#getLogger()
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
