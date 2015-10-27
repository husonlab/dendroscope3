package dendroscope.hybroscale.model.parallelization;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyThreadPool {

	private Hashtable<Runnable, Future<?>> threadToFuture = new Hashtable<Runnable, Future<?>>();
	private int maxPoolSize;
	private ThreadPoolExecutor threadPool;

	public MyThreadPool() {
		maxPoolSize = setPoolSize();
		threadPool = new ThreadPoolExecutor(maxPoolSize, maxPoolSize, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	public Future<?> runTask(Runnable task) {
		Future<?> f = null;
		try {
			f = (threadPool.submit(task));
		} catch (RejectedExecutionException e) {

		}
		if (f != null)
			threadToFuture.put(task, f);
		return f;
	}

	public void removeThreads(Vector<Runnable> threads) {
		for (Runnable t : threads) {
			Future<?> f = threadToFuture.get(t);
			if (f != null)
				f.cancel(true);
		}
		threadPool.purge();
		for (Runnable t : threads)
			threadPool.remove(t);
	}

	public void shutDown() {
		threadPool.shutdownNow();
		for (Future<?> f : threadToFuture.values()) {
			if (f != null)
				f.cancel(true);
		}
		threadPool.purge();
		for (Runnable r : threadPool.getQueue())
			threadPool.remove(r);
		threadPool.shutdownNow();
	}

	public void removeThread(Thread t) {
		threadToFuture.remove(t);
	}

	public void setSize(int size) {
		threadPool.setCorePoolSize(size);
		threadPool.setMaximumPoolSize(size);
	}

	public ThreadPoolExecutor getPool() {
		return threadPool;
	}

	private int setPoolSize() {
		if (Runtime.getRuntime().availableProcessors() - 1 == 0)
			return 1;
		else
			return Runtime.getRuntime().availableProcessors() - 1;
	}

	public String getPoolInfo() {
		return "" + threadPool.getQueue().size();
	}

	public boolean isShutdown() {
		return threadPool.isShutdown();
	}

}
