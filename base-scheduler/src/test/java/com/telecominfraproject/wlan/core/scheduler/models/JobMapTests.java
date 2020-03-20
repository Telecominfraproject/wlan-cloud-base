package com.telecominfraproject.wlan.core.scheduler.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.telecominfraproject.wlan.core.model.scheduler.ImmutableTimeWindow;
import com.telecominfraproject.wlan.core.scheduler.models.JobMap;
import com.telecominfraproject.wlan.core.scheduler.models.JobSchedule;
import com.telecominfraproject.wlan.core.scheduler.models.ScheduledJob;
import com.telecominfraproject.wlan.core.scheduler.models.JobMap.JobDetails;

public class JobMapTests {

    public static class TestJob implements ScheduledJob<Long> {

        private final Long jobId;

        public TestJob(long jobId) {
            this.jobId = jobId;
        }

        @Override
        public void runJob(Long id, long startTime, final ImmutableTimeWindow timeWindows) {
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
    }

    public static interface TimeWindowTestFactory {
        /**
         * create a time window
         * 
         * @param jobId
         * @return
         */
        ImmutableTimeWindow create(long jobId);

        /**
         * verify the time window
         * 
         * @param jobId
         * @param value
         */
        void verify(long jobId, final ImmutableTimeWindow value);

        /**
         * verify the jobId order
         * 
         * @param lastJobId
         * @param jobId
         */
        void verify(Long lastJobId, Long jobId);
    };

    private static final int MAX_JOB = 10;

    private JobMap<Long> jobMap;
    private final AtomicLong jobId = new AtomicLong(10L);

    @Before
    public void setup() {
        this.jobMap = new JobMap<>();
    }

    @After
    public void finish() {
        this.jobMap = null;
    }

    @Test
    public void testScheduleOrdered() {
        // same order w.r.t jobId, same duration
        TimeWindowTestFactory factory = new TimeWindowTestFactory() {

            @Override
            public void verify(long jobId, ImmutableTimeWindow value) {
                assertNotNull(value);
                assertEquals(jobId, value.getBeginTime());
                assertEquals(jobId + TimeUnit.MINUTES.toMillis(1), value.getEndTime());
            }

            @Override
            public ImmutableTimeWindow create(long jobId) {
                return new ImmutableTimeWindow(jobId, jobId + TimeUnit.MINUTES.toMillis(1));
            }

            @Override
            public void verify(Long lastJobId, Long jobId) {
                // increasing order
                if (null != lastJobId) {
                    assertTrue(lastJobId < jobId);
                }
            }
        };
        testSchelder(factory);
    }

    @Test
    public void testScheduleReverse() {
        // reverse order w.r.t jobId
        TimeWindowTestFactory factory = new TimeWindowTestFactory() {

            @Override
            public void verify(long jobId, ImmutableTimeWindow value) {
                assertNotNull(value);
                assertEquals(0 - TimeUnit.MINUTES.toMillis(jobId), value.getBeginTime());
                assertEquals(0L, value.getEndTime());
            }

            @Override
            public ImmutableTimeWindow create(long jobId) {
                return new ImmutableTimeWindow(0 - TimeUnit.MINUTES.toMillis(jobId), 0);
            }

            @Override
            public void verify(Long lastJobId, Long jobId) {
                // increasing order
                if (null != lastJobId) {
                    assertTrue(lastJobId > jobId);
                }
            }
        };
        testSchelder(factory);
    }

    @Test
    public void testScheduleExpanding() {
        // all start at same time, with expanding duration
        TimeWindowTestFactory factory = new TimeWindowTestFactory() {

            @Override
            public void verify(long jobId, ImmutableTimeWindow value) {
                assertNotNull(value);
                assertEquals(0, value.getBeginTime());
                assertEquals(TimeUnit.MINUTES.toMillis(jobId), value.getEndTime());
            }

            @Override
            public ImmutableTimeWindow create(long jobId) {
                return new ImmutableTimeWindow(0, TimeUnit.MINUTES.toMillis(jobId));
            }

            @Override
            public void verify(Long lastJobId, Long jobId) {
                // increasing order
                if (null != lastJobId) {
                    assertTrue(lastJobId < jobId);
                }
            }
        };
        testSchelder(factory);
    }

    private void testSchelder(TimeWindowTestFactory factory) {
        SortedSet<Long> jobIds = createJobs(MAX_JOB, factory);
        assertEquals(MAX_JOB, this.jobMap.getSize());

        Long lastJobId = null;
        for (int i = 0; i < MAX_JOB + 1; ++i) {
            JobSchedule<Long> timer = this.jobMap.getFirstTimer();
            if (jobIds.isEmpty()) {
                assertNull(timer);
                break;
            }
            assertNotNull(timer);
            JobDetails<Long> details = this.jobMap.removeJob(timer);
            assertNotNull(details);
            JobSchedule<Long> schedule = details.getSchedule();
            assertNotNull(schedule);
            assertNotNull(schedule.getJobId());
            factory.verify(lastJobId, schedule.getJobId());
            lastJobId = schedule.getJobId();
            assertTrue(jobIds.remove(lastJobId));
            ImmutableTimeWindow timeWindows = schedule.getTimeWindows();
            factory.verify(lastJobId, timeWindows);
            ScheduledJob<Long> job = details.getJob();
            TestJob testJob = TestJob.class.cast(job);
            assertNotNull(testJob);
            assertEquals(lastJobId, testJob.getJobId());
            assertEquals(lastJobId, schedule.getJobId());
        }
        assertEquals(0, this.jobMap.getSize());
        assertTrue(jobIds.isEmpty());
    }

    private SortedSet<Long> createJobs(int maxJob, TimeWindowTestFactory factory) {
        SortedSet<Long> result = new TreeSet<>();
        for (int i = 0; i < maxJob; ++i) {
            long count = jobId.getAndIncrement();
            JobSchedule<Long> id = new JobSchedule<Long>(count, factory.create(count));
            ScheduledJob<Long> job = new TestJob(count);
            boolean add = this.jobMap.addJob(id, job);
            assertTrue(add);

            add = this.jobMap.addJob(id, job);
            assertFalse(add);
            result.add(count);
        }
        assertEquals(result.size(), maxJob);
        return result;
    }

}
