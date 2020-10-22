/*
 *   CycleRefinement.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyGraph;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;

public class CycleRefinement {

    private EasyTree t1, t2;
    private Vector<String> taxaOrdering;
    private HashMap<EasyNode, BitSet> t1NodeToLeafSet, t2NodeToLeafSet;
    private HashMap<BitSet, EasyNode> leafSetToT1Node, leafSetToT2Node;

    private HashMap<MyNode, Object> graphNodeToInfo;
    private HashMap<MyNode, String> graphNodeToLabel;
    private HashMap<EasyNode, MyNode> nodeToGraphNode;
    private HashMap<EasyNode, Vector<MyNode>> t1NodeToGraphNode, t2NodeToGraphNode;
    private HashMap<MyNode, EasyNode> graphRootToT1Node, graphRootToT2Node;
    private HashMap<Integer, MyNode> levelToNode;
    private HashMap<MyNode, Integer> nodeToLevel;
    private MyNode start;
    private HashSet<MyNode> exitNodes;
    private MyGraph expCycleGraph;
    private int k;

    boolean debug = false;

    public CycleRefinement(EasyTree t1, EasyTree t2, Vector<String> taxaOrdering, HybridTree tree1, HybridTree tree2) {

        this.t1 = t1;
        this.t2 = t2;
        this.taxaOrdering = taxaOrdering;
        t1NodeToLeafSet = new HashMap<EasyNode, BitSet>();
        leafSetToT1Node = new HashMap<BitSet, EasyNode>();
        initTreeLeafLists(t1.getRoot(), t1NodeToLeafSet, leafSetToT1Node);
        t2NodeToLeafSet = new HashMap<EasyNode, BitSet>();
        leafSetToT2Node = new HashMap<BitSet, EasyNode>();
        initTreeLeafLists(t2.getRoot(), t2NodeToLeafSet, leafSetToT2Node);

    }

    public Vector<Vector<EasyTree>> run(Vector<EasyTree> forest, int k, boolean debug) {
        this.k = k;
        this.debug = debug;

        Vector<Vector<EasyTree>> refinedForestsSet = new Vector<Vector<EasyTree>>();
        Vector<Vector<EasyTree>> refinedForests = refineForest(forest);
        if (refinedForests != null)
            refinedForestsSet.addAll(refinedForests);

        return refinedForestsSet;
    }

    public Vector<Vector<EasyTree>> runRec(Vector<Vector<EasyTree>> forests, int k) {
        this.k = k;

        Vector<Vector<EasyTree>> refinedForestsSet = new Vector<Vector<EasyTree>>();
        for (Vector<EasyTree> forest : forests) {
            Vector<Vector<EasyTree>> refinedForests = refineForest(forest);
            if (refinedForests != null)
                refinedForestsSet.addAll(refinedForests);
        }

        return refinedForestsSet;
    }

    private boolean isPartOfCycle;

    public boolean checkTaxon(Vector<EasyTree> forest, EasyNode sib) {

        graphNodeToInfo = new HashMap<MyNode, Object>();
        graphNodeToLabel = new HashMap<MyNode, String>();
        nodeToGraphNode = new HashMap<EasyNode, MyNode>();
        t1NodeToGraphNode = new HashMap<EasyNode, Vector<MyNode>>();
        t2NodeToGraphNode = new HashMap<EasyNode, Vector<MyNode>>();
        graphRootToT1Node = new HashMap<MyNode, EasyNode>();
        graphRootToT2Node = new HashMap<MyNode, EasyNode>();
        levelToNode = new HashMap<Integer, MyNode>();
        nodeToLevel = new HashMap<MyNode, Integer>();

        expCycleGraph = new MyGraph();

        Vector<EasyNode> specialNodes = new Vector<EasyNode>();
        specialNodes.add(sib);
        boolean isRho = true;
        for (EasyTree comp : forest) {
            addCompToGraph(expCycleGraph, comp, isRho, specialNodes);
            isRho = false;
        }

        Vector<MyEdge> specialEdges = new Vector<MyEdge>();

        for (MyNode w : graphRootToT1Node.keySet()) {
            EasyNode wT1 = graphRootToT1Node.get(w);
            if (wT1.getInDegree() != 0 && w.getOutDegree() != 0) {
                EasyNode p = wT1.getParent();
                while (!t1NodeToGraphNode.containsKey(p))
                    p = p.getParent();
                Vector<MyNode> vNodes = t1NodeToGraphNode.get(p);
                for (MyNode v : vNodes) {
                    MyEdge e = expCycleGraph.newEdge(v, w);
                    e.setInfo(1);
                    expCycleGraph.setSpecial(e, true);
                    specialEdges.add(e);
                }
            }
        }

        for (MyNode w : graphRootToT2Node.keySet()) {
            EasyNode wT2 = graphRootToT2Node.get(w);
            if (wT2.getInDegree() != 0 && w.getOutDegree() != 0) {
                EasyNode p = wT2.getParent();
                while (!t2NodeToGraphNode.containsKey(p))
                    p = p.getParent();
                Vector<MyNode> vNodes = t2NodeToGraphNode.get(p);
                for (MyNode v : vNodes) {
                    MyEdge e = expCycleGraph.newEdge(v, w);
                    e.setInfo(2);
                    expCycleGraph.setSpecial(e, true);
                    specialEdges.add(e);
                }
            }
        }

        MyNode v = nodeToGraphNode.get(sib);

        isPartOfCycle = false;
        isPartOfCycle(v, v, expCycleGraph, new Vector<MyEdge>());

        return isPartOfCycle;
    }

    private void isPartOfCycle(MyNode v, MyNode start, MyGraph g, Vector<MyEdge> visited) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode w = e.getTarget();
            if (!visited.contains(e)) {
                visited.add(e);
                if (w.equals(start))
                    isPartOfCycle = true;
                else if (!isPartOfCycle)
                    isPartOfCycle(w, start, g, visited);
            }
        }
    }

    private Vector<Vector<EasyTree>> refineForest(Vector<EasyTree> forest) {

        graphNodeToInfo = new HashMap<MyNode, Object>();
        graphNodeToLabel = new HashMap<MyNode, String>();
        nodeToGraphNode = new HashMap<EasyNode, MyNode>();
        t1NodeToGraphNode = new HashMap<EasyNode, Vector<MyNode>>();
        t2NodeToGraphNode = new HashMap<EasyNode, Vector<MyNode>>();
        graphRootToT1Node = new HashMap<MyNode, EasyNode>();
        graphRootToT2Node = new HashMap<MyNode, EasyNode>();
        levelToNode = new HashMap<Integer, MyNode>();
        nodeToLevel = new HashMap<MyNode, Integer>();

        expCycleGraph = new MyGraph();

        boolean isRho = false;
        for (EasyTree comp : forest) {
            if (!isRho) {
                for (EasyNode leaf : comp.getLeaves()) {
                    if (leaf.getLabel().equals("rho")) {
                        isRho = true;
                        break;
                    }
                }
            }
            addCompToGraph(expCycleGraph, comp, isRho, new Vector<EasyNode>());
            isRho = false;
        }

        Vector<MyEdge> specialEdges = new Vector<MyEdge>();
        for (MyNode w : graphRootToT1Node.keySet()) {
            EasyNode wT1 = graphRootToT1Node.get(w);
            if (wT1.getInDegree() != 0 && w.getOutDegree() != 0) {
                EasyNode p = wT1.getParent();
                while (!t1NodeToGraphNode.containsKey(p))
                    p = p.getParent();
                Vector<MyNode> vNodes = t1NodeToGraphNode.get(p);
                for (MyNode v : vNodes) {
                    if (graphRootToT1Node.containsKey(v)) {
                        Iterator<MyEdge> it = v.outEdges().iterator();
                        while (it.hasNext()) {
                            MyEdge eOut = it.next();
                            if (!eOut.isSpecial()) {
                                MyEdge e = expCycleGraph.newEdge(eOut.getTarget(), w);
                                e.setInfo(1);
                                expCycleGraph.setSpecial(e, true);
                                specialEdges.add(e);
                                // System.out.println(e.getSource().getInfo()+" -1-> "+e.getTarget().getInfo());
                            }
                        }
                    } else {
                        MyEdge e = expCycleGraph.newEdge(v, w);
                        e.setInfo(1);
                        expCycleGraph.setSpecial(e, true);
                        specialEdges.add(e);
                        // System.out.println(e.getSource().getInfo()+" -1-> "+e.getTarget().getInfo());
                    }
                }
            }
        }

        for (MyNode w : graphRootToT2Node.keySet()) {
            EasyNode wT2 = graphRootToT2Node.get(w);
            if (wT2.getInDegree() != 0 && w.getOutDegree() != 0) {
                EasyNode p = wT2.getParent();
                while (!t2NodeToGraphNode.containsKey(p))
                    p = p.getParent();
                Vector<MyNode> vNodes = t2NodeToGraphNode.get(p);
                for (MyNode v : vNodes) {
                    if (graphRootToT2Node.containsKey(v)) {
                        Iterator<MyEdge> it = v.outEdges().iterator();
                        while (it.hasNext()) {
                            MyEdge eOut = it.next();
                            if (!eOut.isSpecial()) {
                                MyEdge e = expCycleGraph.newEdge(eOut.getTarget(), w);
                                e.setInfo(2);
                                expCycleGraph.setSpecial(e, true);
                                specialEdges.add(e);
                                // System.out.println(e.getSource().getInfo()+" -2-> "+e.getTarget().getInfo());
                            }
                        }
                    } else {
                        MyEdge e = expCycleGraph.newEdge(v, w);
                        e.setInfo(2);
                        expCycleGraph.setSpecial(e, true);
                        specialEdges.add(e);
                        // System.out.println(e.getSource().getInfo()+" -2-> "+e.getTarget().getInfo());
                    }
                }
            }
        }

        Vector<Vector<EasyTree>> cutForests = new Vector<Vector<EasyTree>>();
        Vector<MyNode> rootNodes = new Vector<MyNode>();
        for (MyNode rootNode : graphRootToT1Node.keySet())
            rootNodes.add(rootNode);

        return cutForest(cutForests, expCycleGraph, start, rootNodes, graphNodeToLabel);

    }

    private Vector<Vector<EasyTree>> cutForest(Vector<Vector<EasyTree>> cutForests, MyGraph g, MyNode startNode,
                                               Vector<MyNode> rootNodes, HashMap<MyNode, String> graphNodeToLabel2) {
        boolean hasCycle = hasCycle(g, startNode);

        if (!hasCycle) {

            Vector<EasyTree> forest = new Vector<EasyTree>();
            for (MyNode v : rootNodes) {
                EasyNode vE = new EasyNode(null, null, null);
                vE.setInfo(vE.getInfo());
                EasyTree eT = new EasyTree(vE);
                vE.setOwner(eT);
                if (v.getOutDegree() == 0 || (v.getFirstOutEdge()).isSpecial())
                    vE.setLabel(graphNodeToLabel2.get(v));
                parseComponent(g, v, vE, eT, graphNodeToLabel2, rootNodes);
                forest.add(eT);
            }

            if (forest.size() <= k) {

                Vector<EasyTree> acyclicOrder = new FastAcyclicCheck().run(forest, t1, t2, taxaOrdering, null, false,
                        true);

                if (acyclicOrder != null)
                    cutForests.add(acyclicOrder);

                return cutForests;
            }
        } else if (rootNodes.size() < k) {

            // System.out.println(t1.getPhyloTree()+";\n"+t2.getPhyloTree()+";");
            // System.out.println("Forest: ");
            // for(EasyTree c : forest)
            // System.out.println(c.getPhyloTree()+";");
            // System.out.println("#ExitNodes: "+exitNodes.size());

            for (MyNode exitNode : exitNodes) {

                Vector<MyNode> copyNodes = new Vector<MyNode>();
                Vector<MyNode> rootNodesCopy = new Vector<MyNode>();
                HashMap<MyNode, String> nodeToLabelCopy = new HashMap<MyNode, String>();
                MyGraph gCopy = copyGraph(g, startNode, exitNode, rootNodes, graphNodeToLabel2, copyNodes,
                        rootNodesCopy, nodeToLabelCopy);

                fixExitNode(gCopy, copyNodes.get(1), rootNodesCopy, startNode, graphNodeToLabel2);

                Vector<EasyTree> cutForest = new Vector<EasyTree>();

                for (MyNode v : rootNodesCopy) {
                    EasyNode vE = new EasyNode(null, null, null);
                    vE.setInfo(v.getInfo());
                    EasyTree eT = new EasyTree(vE);
                    vE.setOwner(eT);
                    if (v.getOutDegree() == 0 || (v.getFirstOutEdge()).isSpecial())
                        vE.setLabel(nodeToLabelCopy.get(v));
                    parseComponent(gCopy, v, vE, eT, nodeToLabelCopy, rootNodesCopy);
                    cutForest.add(eT);
                }

                // System.out.println("CutForest: ");
                // for(EasyTree c : cutForest)
                // System.out.println(c.getPhyloTree()+";");

                cutForests.add(cutForest);
            }

            return runRec(cutForests, k);
        }

        return null;
    }

    private MyGraph copyGraph(MyGraph g, MyNode startNode, MyNode exitNode, Vector<MyNode> rootNodes,
                              HashMap<MyNode, String> graphNodeToLabel2, Vector<MyNode> copyNodes, Vector<MyNode> rootNodesCopy,
                              HashMap<MyNode, String> nodeToLabelCopy) {

        MyGraph gCopy = new MyGraph();

        // copying single nodes
        for (MyNode rootNode : rootNodes) {
            if (rootNode.getDegree() == 0) {
                MyNode singleNode = gCopy.newNode();
                rootNodesCopy.add(singleNode);
                singleNode.setInfo(rootNode.getInfo());
                nodeToLabelCopy.put(singleNode, graphNodeToLabel2.get(rootNode));
            }
        }

        // copying connected subgraph
        MyNode startCopy = gCopy.newNode();
        startCopy.setInfo(startNode.getInfo());
        copyNodes.add(startCopy);
        rootNodesCopy.add(copyNodes.get(0));
        if (graphNodeToInfo.containsKey(startNode))
            graphNodeToInfo.put(startCopy, graphNodeToInfo.get(startNode));
        copyGraphRec(startNode, copyNodes.get(0), new HashMap<MyNode, MyNode>(), g, gCopy, exitNode, rootNodes,
                graphNodeToLabel2, copyNodes, rootNodesCopy, nodeToLabelCopy);

        return gCopy;
    }

    private void copyGraphRec(MyNode v, MyNode vCopy, HashMap<MyNode, MyNode> visitedToNode, MyGraph g, MyGraph gCopy,
                              MyNode exitNode, Vector<MyNode> rootNodes, HashMap<MyNode, String> graphNodeToLabel2,
                              Vector<MyNode> copyNodes, Vector<MyNode> rootNodesCopy, HashMap<MyNode, String> nodeToLabelCopy) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode w = e.getTarget();
            if (!visitedToNode.containsKey(w)) {
                MyNode wCopy = gCopy.newNode();
                visitedToNode.put(w, wCopy);
                wCopy.setInfo(w.getInfo());
                MyEdge eCopy = gCopy.newEdge(vCopy, wCopy);
                gCopy.setSpecial(eCopy, e.isSpecial());
                if (w.equals(exitNode))
                    copyNodes.add(wCopy);
                if (rootNodes.contains(w))
                    rootNodesCopy.add(wCopy);
                if (graphNodeToLabel2.containsKey(w))
                    nodeToLabelCopy.put(wCopy, graphNodeToLabel2.get(w));
                if (graphNodeToInfo.containsKey(w))
                    graphNodeToInfo.put(wCopy, graphNodeToInfo.get(w));
                copyGraphRec(w, wCopy, visitedToNode, g, gCopy, exitNode, rootNodes, graphNodeToLabel2, copyNodes,
                        rootNodesCopy, nodeToLabelCopy);
            } else {
                MyNode wCopy = visitedToNode.get(w);
                MyEdge eCopy = gCopy.newEdge(vCopy, wCopy);
                gCopy.setSpecial(eCopy, e.isSpecial());
            }
        }
    }

    private void fixExitNode(MyGraph g, MyNode exitNode, Vector<MyNode> rootNodes, MyNode startNode,
                             HashMap<MyNode, String> graphNodeToLabel2) {

        MyEdge e = exitNode.getFirstInEdge();
        MyNode p = e.getSource();
        Vector<MyNode> newRootNodes = new Vector<MyNode>();

        while (!e.isSpecial()) {
            p = e.getSource();
            MyNode v = e.getTarget();
            g.deleteEdge(e);
            newRootNodes.add(v);
            e = p.getFirstInEdge();
        }

        newRootNodes.add(p);
        rootNodes.remove(p);

        Vector<MyNode> sourceNodes = new Vector<MyNode>();
        Iterator<MyEdge> it = p.inEdges().iterator();
        while (it.hasNext())
            sourceNodes.add(it.next().getSource());

        for (MyNode newRootNode : newRootNodes) {

            Vector<MyEdge> specialEdges = new Vector<MyEdge>();
            Iterator<MyEdge> itOut = newRootNode.outEdges().iterator();
            while (itOut.hasNext()) {
                MyEdge outEdge = itOut.next();
                if (outEdge.isSpecial())
                    specialEdges.add(outEdge);
            }

            for (MyEdge eSpec : specialEdges)
                g.deleteEdge(eSpec);

            if (newRootNode.getOutDegree() == 1) {
                MyEdge outEdge = newRootNode.getFirstOutEdge();
                MyNode w = outEdge.getTarget();
                g.deleteEdge(outEdge);
                g.deleteNode(newRootNode);
                newRootNode = w;
            }
            for (MyNode source : sourceNodes) {
                if (((BitSet) newRootNode.getInfo()).cardinality() > 1) {
                    MyEdge newEdge = g.newEdge(source, newRootNode);
                    g.setSpecial(newEdge, true);
                }
            }

            rootNodes.add(newRootNode);
        }

    }

    private boolean hasCycle(MyGraph g, MyNode startNode) {

        // init levels
        Vector<MyNode> visited = new Vector<MyNode>();
        initLevels(startNode, 0, visited);

        // search for scc
        for (int i = visited.size() - 1; i >= 0; i--) {
            MyNode v = levelToNode.get(i);
            Iterator<MyEdge> it = v.inEdges().iterator();
            while (it.hasNext()) {
                MyNode w = it.next().getSource();
                if (nodeToLevel.get(w) < i) {
                    computeExitNodes(g, w, w, new HashSet<MyNode>(), new HashSet<MyEdge>());
                    // System.out.println(v.getInfo()+" < "+w.getInfo()+" "+exitNodes.size());
                    return true;
                }
            }
        }
        return false;
    }

    private void computeExitNodes(MyGraph g, MyNode v, MyNode start, HashSet<MyNode> exitNodes, HashSet<MyEdge> visited) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            if (!visited.contains(e)) {
                MyNode w = e.getTarget();
                if (w.equals(start))
                    this.exitNodes = exitNodes;
                else {
                    HashSet<MyEdge> visitedCopy = (HashSet<MyEdge>) visited.clone();
                    visitedCopy.add(e);
                    visited.add(e);
                    HashSet<MyNode> exitNodesCopy = (HashSet<MyNode>) exitNodes.clone();
                    // System.out.println(e.getSource().getInfo()+" -> "+e.getTarget().getInfo()+" "+e.isSpecial()+" "+(graphNodeToInfo.get(e.getTarget())+" "+e.getInfo()));
                    if (e.isSpecial()) { // &&
                        // graphNodeToInfo.get(e.getTarget())
                        // instanceof Integer) {
                        // Integer headInfo = (Integer)
                        // graphNodeToInfo.get(e.getTarget());
                        // Integer edgeInfo = (Integer) e.getInfo();
                        // if (headInfo != null && edgeInfo != null &&
                        // headInfo.equals(edgeInfo))
                        exitNodesCopy.add(e.getSource());
                    }
                    computeExitNodes(g, w, start, exitNodesCopy, visitedCopy);
                }
            }
        }
    }

    private int initLevels(MyNode v, int level, Vector<MyNode> visited) {

        visited.add(v);
        Iterator<MyEdge> it = v.outEdges().iterator();
        int value = level;
        while (it.hasNext()) {
            MyNode w = it.next().getTarget();
            if (!visited.contains(w))
                value = initLevels(w, value, visited) + 1;
        }
        levelToNode.put(value, v);
        nodeToLevel.put(v, value);
        return value;
    }

    private void addCompToGraph(MyGraph g, EasyTree comp, boolean isRho, Vector<EasyNode> specialNodes) {

        HashMap<EasyNode, BitSet> compNodeToLeafSet = new HashMap<EasyNode, BitSet>();
        HashMap<BitSet, EasyNode> leafSetToCompNode = new HashMap<BitSet, EasyNode>();
        initTreeLeafLists(comp.getRoot(), compNodeToLeafSet, leafSetToCompNode);
        BitSet compSet = compNodeToLeafSet.get(comp.getRoot());

        MyNode rootG = g.newNode();
        EasyNode root = comp.getRoot();
        rootG.setInfo(compNodeToLeafSet.get(root));
        if (root.getInfo() != null)
            graphNodeToInfo.put(rootG, root.getInfo());
        Vector<MyNode> newLeaves = new Vector<MyNode>();
        addNodesToGraph(g, root, rootG, compNodeToLeafSet, newLeaves, specialNodes);

        if (isRho)
            start = rootG;

        for (MyNode leaf : newLeaves) {
            BitSet leafSet = (BitSet) leaf.getInfo();

            EasyNode t1Node = leafSetToT1Node.get(leafSet);

            HashSet<MyNode> t1Visited = new HashSet<MyNode>();
            for (Vector<MyNode> v : t1NodeToGraphNode.values())
                t1Visited.addAll(v);
            assignTreeNodesToGraphNodes(t1NodeToGraphNode, t1NodeToLeafSet, graphRootToT1Node, t1Node, leaf, leafSet,
                    compSet, rootG, t1Visited);

            EasyNode t2Node = leafSetToT2Node.get(leafSet);
            HashSet<MyNode> t2Visited = new HashSet<MyNode>();
            for (Vector<MyNode> v : t2NodeToGraphNode.values())
                t2Visited.addAll(v);
            assignTreeNodesToGraphNodes(t2NodeToGraphNode, t2NodeToLeafSet, graphRootToT2Node, t2Node, leaf, leafSet,
                    compSet, rootG, t2Visited);
        }

    }

    private void addNodesToGraph(MyGraph g, EasyNode v, MyNode vG, HashMap<EasyNode, BitSet> compNodeToLeafSet,
                                 Vector<MyNode> leaves, Vector<EasyNode> specialNodes) {
        for (EasyNode c : v.getChildren()) {
            MyNode cG = g.newNode();
            cG.setInfo(compNodeToLeafSet.get(c));
            if (c.getInfo() != null)
                graphNodeToInfo.put(cG, c.getInfo());
            if (specialNodes.contains(c))
                nodeToGraphNode.put(c, cG);
            addNodesToGraph(g, c, cG, compNodeToLeafSet, leaves, specialNodes);
            g.newEdge(vG, cG);
        }
        if (v.getOutDegree() == 0) {
            graphNodeToLabel.put(vG, v.getLabel());
            leaves.add(vG);
        }
    }

    private void assignTreeNodesToGraphNodes(HashMap<EasyNode, Vector<MyNode>> treeNodeToGraphNode,
                                             HashMap<EasyNode, BitSet> treeNodeToLeafSet, HashMap<MyNode, EasyNode> graphRootToTreeNode,
                                             EasyNode treeNode, MyNode v, BitSet vSet, BitSet compSet, MyNode gRoot, HashSet<MyNode> visited) {
        if (!visited.contains(v)) {
            BitSet b = treeNodeToLeafSet.get(treeNode);
            BitSet b1 = (BitSet) b.clone();
            b1.and(compSet);
            if (b1.equals(vSet)) {
                if (!treeNodeToGraphNode.containsKey(treeNode))
                    treeNodeToGraphNode.put(treeNode, new Vector<MyNode>());
                treeNodeToGraphNode.get(treeNode).add(v);
                if (v.getInDegree() == 0)
                    graphRootToTreeNode.put(v, treeNode);
                else
                    assignTreeNodesToGraphNodes(treeNodeToGraphNode, treeNodeToLeafSet, graphRootToTreeNode,
                            treeNode.getParent(), v, vSet, compSet, gRoot, visited);
            } else if (v.inEdges().iterator().hasNext()) {
                MyNode p = v.getFirstInEdge().getSource();
                assignTreeNodesToGraphNodes(treeNodeToGraphNode, treeNodeToLeafSet, graphRootToTreeNode, treeNode, p,
                        (BitSet) p.getInfo(), compSet, gRoot, visited);
            }
        }
    }

    private BitSet initTreeLeafLists(EasyNode v, HashMap<EasyNode, BitSet> treeNodeToLeafSet,
                                     HashMap<BitSet, EasyNode> leafSetToTreeNode) {
        BitSet b = new BitSet(taxaOrdering.size());
        if (v.getOutDegree() == 0)
            b.set(taxaOrdering.indexOf(v.getLabel()));
        else {
            for (EasyNode c : v.getChildren())
                b.or(initTreeLeafLists(c, treeNodeToLeafSet, leafSetToTreeNode));
        }
        treeNodeToLeafSet.put(v, b);
        leafSetToTreeNode.put(b, v);
        return b;
    }

    private void parseComponent(MyGraph g, MyNode v, EasyNode vE, EasyTree eT,
                                HashMap<MyNode, String> graphNodeToLabel2, Vector<MyNode> rootNodes) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            if (!e.isSpecial()) {
                MyNode w = e.getTarget();
                EasyNode wE = new EasyNode(vE, eT, graphNodeToLabel2.get(w));
                wE.setInfo(w.getInfo());
                parseComponent(g, w, wE, eT, graphNodeToLabel2, rootNodes);
            }
        }

    }

    public String compToString(EasyNode v) {
        if (v.getOutDegree() == 0)
            return v.getLabel();
        else {
            String s1 = compToString(v.getChildren().get(0));
            String s2 = compToString(v.getChildren().get(1));
            return "(" + s1 + "," + s2 + " - " + v.getOutDegree() + ")";
        }
    }

    public void graphToString(MyNode v, Vector<MyNode> visited) {
        if (!visited.contains(v)) {
            visited.add(v);
            if (v.getOutDegree() != 0) {
                Iterator<MyEdge> it = v.outEdges().iterator();
                while (it.hasNext()) {
                    MyEdge e = it.next();
                    MyNode c = e.getTarget();
                    System.out.println(v.getInfo() + "->" + c.getInfo() + " " + e.getInfo());
                    graphToString(c, visited);
                }
            }
        }
    }

}
