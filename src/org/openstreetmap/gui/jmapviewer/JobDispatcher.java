// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;

/**
 * A generic class that processes a list of {@link Runnable} one-by-one using
 * one or more {@link Thread}-instances. The number of instances varies between
 * 1 and {@link #workerThreadMaxCount} (default: 8). If an instance is idle
 * more than {@link #workerThreadTimeout} seconds (default: 30), the instance
 * ends itself.
 *
 * @author Jan Peter Stotz
 */
public final class JobDispatcher {

    private static final JobDispatcher instance = new JobDispatcher();

    private static int workerThreadMaxCount = 8;

    /**
     * Specifies the time span in seconds that a worker thread waits for new
     * jobs to perform. If the time span has elapsed the worker thread
     * terminates itself. Only the first worker thread works differently, it
     * ignores the timeout and will never terminate itself.
     */
    private static int workerThreadTimeout = 30;

    /**
     * Type of queue, FIFO if <code>false</code>, LIFO if <code>true</code>
     */
    private boolean modeLIFO = false;

    /**
     * Total number of worker threads currently idle or active
     */
    private int workerThreadCount = 0;

    /**
     * Number of worker threads currently idle
     */
    private int workerThreadIdleCount = 0;

    /**
     * Just an id for identifying an worker thread instance
     */
    private int workerThreadId = 0;

    private final BlockingDeque<TileJob> jobQueue = new LinkedBlockingDeque<>();

    private JobDispatcher() {
        addWorkerThread().firstThread = true;
    }

    /**
     * @return the singelton instance of the {@link JobDispatcher}
     */
    public static JobDispatcher getInstance() {
        return instance;
    }

    /**
     * Removes all jobs from the queue that are currently not being processed.
     */
    public void cancelOutstandingJobs() {
        jobQueue.clear();
    }

    /**
     * Function to set the maximum number of workers for tile loading.
     * @param workers maximum number of workers
     */
    public static void setMaxWorkers(int workers) {
        workerThreadMaxCount = workers;
    }

    /**
     * Function to set the LIFO/FIFO mode for tile loading job.
     *
     * @param lifo <code>true</code> for LIFO mode, <code>false</code> for FIFO mode
     */
    public void setLIFO(boolean lifo) {
        modeLIFO = lifo;
    }

    /**
     * Adds a job to the queue.
     * Jobs for tiles already contained in the are ignored (using a <code>null</code> tile
     * prevents skipping).
     *
     * @param job the the job to be added
     */
    public void addJob(TileJob job) {
        try {
            if (job.getTile() != null) {
                for (TileJob oldJob : jobQueue) {
                    if (job.getTile().equals(oldJob.getTile())) {
                        return;
                    }
                }
            }
            jobQueue.put(job);
            synchronized (this) {
                if (workerThreadIdleCount == 0 && workerThreadCount < workerThreadMaxCount)
                    addWorkerThread();
            }
        } catch (InterruptedException e) {
            System.err.println("InterruptedException: " + e.getMessage());
        }
    }

    private JobThread addWorkerThread() {
        JobThread jobThread = new JobThread(++workerThreadId);
        synchronized (this) {
            workerThreadCount++;
        }
        jobThread.start();
        return jobThread;
    }

    public class JobThread extends Thread {

        private Runnable job;
        private boolean firstThread = false;

        /**
         * Constructs a new {@code JobThread}.
         * @param threadId Thread ID
         */
        public JobThread(int threadId) {
            super("OSMJobThread " + threadId);
            setDaemon(true);
            job = null;
        }

        @Override
        public void run() {
            executeJobs();
            synchronized (instance) {
                workerThreadCount--;
            }
        }

        protected void executeJobs() {
            while (!isInterrupted()) {
                try {
                    synchronized (instance) {
                        workerThreadIdleCount++;
                    }
                    if (modeLIFO) {
                        if (firstThread)
                            job = jobQueue.takeLast();
                        else
                            job = jobQueue.pollLast(workerThreadTimeout, TimeUnit.SECONDS);
                    } else {
                        if (firstThread)
                            job = jobQueue.take();
                        else
                            job = jobQueue.poll(workerThreadTimeout, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e1) {
                    return;
                } finally {
                    synchronized (instance) {
                        workerThreadIdleCount--;
                    }
                }
                if (job == null)
                    return;
                try {
                    job.run();
                    job = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
