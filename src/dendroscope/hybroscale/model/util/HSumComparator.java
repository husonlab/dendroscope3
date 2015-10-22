package dendroscope.hybroscale.model.util;

import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class HSumComparator implements Comparator<Vector<Integer>> {

	private ConcurrentHashMap<Vector<Integer>, Vector<Integer>> orderingToMaxima;

	public HSumComparator(ConcurrentHashMap<Vector<Integer>, Vector<Integer>> orderingToMaxima) {
		this.orderingToMaxima = orderingToMaxima;
	}

	@Override
	public int compare(Vector<Integer> o1, Vector<Integer> o2) {
		Vector<Integer> maxVec1 = orderingToMaxima.get(o1);
		Vector<Integer> maxVec2 = orderingToMaxima.get(o2);
		
		for (int i = 0; i < maxVec1.size(); i++) {
			int max1 = maxVec1.get(i);
			int max2 = maxVec2.get(i);
			if (max1 > max2)
				return -1;
			if (max1 < max2)
				return 1;		
		}
		return 0;
	}

}
