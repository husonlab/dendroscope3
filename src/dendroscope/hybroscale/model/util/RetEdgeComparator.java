package dendroscope.hybroscale.model.util;

import java.util.Comparator;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;

public class RetEdgeComparator implements Comparator<SparseNetEdge> {

	@Override
	public int compare(SparseNetEdge e1, SparseNetEdge e2) {
		if(e1.getSource().getOrder() > e2.getSource().getOrder())
			return 1;
		if(e1.getSource().getOrder() < e2.getSource().getOrder())
			return -1;
		return 0;
	}

}
