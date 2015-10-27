package dendroscope.hybroscale.model.cmpAllMAAFs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class EasyIsomorphismCheck {

	public boolean run(EasyTree n1, EasyTree n2) {

		EasyTree n1Copy = new EasyTree(n1);
		EasyTree n2Copy = new EasyTree(n2);

		if (n1Copy.getNodes().size() != n2Copy.getNodes().size())
			return false;

		while (n1Copy.getNodes().size() > 2) {
			
			HashSet<String> t1Cherrys = new HashSet<String>();
			Hashtable<String, EasyNode> t1Taxa2parent = new Hashtable<String, EasyNode>();

			// collect all cherries in t1
			// -> a cherry is a sorted string assembled by its taxon labelings
			getCherrys(n1Copy, t1Cherrys, t1Taxa2parent);

			HashSet<String> t2Cherrys = new HashSet<String>();
			Hashtable<String, EasyNode> t2Taxa2parent = new Hashtable<String, EasyNode>();

			// collect all cherries in t2
			getCherrys(n2Copy, t2Cherrys, t2Taxa2parent);

			// compare the two cherry sets..
			if (t1Cherrys.size() != t2Cherrys.size())
				return false;

			Iterator<String> it = t2Cherrys.iterator();
			while (it.hasNext()) {
				if (!t1Cherrys.contains(it.next()))
					return false;
			}

			// generate new cherries in both trees
			if (n1Copy.getNodes().size() > 3) {
				replaceCherrys(n1Copy, t1Taxa2parent);
				replaceCherrys(n2Copy, t2Taxa2parent);
			} else
				return true;

		}

		return true;
	}

	private void replaceCherrys(EasyTree t, Hashtable<String, EasyNode> taxa2parent) {
		Iterator<String> it = taxa2parent.keySet().iterator();
		while (it.hasNext()) {
			String taxon = it.next();
			EasyNode v = taxa2parent.get(taxon);
			v.setLabel(taxon);
			Vector<EasyNode> children = new Vector<EasyNode>();
			for (EasyNode c : v.getChildren())
				children.add(c);
			for (EasyNode c : children) {
				c.delete();
			}
			
			int size = v.getChildren().size();
			for (int i = 0; i < size; i++)
				t.deleteNode(v.getChildren().get(0));
		}
	}

	private void getCherrys(EasyTree n1Mod, HashSet<String> cherrys, Hashtable<String, EasyNode> taxa2parent) {
		Iterator<EasyNode> it = n1Mod.getLeaves().iterator();
		Vector<EasyNode> parents = new Vector<EasyNode>();
		while (it.hasNext()) {
			EasyNode v = it.next();
			EasyNode p = v.getParent();
			if (!parents.contains(p) && isCherry(p)) {

				Vector<String> taxa = new Vector<String>();

				// collect taxa
				for (EasyNode c : p.getChildren())
					taxa.add(c.getLabel());

				// sort taxas lexicographically
				Collections.sort(taxa);

				// generate cherry-string
				String taxaString = "";
				for (String s : taxa)
					taxaString = taxaString.concat(s);
				cherrys.add(taxaString);

				parents.add(p);
				taxa2parent.put(taxaString, p);
			}
		}
	}

	private boolean isCherry(EasyNode p) {
		for (EasyNode c : p.getChildren()) {
			if (c.getOutDegree() != 0)
				return false;
		}
		return true;
	}
}
