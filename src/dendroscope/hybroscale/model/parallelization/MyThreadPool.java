/*
 *   MyThreadPool.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.*;

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
