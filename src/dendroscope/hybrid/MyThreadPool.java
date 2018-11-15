/**
 * MyThreadPool.java 
 * Copyright (C) 2018 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package dendroscope.hybrid;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.*;

public class MyThreadPool {

    private final Hashtable<Runnable, Future<?>> threadToFuture = new Hashtable<>();
    private final int poolSize;
    private final int maxPoolSize;
    private long keepAliveTime = Long.MAX_VALUE;
    private final ThreadPoolExecutor threadPool;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public MyThreadPool() {
        poolSize = setPoolSize();
        maxPoolSize = setPoolSize();
        threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, queue);
    }

    public Future<?> runTask(Runnable task) {
        Future<?> f = (threadPool.submit(task));
        threadToFuture.put(task, f);
        return f;
    }

    public void removeThreads(Vector<ForestThread> threads) {

        for (ForestThread t : threads) {
            Future<?> f = threadToFuture.get(t);
            if (f != null)
                f.cancel(true);
        }
        threadPool.purge();

        for (ForestThread t : threads)
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
        keepAliveTime = 0;
    }

    public void setSize(int size) {
        threadPool.setCorePoolSize(size);
        threadPool.setMaximumPoolSize(size);
    }

    private int setPoolSize() {

        if (Runtime.getRuntime().availableProcessors() - 1 == 0)
            return 1;
        else
            return Runtime.getRuntime().availableProcessors() - 1;

        // return 1;
    }


}
