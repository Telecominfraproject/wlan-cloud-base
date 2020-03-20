package com.telecominfraproject.wlan.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.EpochTimeWindow;
import com.telecominfraproject.wlan.core.model.scheduler.ImmutableTimeWindow;
import com.telecominfraproject.wlan.core.scheduler.BaseScheduler;
import com.telecominfraproject.wlan.core.scheduler.models.JobSchedule;
import com.telecominfraproject.wlan.core.scheduler.models.ScheduleRateThrottle;
import com.telecominfraproject.wlan.core.scheduler.models.ScheduledJob;
import com.telecominfraproject.wlan.core.scheduler.models.JobMap.JobDetails;

public class SchedulerTests {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerTests.class);

    public static interface TimeWindowTestFactory<I> {
        /**
         * create a time window
         * 
         * @param jobId
         * @return
         */
        ImmutableTimeWindow create(I jobId);

        Long getTestEnd();

        void clearResult();

        /**
         * Set up total test and test interval
         * 
         * @param now
         * @param total
         * @param interval
         */
        void setTestInterval(long now, long total, long interval);

        /**
         * verify the jobDetails
         * 
         * @param jobDetails
         */
        void verify(JobDetails<Long> jobDetails);

        void verifyTotal();
    };

    public static class TestScheduler extends BaseScheduler<Long> {
        private TimeWindowTestFactory<Long> factory;

        public TestScheduler(TimeWindowTestFactory<Long> factory) {
            // half second warm up and 1.1 / second rate
            super(LOG, new ScheduleRateThrottle(1.1, 500, TimeUnit.MICROSECONDS));
            this.factory = factory;
        }

        @Override
        protected void enqueuJob(JobDetails<Long> jobDetails) {
            LOG.debug("enqueuJob {}", jobDetails);
            assertNotNull(jobDetails);
            factory.verify(jobDetails);
            ScheduledJob<Long> job = jobDetails.getJob();
            JobSchedule<Long> schedule = jobDetails.getSchedule();
            job.runJob(schedule.getJobId(), System.currentTimeMillis(), schedule.getTimeWindows());
        }

        @Override
        protected void emptyJobQueue() {
        }

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }
    }

    public static class TestJob implements ScheduledJob<Long> {
        private final Long jobId;

        public TestJob(long jobId) {
            this.jobId = jobId;
        }

        @Override
        public void runJob(Long id, long startTime, final ImmutableTimeWindow timeWindows) {
            assertNotNull(id);
            assertEquals(jobId, id);
            assertNotNull(timeWindows);
            assertTrue(timeWindows.isWithin(System.currentTimeMillis()));
        }

        @Override
        public void cancel(Long id, long startTime, ImmutableTimeWindow timeWindows) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        public Long getJobId() {
            return jobId;
        }

        @Override
        public String toString() {
            return BaseJsonModel.toJsonString(this);
        }
    }

    private static final int MAX_LOOP = 10;

    @Ignore("DTOP: remove this ignore")
    @Test
    public void testBasic() {
        /**
         * Create a reverse order test factory
         */
        TimeWindowTestFactory<Long> factory = new TimeWindowTestFactory<Long>() {
            private Long testEnd;
            private Long total;
            private Long interval;
            private Long lastJobId;
            private Long verified;

            @Override
            public void setTestInterval(long now, long total, long interval) {
                this.testEnd = now + (total + 1) * 2 * interval;
                this.total = total;
                this.interval = interval;
                this.lastJobId = null;
                this.verified = 0L;
            }

            @Override
            public ImmutableTimeWindow create(Long jobId) {
                assertNotNull(testEnd);
                assertTrue(jobId < total);
                long end = testEnd - jobId * 2 * interval;
                long begin = end - interval;
                return new ImmutableTimeWindow(begin, end);
            }

            private void verify(Long jobId, ImmutableTimeWindow value) {
                ImmutableTimeWindow expected = create(jobId);
                assertEquals(0, EpochTimeWindow.compare(expected, value));
            }

            private void verify(Long jobId) {
                assertNotNull(jobId);
                if (null != lastJobId) {
                    // ensure reverse order
                    assertTrue(jobId < lastJobId);
                }
            }

            @Override
            public void verify(JobDetails<Long> jobDetails) {
                assertNotNull(jobDetails);
                JobSchedule<Long> schedule = jobDetails.getSchedule();
                verify(schedule.getJobId(), schedule.getTimeWindows());
                verify(schedule.getJobId());
                ScheduledJob<Long> job = jobDetails.getJob();
                assertNotNull(job);
                ++verified;
                // signal we've all done
                if (this.verified.equals(this.total)) {
                    synchronized (this) {
                        this.notifyAll();
                    }
                }
            }

            @Override
            public void clearResult() {
                this.lastJobId = null;
                this.verified = 0L;
            }

            @Override
            public Long getTestEnd() {
                return this.testEnd;
            }

            @Override
            public void verifyTotal() {
                assertEquals(total, verified);
            }
        };

        TestScheduler scheduler = new TestScheduler(factory);
        boolean start = scheduler.startScheduler(TimeUnit.SECONDS.toMillis(10));
        assertTrue(start);
        factory.setTestInterval(System.currentTimeMillis(), MAX_LOOP, TimeUnit.SECONDS.toMillis(1));
        factory.clearResult();

        for (int i = 0; i < MAX_LOOP; ++i) {
            long jobId = i;
            JobSchedule<Long> schedule = new JobSchedule<Long>(jobId, factory.create(jobId));
            ScheduledJob<Long> job = new TestJob(jobId);
            scheduler.submitJob(schedule, job);
        }

        Long testEndTime = factory.getTestEnd();
        assertNotNull(testEndTime);
        LOG.debug("Test started at {}, last end time {}", Instant.now(), Instant.ofEpochMilli(testEndTime));
        synchronized (factory) {
            try {
                factory.wait(testEndTime - System.currentTimeMillis());
            } catch (InterruptedException e) {
            }
            factory.verifyTotal();
        }

        start = scheduler.shutdownScheduler(TimeUnit.SECONDS.toMillis(10));
        assertFalse(start);
    }

}
