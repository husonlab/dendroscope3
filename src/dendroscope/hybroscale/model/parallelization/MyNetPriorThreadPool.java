package dendroscope.hybroscale.model.parallelization;

import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MyNetPriorThreadPool extends ThreadPoolExecutor {

	private Vector<Future<?>> futures = new Vector<Future<?>>();
	private boolean isStopped = false;

	public MyNetPriorThreadPool() {
		super(1, 1, 60L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(11,
				new PriorityTaskComparator()), new ThreadPoolExecutor.DiscardPolicy());
		allowCoreThreadTimeOut(true);
	}

	public void setSize(int size) {
		setCorePoolSize(size);
		setMaximumPoolSize(size);
	}

	public void forceShutDown() {
		isStopped = true;
		for (Future<?> f : futures) {
			if (f != null) 
				f.cancel(true);
		}
		purge();
		for (Runnable r : getQueue())
			remove(r);
		shutdownNow();
	}
	
	public void stopCurrentExecution() {
		isStopped = true;
		for (Future<?> f : futures) {
			if (f != null) 
				f.cancel(true);
		}
		for (Runnable r : getQueue())
			remove(r);
		isStopped = false;
	}
	
	@Override
	public Future<?> submit(final Runnable task) {
		Future<?> f = super.submit(task);
		if(!isStopped)
			futures.add(f);
		return f;
	}


//	@Override
//	public Future<?> submit(final Runnable task) {
//		if (task == null)
//			throw new NullPointerException();
//		final RunnableFuture<Object> ftask = newTaskFor(task, null);
//		if (!isStopped) {
//			futures.add(ftask);
//			try {
//				execute(ftask);
//			} catch (Exception e) {
//				e.printStackTrace();
//				if (!(e instanceof RejectedExecutionException))
//					e.printStackTrace();
//			}
//		}
//		return ftask;
//	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
		return new PriorityTask<T>(0, runnable, value);
	}

	private static final class PriorityTask<T> extends FutureTask<T> implements Comparable<PriorityTask<T>> {

		private final int priority;

		public PriorityTask(final int priority, final Runnable runnable, final T result) {
			super(runnable, result);
			this.priority = priority;
		}

		@Override
		public int compareTo(final PriorityTask<T> o) {
			int p1 = priority;
			int p2 = o.priority;
			if (p1 < p2)
				return -1;
			else if (p1 > p2)
				return 1;
			return 0;
		}

	}

	private static class PriorityTaskComparator implements Comparator<Runnable> {
		@Override
		public int compare(final Runnable left, final Runnable right) {
			return ((PriorityTask) left).compareTo((PriorityTask) right);
		}
	}

	public void setStopped(boolean isStopped) {
		this.isStopped = isStopped;
	}
	
	public boolean isStopped() {
		return isStopped;
	}

}