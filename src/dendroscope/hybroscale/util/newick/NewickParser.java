package dendroscope.hybroscale.util.newick;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.Hashtable;
import java.util.Vector;

public class NewickParser {

	private Hashtable<String, Vector<MyNode>> hTagToNodes;
	private Hashtable<Integer, Integer> bracketToBracket;
	private Hashtable<Integer, Integer> sqBracketToSqBracket;
	private String newickString;

	public NewickParser() {
		hTagToNodes = new Hashtable<String, Vector<MyNode>>();
		bracketToBracket = new Hashtable<Integer, Integer>();
		sqBracketToSqBracket = new Hashtable<Integer, Integer>();
	}

	public MyPhyloTree run(String newickString) {
		this.newickString = newickString;
		mapBrackets(newickString);
		mapSqBrackets(newickString);

		MyPhyloTree t = new MyPhyloTree();
		int pos = newickString.length() - 1;
		String rootLabel = "";
		while (pos >= 0 && newickString.charAt(pos) != ')') {
			char c = newickString.charAt(pos);
			if (c != ';')
				rootLabel = c + rootLabel;
			pos--;
		}
		t.getRoot().setLabel(rootLabel);
		if (pos > 0)
			parseChildren(t, t.getRoot(), pos);

		setReticulations(t);

		return t;

	}

	private void setReticulations(MyPhyloTree t) {
		for (String hTag : hTagToNodes.keySet()) {
			MyNode target = null;
			for (MyNode v : hTagToNodes.get(hTag)) {
				if (v.getOutDegree() != 0) {
					target = v;
					break;
				}
			}
			if(target == null)
				target = hTagToNodes.get(hTag).firstElement();
			hTagToNodes.get(hTag).remove(target);
			for (MyNode v : hTagToNodes.get(hTag)) {
				if (v.getLabel() != null)
					target.setLabel(v.getLabel());
                MyEdge inEdge = v.getFirstInEdge();
				Object info = inEdge.getInfo();
				MyNode source = inEdge.getSource();
				t.deleteEdge(inEdge);
				MyEdge e = t.newEdge(source, target);
				e.setInfo(info);
			}
		}
	}

	private void mapBrackets(String newickString) {
		Vector<Integer> openBrackets = new Vector<Integer>();
		for (int i = 0; i < newickString.length(); i++) {
			char c = newickString.charAt(i);
			if (c == '(')
				openBrackets.add(i);
			else if (c == ')') {
				int openBracket = openBrackets.lastElement();
				bracketToBracket.put(openBracket, i);
				bracketToBracket.put(i, openBracket);
				openBrackets.removeElement(openBracket);
			}
		}
	}

	private void mapSqBrackets(String newickString) {
		int openSqBrackets = -1;
		for (int i = 0; i < newickString.length(); i++) {
			char c = newickString.charAt(i);
			if (c == '[')
				openSqBrackets = i;
			else if (c == ']') {
				sqBracketToSqBracket.put(openSqBrackets, i);
				sqBracketToSqBracket.put(i, openSqBrackets);
			}
		}
	}

	private void parseChildren(MyPhyloTree t, MyNode v, int i) {
		Hashtable<String, Vector<Integer>> labelToStartpos = splitBracket(i);
		for (String s : labelToStartpos.keySet()) {
			for (int startPos : labelToStartpos.get(s)) {
				String[] a = parseLabel(s);
				MyNode w = t.newNode();
				w.setLabel(a[0]);
				MyEdge e = t.newEdge(v, w);
				if (a[2] != null)
					e.setWeight(Double.parseDouble(a[2]));
				if (a[3] != null)
					e.setInfo(a[3].substring(0, a[3].length() - 1));
				if (a[1] != null)
					addNodeToHTag(a[1], w);
				if (startPos != -1)
					parseChildren(t, w, startPos);
			}
		}
	}

	private void addNodeToHTag(String hTag, MyNode w) {
		if (!hTagToNodes.containsKey(hTag))
			hTagToNodes.put(hTag, new Vector<MyNode>());
		hTagToNodes.get(hTag).add(w);
	}

	private Hashtable<String, Vector<Integer>> splitBracket(int pos) {
		Hashtable<String, Vector<Integer>> labelToStartpos = new Hashtable<String, Vector<Integer>>();
		int i = pos - 1;
		String s = "";
		while (i >= bracketToBracket.get(pos)) {
			char c = newickString.charAt(i);
			if (c == ',' || c == '(') {
				String label = s;
				if (!labelToStartpos.containsKey(label))
					labelToStartpos.put(label, new Vector<Integer>());
				labelToStartpos.get(label).add(-1);
				s = "";
			} else if (c == ')') {
				String label = s;
				if (!labelToStartpos.containsKey(label))
					labelToStartpos.put(label, new Vector<Integer>());
				labelToStartpos.get(label).add(i);
				i = bracketToBracket.get(i) - 1;
				s = "";
			} else if (c == ']') {
				s = newickString.substring(sqBracketToSqBracket.get(i), i + 1) + s;
				i = sqBracketToSqBracket.get(i);
			} else
				s = String.valueOf(c) + s;
			i--;
		}
		return labelToStartpos;
	}

	public String[] parseLabel(String l) {
		String[] content = new String[4];
		if (l.contains("[")) {
			int index = l.indexOf("[");
			String a[] = { l.substring(0, index), l.substring(index + 1) };
			content[3] = a.length == 2 ? a[1] : "";
			if (a.length > 1) {
				if (a[0].contains(":")) {
					a = a[0].split(":");
					content[2] = a.length == 2 ? a[1] : "";
					if (a.length > 1) {
						if (a[0].contains("#")) {
							a = a[0].split("#");
							content[0] = a.length > 0 ? a[0] : null;
							content[1] = a.length == 2 ? a[1] : "";
						} else
							content[0] = a[0];
					}
				} else if (a[0].contains("#")) {
					a = a[0].split("#");
					content[0] = a.length > 0 ? a[0] : null;
					content[1] = a.length == 2 ? a[1] : "";
				} else
					content[0] = a[0];
			}
		} else if (l.contains(":")) {
			String[] a = l.split(":");
			content[2] = a.length == 2 ? a[1] : "";
			if (a.length > 1) {
				if (a[0].contains("#")) {
					a = a[0].split("#");
					content[0] = a.length > 0 ? a[0] : null;
					content[1] = a.length == 2 ? a[1] : "";
				} else
					content[0] = a.length > 0 ? a[0] : null;
			}
		} else if (l.contains("#")) {
			String[] a = l.split("#");
			if (a.length > 1) {
				content[0] = a.length > 0 ? a[0] : null;
				content[1] = a.length == 2 ? a[1] : "";
			}
		} else
			content[0] = l;
		return content;
	}

}
