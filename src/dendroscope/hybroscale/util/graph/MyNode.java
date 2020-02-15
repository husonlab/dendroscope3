/*
 *   MyNode.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.util.graph;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class MyNode {

	private Vector<MyEdge> inEdges = new Vector<>();
	private Vector<MyEdge> outEdges = new Vector<>();

	private BitSet cluster;
	private MyGraph owner;
	private Object info;
	private String label;
	private boolean solid = false;

	public MyNode(MyGraph owner, String label) {
		this.owner = owner;
		this.label = label;
	}

	public MyGraph getOwner() {
		return owner;
	}

	public int getOutDegree() {
		return outEdges.size();
	}

	public int getInDegree() {
		return inEdges.size();
	}

	public int getDegree() {
		return inEdges.size() + outEdges.size();
	}

	public void addInEdge(MyEdge e) {
		if (e.getTarget().equals(this))
			inEdges.add(e);
	}

	public void removeOutEdge(MyEdge e) {
		if (outEdges.contains(e))
			outEdges.remove(e);
	}

	public Iterator<MyEdge> getOutEdges() {
		return outEdges.iterator();
	}

	public void addOutEdge(MyEdge e) {
		if (e.getSource().equals(this))
			outEdges.add(e);
	}

	public void removeInEdge(MyEdge e) {
		if (inEdges.contains(e))
			inEdges.remove(e);
	}

	public Iterable<MyEdge> outEdges() {
		return () -> outEdges.iterator();
	}


	public Iterable<MyEdge> inEdges() {
		return () -> inEdges.iterator();
	}


	public Iterator<MyEdge> getInEdges() {
		return inEdges.iterator();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String s) {
		label = s;
	}

	public void setOwner(MyPhyloTree t) {
		owner = t;
	}

	public Object getInfo() {
		return info;
	}

	public void setInfo(Object info) {
		this.info = info;
	}

	public BitSet getCluster() {
		return cluster;
	}

	public void setCluster(BitSet cluster) {
		this.cluster = cluster;
	}

	public String toNewick(String newickString, Vector<MyNode> visitedNodes, Hashtable<MyNode, String> nodeToHTag) {
		if (!visitedNodes.contains(this)) {
			visitedNodes.add(this);
			if (outEdges.isEmpty() && inEdges.isEmpty())
				return "("+getLabelString()+")";
			else if (outEdges.isEmpty()) {
				if (inEdges.size() < 2)
					return getLabelString();
				else {
					String hTag = "#H" + nodeToHTag.keySet().size();
					nodeToHTag.put(this, hTag);
					return getLabelString() + hTag + ":0.0";
				}
			} else {
				String subString = "(";
				for (MyEdge e : outEdges) {
					MyNode c = e.getTarget();
					String hTag = "";
					if (nodeToHTag.containsKey(c))
						hTag = nodeToHTag.get(c) + ":0.0";
					if (outEdges.lastElement().equals(e))
						subString = subString.concat(c.toNewick(newickString, visitedNodes, nodeToHTag) + hTag + ")");
					else
						subString = subString.concat(c.toNewick(newickString, visitedNodes, nodeToHTag) + hTag + ",");
				}
				if (inEdges.size() < 2)
					return subString + getLabelString();
				else {
					String hTag = "#H" + nodeToHTag.keySet().size();
					nodeToHTag.put(this, hTag);
					return subString + getLabelString() + hTag + ":0.0";
				}
			}
		} else
			return getLabelString();
	}

	public String toMyNewick(String newickString, Vector<MyNode> visitedNodes, Hashtable<MyNode, String> nodeToHTag,
							 MyEdge inEdge) {
		if (!visitedNodes.contains(this)) {
			visitedNodes.add(this);
			if (outEdges.isEmpty() && inEdges.isEmpty())
				return "("+getLabelString()+")";
			else if (outEdges.isEmpty()) {
				if (inEdges.size() < 2) {
					String l = getLabelString() + getEdgeString(inEdge);
					return l;
				} else {
					String hTag = "#H" + nodeToHTag.keySet().size();
					nodeToHTag.put(this, hTag);
					String l = getLabelString() + hTag + getEdgeString(inEdge);
					return l;
				}
			} else {
				String subString = "(";
				for (MyEdge e : outEdges) {
					MyNode c = e.getTarget();
					String hTag = "";
					if (nodeToHTag.containsKey(c))
						hTag = nodeToHTag.get(c) + getEdgeString(e);
					if (outEdges.lastElement().equals(e))
						subString = subString.concat(c.toMyNewick(newickString, visitedNodes, nodeToHTag, e) + hTag
								+ ")");
					else
						subString = subString.concat(c.toMyNewick(newickString, visitedNodes, nodeToHTag, e) + hTag
								+ ",");
				}
				if (inEdges.size() < 2) {
					String l = subString + getLabelString() + getEdgeString(inEdge);
					return l;
				} else {
					String hTag = "#H" + nodeToHTag.keySet().size();
					nodeToHTag.put(this, hTag);
					String l = subString + getLabelString() + hTag + getEdgeString(inEdge);
					return l;
				}
			}
		} else {
			String l = getLabelString();
			return l;
		}
	}

	private String getLabelString() {
		if (label == null)
			return "";
		return label;
	}

	private String getEdgeString(MyEdge e) {
		if (e == null)
			return "";
		return e.getMyNewickString();
	}

	public void setSolid(boolean solid) {
		this.solid = solid;
	}

	public boolean isSolid() {
		return solid;
	}

	public MyEdge getFirstInEdge() {
		return inEdges.firstElement();
	}

	public MyEdge getFirstOutEdge() {
		return outEdges.firstElement();
	}

}
