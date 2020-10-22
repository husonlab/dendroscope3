/*
 *   TerminalManager.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.terminals;

import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class TerminalManager extends Thread {

    private MyPhyloTree tOne, tTwo;
    private Vector<String> taxaOrdering;
    private int labelIndex = 0;
    private int start = 0, stop = Integer.MAX_VALUE;

    private TerminalAlg_Sparse tA;
    private boolean isStopped = false;

    private MyNetPriorThreadPool myThreadPool;
    private Vector<Future<?>> futures = new Vector<Future<?>>();
    private int[] resArray;
    private int result = -1;
    private Iterator<TerminalThread> threadIterator;
    private Vector<TerminalThread> terminalThreads = new Vector<TerminalThread>();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public TerminalManager(MyPhyloTree tOne, MyPhyloTree tTwo, Vector<String> taxaOrdering) {
        this.tOne = tOne;
        this.tTwo = tTwo;
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
    }

    public TerminalManager(MyPhyloTree tOne, MyPhyloTree tTwo, Vector<String> taxaOrdering, int lowerBound,
                           int upperBound) {
        this.tOne = tOne;
        this.tTwo = tTwo;
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        this.start = lowerBound;
        this.stop = upperBound;
    }

    public TerminalManager(MyPhyloTree tOne, MyPhyloTree tTwo, Vector<String> taxaOrdering,
                           MyNetPriorThreadPool myThreadPool, int lowerBound, Integer upperBound) {
        this.tOne = tOne;
        this.tTwo = tTwo;
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        this.myThreadPool = myThreadPool;
        this.start = lowerBound;
        this.stop = upperBound == null ? Integer.MAX_VALUE : upperBound;
    }

    public TerminalManager(MyPhyloTree tOne, MyPhyloTree tTwo, Vector<String> taxaOrdering,
                           MyNetPriorThreadPool myThreadPool, int lowerBound) {
        this.tOne = tOne;
        this.tTwo = tTwo;
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        this.myThreadPool = myThreadPool;
        this.start = lowerBound;
    }

    public void run() {

        MyPhyloTree t1 = new MyPhyloTree(tOne);
        MyPhyloTree t2 = new MyPhyloTree(tTwo);
        int maxR = stop < taxaOrdering.size() ? stop : taxaOrdering.size();

        if (start < stop) {

            clearInnerLabels(t1);
            clearInnerLabels(t2);

            initLabelIndex(t1);
            HashMap<String, Integer> taxaToInt = new HashMap<String, Integer>();
            initLeafLabels(t1, taxaToInt);
            initLeafLabels(t2, taxaToInt);
            taxaOrdering = parseTaxa(t1);
            initSets(t1);
            initSets(t2);

            resArray = new int[maxR];
            for (int i = 0; i < resArray.length; i++)
                resArray[i] = -1;
            for (int i = 0; i < start; i++)
                resArray[i] = 0;

            isStopped = false;
            if (myThreadPool != null)
                new StoppingThread(10).start();

            ConcurrentHashMap<BitSet, Integer> prevFailedCutSets = new ConcurrentHashMap<BitSet, Integer>();
            int lowerBound = 0;
            for (int r = start; r < maxR; r++) {
                tA = new TerminalAlg_Sparse(t1, t2, r, taxaOrdering);

                for (BitSet cutSet : prevFailedCutSets.keySet())
                    tA.addFailedCutSet(cutSet, prevFailedCutSets.get(cutSet));

                if (tA.run()) {
                    result = r;
                    break;
                }

                if (r == maxR - 1)
                    result = Integer.MAX_VALUE;

                prevFailedCutSets = tA.getFailedCutSets();
                resArray[r] = 0;
                if (isStopped) {
                    resArray[r] = -1;
                    lowerBound = r;
                    break;
                }

            }

            // System.out.println("Result: " + result + " | " + lowerBound + " "
            // + maxR);

            if (result == -1) {

                isStopped = false;
                int threadPriority = 5;
                for (int r = lowerBound; r < maxR; r++) {
                    TerminalThread t = new TerminalThread(new TerminalAlg_Sparse(t1, t2, r, taxaOrdering));
                    t.addFailedCutSets(prevFailedCutSets);
                    t.setPriority(threadPriority);
                    terminalThreads.add(t);
                }

                threadIterator = terminalThreads.iterator();
                for (int i = 0; i < myThreadPool.getCorePoolSize(); i++) {
                    if (threadIterator.hasNext())
                        futures.add(myThreadPool.submit(threadIterator.next()));
                }

                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (Future<?> f : futures) {
                    if (f != null)
                        f.cancel(true);
                }

            }

            terminalThreads = null;
            prevFailedCutSets = null;

        } else
            result = Integer.MAX_VALUE;

    }

    private void initSets(MyPhyloTree t) {
        for (MyNode v : t.getNodes()) {
            BitSet b = new BitSet(taxaOrdering.size());
            v.setInfo(b);
            if (v.getOutDegree() == 0)
                b.set(taxaOrdering.indexOf(v.getLabel()));
        }
    }

    private void initLabelIndex(MyPhyloTree t1) {
        for (MyNode v : t1.getLeaves()) {
            try {
                int i = Integer.parseInt(v.getLabel());
                labelIndex = labelIndex < i ? i : labelIndex;
            } catch (Exception e) {
            }
        }
    }

    private Vector<String> parseTaxa(MyPhyloTree t) {
        Vector<String> taxaOrdering = new Vector<String>();
        for (MyNode v : t.getLeaves())
            taxaOrdering.add(v.getLabel());
        Collections.sort(taxaOrdering);
        return taxaOrdering;
    }

    private void initLeafLabels(MyPhyloTree t, HashMap<String, Integer> taxaToInt) {
        for (MyNode v : t.getLeaves()) {
            if (taxaToInt.containsKey(v.getLabel()))
                v.setLabel(taxaToInt.get(v.getLabel()) + "");
            else {
                taxaToInt.put(v.getLabel(), labelIndex);
                v.setLabel(labelIndex + "");
                labelIndex++;
            }
        }
    }

    private void clearInnerLabels(MyPhyloTree t) {
        for (MyNode v : t.getNodes()) {
            if (v.getOutDegree() != 0)
                v.setLabel("");
        }
    }

    public int getResult() {
        return result;
    }

    public class TerminalThread extends Thread implements Runnable {

        private TerminalAlg_Sparse tA;

        public TerminalThread(TerminalAlg_Sparse tA) {
            this.tA = tA;
        }

        public void run() {

            int res = tA.run() ? 1 : 0;
            int curRes = checkResultArray(tA.getR(), res);

            if (curRes != -1) {
                result = curRes;
                stopThreads();
            }

            for (TerminalThread t : terminalThreads) {
                if (t.getAlgorithm() != null && tA.getR() < t.getAlgorithm().getR())
                    t.addFailedCutSets(tA.getFailedCutSets());
            }

            startNextThread(res);

            tA.freeMemory();
            tA = null;

        }

        private void addFailedCutSets(ConcurrentHashMap<BitSet, Integer> failedCutSets) {
            for (BitSet cutSet : failedCutSets.keySet()) {
                if (tA != null)
                    tA.addFailedCutSet(cutSet, failedCutSets.get(cutSet));
            }
        }

        public void stopExecution() {
            if (tA != null)
                tA.stopExecution();
        }

        public TerminalAlg_Sparse getAlgorithm() {
            return tA;
        }

    }

    private synchronized void startNextThread(int res) {
        if (res == 0 && threadIterator.hasNext()) {
            TerminalThread thread = threadIterator.next();
            myThreadPool.submit(thread);
        } else if (res == 0 && !threadIterator.hasNext() && myThreadPool.getActiveCount() == 1) {
            result = Integer.MAX_VALUE;
            stopThreads();
        }
    }

    private synchronized int checkResultArray(int pos, int val) {

        resArray[pos] = val;

        // for (int i = 0; i < resArray.length; i++)
        // System.out.print(resArray[i]);
        // System.out.println();

        for (int i = 0; i < resArray.length; i++) {
            if (resArray[i] == -1)
                return -1;
            else if (resArray[i] == 1)
                return i;
        }
        return -1;
    }

    private synchronized void stopThreads() {
        Vector<Runnable> threads = new Vector<Runnable>();
        for (TerminalThread t : terminalThreads)
            threads.add(t);
        for (TerminalThread t : terminalThreads) {
            t.interrupt();
            t.stopExecution();
        }
        countDownLatch.countDown();
    }

    public class StoppingThread extends Thread {

        private int sleepingTime;

        public StoppingThread(int sleepingTime) {
            this.sleepingTime = sleepingTime;
        }

        public void run() {
            try {
                sleep(sleepingTime * 1000);
                isStopped = true;
                if (tA != null)
                    tA.stopExecution();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
