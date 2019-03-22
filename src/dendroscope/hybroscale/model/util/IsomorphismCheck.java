package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class IsomorphismCheck {

	@SuppressWarnings("unchecked")
	public boolean run(HybridTree t1, HybridTree t2, Vector<String> taxaOrdering) {
		
		HybridTree t1Mod = new HybridTree(t1, false, (Vector<String>) taxaOrdering);
		HybridTree t2Mod = new HybridTree(t2, false, (Vector<String>) taxaOrdering);
		
		if(t1Mod.getNumberOfNodes() != t2Mod.getNumberOfNodes())
			return false;	
		
		while (t1Mod.getNumberOfNodes() > 2) {	
			
			HashSet<String> t1Cherrys = new HashSet<String>();
			Hashtable<String, MyNode> t1Taxa2parent = new Hashtable<String, MyNode>();
			
			//collect all cherries in t1
			//-> a cherry is a sorted string assembled by its taxon labelings
			getCherrys(t1Mod, t1Cherrys, t1Taxa2parent);
			
			HashSet<String> t2Cherrys = new HashSet<String>();
			Hashtable<String, MyNode> t2Taxa2parent = new Hashtable<String, MyNode>();
			
			//collect all cherries in t2
			getCherrys(t2Mod, t2Cherrys, t2Taxa2parent);
			
			//compare the two cherry sets..
			if (t1Cherrys.size() != t2Cherrys.size())
				return false;

			Iterator<String> it = t2Cherrys.iterator();
			while (it.hasNext()) {
				if (!t1Cherrys.contains(it.next()))
					return false;		
			}
			
			//generate new cherries in both trees
			if (t1Mod.getNumberOfNodes() > 3) {
				replaceCherrys(t1Mod, t1Taxa2parent);
				replaceCherrys(t2Mod, t2Taxa2parent);
			} else
				return true;
			
		}

		return true;
	}

	private void replaceCherrys(HybridTree n,
			Hashtable<String, MyNode> taxa2parent) {
		Iterator<String> it = taxa2parent.keySet().iterator();
		while (it.hasNext()) {
			String taxon = it.next();
			MyNode v = taxa2parent.get(taxon);
			MyNode newV = n.newNode();
			n.setLabel(newV, taxon);
			n.deleteSubtree(v, newV, true);
		}
	}

	private void getCherrys(HybridTree n, HashSet<String> cherrys,
			Hashtable<String, MyNode> taxa2parent) {
		Iterator<MyNode> it = n.getLeaves().iterator();
		Vector<MyNode> parents = new Vector<MyNode>();
		while (it.hasNext()) {
			MyNode v = it.next();
            MyNode p = ((MyEdge) v.getFirstInEdge()).getSource();
			if (!parents.contains(p) && isCherry(p)) {
				
				Vector<String> taxa = new Vector<String>();
                Iterator<MyEdge> it2 = p.outEdges().iterator();
				
				//collect taxa
				while (it2.hasNext())
					taxa.add(n.getLabel(it2.next().getTarget()));
				
				//sort taxas lexicographically
				Collections.sort(taxa);
				
				//generate cherry-string
				String taxaString = "";
				for (String s : taxa)
					taxaString = taxaString.concat(s);
				cherrys.add(taxaString);
				
				parents.add(p);
				taxa2parent.put(taxaString, p);
			}
		}
	}

	private boolean isCherry(MyNode p) {
        Iterator<MyEdge> it = p.outEdges().iterator();
		while (it.hasNext()) {
			if (it.next().getTarget().getOutDegree() != 0)
				return false;
		}
		return true;
	}
}
