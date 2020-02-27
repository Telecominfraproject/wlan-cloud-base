package com.whizcontrol.core.server.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.whizcontrol.core.server.async.example.AsyncCallerExample;
import com.whizcontrol.core.server.async.example.AsyncExample;

/**
 * @author dtoptygin
 * 
 * <br>Any @Bean or @Component or @Configuration can have methods annotated with @Async - if they are meant to be executed asynchronously.
 * <li>There can be multiple @Bean-s defined for ThreadPoolTaskExecutor - with different names.
 * <li>If a method is marked with just @Async, then it will be executed by a @Primary ThreadPoolTaskExecutor @Bean.
 * <li>If a method is marked with @Async("executorName"), then it will be executed by ThreadPoolTaskExecutor @Bean with name "executorName".
 * 
 * 
 * @see AsyncExample 
 * @see AsyncCallerExample
 * @see org.springframework.scheduling.annotation.Async
 *
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncConfiguration.class);
    
    @Autowired private Environment environment;
    
    @Monitor(name="TotalAsyncThreads", type=DataSourceType.GAUGE)
    private static final AtomicInteger totalAsyncThreads = new AtomicInteger(0);

    @Monitor(name="TotalTasksExecuted", type=DataSourceType.COUNTER)
    private static final AtomicInteger totalTasksExecuted = new AtomicInteger(0);
    
    @Monitor(name = "TotalTasksRejected", type = DataSourceType.COUNTER)
    private static final AtomicInteger totalTasksRejected = new AtomicInteger(0);

    @Monitor(name="TasksInTheQueue", type=DataSourceType.GAUGE)
    private static final AtomicInteger tasksInTheQueue = new AtomicInteger(0);
    
    static interface RunnableBlockingQueueInSpringClassloaderInterface extends BlockingQueue<Runnable> {
    }

    static class RunnableBlockingQueueInSpringClassloader implements RunnableBlockingQueueInSpringClassloaderInterface{
        private BlockingQueue<Runnable> delegate;
        
        public RunnableBlockingQueueInSpringClassloader(BlockingQueue<Runnable> q) {
            this.delegate = q;
        }

        public int size() {
            return delegate.size();
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        public boolean add(Runnable e) {
            boolean ret = delegate.add(e);
            if(ret){
                tasksInTheQueue.incrementAndGet();
            }
            return ret;
        }

        public Iterator<Runnable> iterator() {
            return delegate.iterator();
        }

        public Runnable remove() {
            Runnable ret = delegate.remove(); 
            tasksInTheQueue.decrementAndGet();
            return ret;
        }

        public boolean offer(Runnable e) {
            boolean ret = delegate.offer(e);
            if(ret){
                tasksInTheQueue.incrementAndGet();
            }
            return ret;
        }

        public Runnable poll() {
            Runnable ret = delegate.poll();
            if(ret!=null){
                tasksInTheQueue.decrementAndGet();
            }
            return ret; 
        }

        public Object[] toArray() {
            return delegate.toArray();
        }

        public Runnable element() {
            return delegate.element();
        }

        public Runnable peek() {
            return delegate.peek();
        }

        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        public void put(Runnable e) throws InterruptedException {
            delegate.put(e);
            tasksInTheQueue.incrementAndGet();
        }

        public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
            boolean ret = delegate.offer(e, timeout, unit);
            if(ret){
                tasksInTheQueue.incrementAndGet();
            }
            return ret;
        }

        public Runnable take() throws InterruptedException {
            Runnable ret = delegate.take();
            tasksInTheQueue.decrementAndGet();
            return ret; 
        }

        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            Runnable ret = delegate.poll(timeout, unit);
            if(ret!=null){
                tasksInTheQueue.decrementAndGet();
            }
            return ret;             
        }

        public int remainingCapacity() {
            return delegate.remainingCapacity();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
//            boolean ret = delegate.remove(o);
//            
//            //dtop: situation where one remove call actually removes more than one element is not covered in here
//            if(ret){
//                tasksInTheQueue.decrementAndGet();
//            }
//            
//            return ret;
        }

        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        public int drainTo(Collection<? super Runnable> c) {
            int ret = delegate.drainTo(c);
            tasksInTheQueue.addAndGet(-ret);
            return ret;
        }

        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            int ret = delegate.drainTo(c, maxElements);
            tasksInTheQueue.addAndGet(-ret);
            return ret;
        }

        public boolean addAll(Collection<? extends Runnable> c) {
            int size = c.size();
            boolean ret = delegate.addAll(c);
            if(ret){
                tasksInTheQueue.addAndGet(size);
            }
            return ret;
        }

        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
            //dtop: do not know what to do in here, but this method is not going to be called by AsyncExecutor
            //return delegate.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
            //dtop: do not know what to do in here, but this method is not going to be called by AsyncExecutor
            //return delegate.retainAll(c);
        }

        public void clear() {
            delegate.clear();
            tasksInTheQueue.set(0);
        }

        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        public int hashCode() {
            return delegate.hashCode();
        }
    }
    
    static class RunnableWrapper implements Runnable{
        
        Runnable target;
        
        public RunnableWrapper(Runnable target) {
            this.target = target;
        }
        
        @Override
        public void run() {
            if(this.target!=null){
                target.run();
            }
            
            totalAsyncThreads.decrementAndGet();
        }
    }
    
    @Bean(name="asyncExecutor")
    @Primary
    @Override
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(){
            private static final long serialVersionUID = -8981191394871663109L;


            @Override
            public Thread createThread(Runnable runnable) {
                totalAsyncThreads.incrementAndGet();
                Thread ret = super.createThread(new RunnableWrapper(runnable));
                return ret;
            }
            @Override
            public void execute(Runnable task) {
                totalTasksExecuted.incrementAndGet();
                super.execute(task);
            }
            
            @Override
            public Future<?> submit(Runnable task) {
                try {
                    Future<?> result = super.submit(task);
                    totalTasksExecuted.incrementAndGet();
                    return result;
                } catch (TaskRejectedException exp) {
                    totalTasksRejected.incrementAndGet();
                    throw exp;
                }
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                try {
                    Future<T> result = super.submit(task);
                    totalTasksExecuted.incrementAndGet();
                    return result;
                } catch (TaskRejectedException exp) {
                    totalTasksRejected.incrementAndGet();
                    throw exp;
                }
            }

            @Override
            public ListenableFuture<?> submitListenable(Runnable task) {
                try {
                    ListenableFuture<?> result = super.submitListenable(task);
                    totalTasksExecuted.incrementAndGet();
                    return result;
                } catch (TaskRejectedException exp) {
                    totalTasksRejected.incrementAndGet();
                    throw exp;
                }
            }

            @Override
            public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
                try {
                    ListenableFuture<T> result = super.submitListenable(task);
                    totalTasksExecuted.incrementAndGet();
                    return result;
                } catch (TaskRejectedException exp) {
                    totalTasksRejected.incrementAndGet();
                    throw exp;
                }
            }

            
            @Override
            protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
                RunnableBlockingQueueInSpringClassloader originalQueue = new RunnableBlockingQueueInSpringClassloader(super.createQueue(queueCapacity));
                return originalQueue;
                
//                //wrap and register this object to produce JMX metrics
//                @SuppressWarnings("unchecked")
//                RunnableBlockingQueueInSpringClassloaderInterface ret = TimedInterface.newProxy(RunnableBlockingQueueInSpringClassloaderInterface.class, originalQueue, "AsyncExecutorQueue");
//                DefaultMonitorRegistry.getInstance().register((CompositeMonitor)ret);
//                
//                return ret;
            }
            
        };
        
        executor.setCorePoolSize(Integer.parseInt(environment.getProperty("whizcontrol.AsyncExecutor.CorePoolSize", "7")));
        executor.setMaxPoolSize(Integer.parseInt(environment.getProperty("whizcontrol.AsyncExecutor.MaxPoolSize", "42")));
        int queueCapacity = Integer.parseInt(environment.getProperty("whizcontrol.AsyncExecutor.QueueCapacity", "11"));
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.setDaemon(true);
        
        final RejectedExecutionHandler defaultRejectionHandler = new ThreadPoolExecutor.AbortPolicy();
        
        RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                totalTasksRejected.incrementAndGet();
                defaultRejectionHandler.rejectedExecution(r, executor);
            }
        };
        
        executor.setRejectedExecutionHandler(rejectedExecutionHandler );

        LOG.info("Configuring {} with CorePoolSize={} MaxPoolSize={} QueueCapacity={}", 
                executor.getThreadNamePrefix(), executor.getCorePoolSize(), executor.getMaxPoolSize(), queueCapacity);
        
        Monitors.registerObject(AsyncConfiguration.class.getSimpleName(), this);
        
        return executor;
    }

    @Bean(name="asyncUncaughtExceptionHandler")
    @Primary
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new UncaughtExceptionHandler();
    }
}
