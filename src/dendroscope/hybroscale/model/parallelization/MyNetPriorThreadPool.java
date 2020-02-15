/*
 *   MyNetPriorThreadPool.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dendroscope.hybroscale.model.parallelization;

import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.*;


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