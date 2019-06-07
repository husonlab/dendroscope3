/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.phylo.PhyloTree;

import java.util.Iterator;
import java.util.Vector;

public interface EasyTreeInterface {

    EasyNode getRoot();

    Iterator<EasyNode> postOrderWalk();

    Vector<EasyNode> getLeaves();

    void deleteNode(EasyNode v);

    PhyloTree getPhyloTree();

    EasyTree pruneSubtree(EasyNode v);

    Vector<EasyNode> getNodes();

    void setLabel(EasyNode v, String s);

}
