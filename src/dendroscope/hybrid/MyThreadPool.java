/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
