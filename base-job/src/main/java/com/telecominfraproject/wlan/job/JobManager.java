package com.telecominfraproject.wlan.job;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.json.GenericResponse;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author ekeddy
 *
 */
@Component
public class JobManager {
    private static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

    /**
     * How long we wait for a job in the job queue
     */
    protected static final long JOB_QUEUE_POLL_TIME_MS = 1000;

    private final int queueSize = Integer.getInteger("tip.wlan.JobManager.queueSize", 10000);
    /**
     *  job queue
     */
    private final BlockingQueue<Runnable> jobQueue = new LinkedBlockingDeque<>(queueSize);

    private Thread jobManagerThread;

    @PostConstruct
    public void startupAuditor() {
        // start up the thread
        jobManagerThread = new Thread(new Runnable() {
            private boolean isRunning = true;

            @Override
            public void run() {
                LOG.info("Job Manager Started");
                while (isRunning) {
                    try {
                        Runnable job = jobQueue.poll(JOB_QUEUE_POLL_TIME_MS, TimeUnit.MILLISECONDS);
                        if (null != job) {
                            job.run();
                        }
                    } catch (RuntimeException e) {
                        LOG.error("Failed to run job", e);
                    } catch (InterruptedException e) {
                        LOG.debug("Job queue poll interrupted");
                        Thread.currentThread().interrupt();
                    }
                }
                LOG.info("Job Manager Stopped");
            }
        }, "JobManagerThread");
        jobManagerThread.setDaemon(true);
        jobManagerThread.start();
    }

    /**
     * Submit a job to the queue
     * @param job
     */
    public void submitJob(Runnable job) {
        if(!jobQueue.offer(job)){
            throw new GenericErrorException("Job Manager queue is over capacity ("+queueSize+")");
        }        
    }

    /**
     * Submit a job to the queue
     * @param job
     */
    public GenericResponse submitNamedJob(NamedJob job) {
        GenericResponse result = new GenericResponse();
        try {
            submitJob(job);
            result.setSuccess(true);
            result.setMessage(job.getJobName()+"scheduled");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Failed to schedule "+job.getJobName()+": " + e.getLocalizedMessage());
        }
        return result;
    }

}
