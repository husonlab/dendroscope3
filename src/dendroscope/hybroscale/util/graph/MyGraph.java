/*
 *   MyGraph.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class MyGraph {

	private Vector<MyNode> nodes = new Vector<MyNode>();

	public MyEdge newEdge(MyNode source, MyNode target) {
		MyEdge e = new MyEdge(this, source, target);
		source.addOutEdge(e);
		target.addInEdge(e);
		return e;
	}

	public MyNode newNode(MyNode v) {
		MyNode newNode = new MyNode(this, v.getLabel());
		newNode.setInfo(v.getInfo());
		nodes.add(newNode);
		return newNode;
	}

	public MyNode newNode() {
		MyNode newNode = new MyNode(this, null);
		nodes.add(newNode);
		return newNode;
	}

	public void deleteEdge(MyEdge e) {
		e.getSource().removeOutEdge(e);
		e.getTarget().removeInEdge(e);
	}

	public void deleteNode(MyNode v) {
		if (v.getOwner().equals(this)) {
			Iterator<MyEdge> it = v.inEdges().iterator();
			HashSet<MyEdge> toRemove = new HashSet<MyEdge>();
			while (it.hasNext()) {
				MyEdge e = it.next();
				e.getSource().removeOutEdge(e);
				toRemove.add(e);
			}
			for(MyEdge e : toRemove)
				v.removeInEdge(e);
			it = v.outEdges().iterator();
			toRemove = new HashSet<MyEdge>();
			while (it.hasNext()) {
				MyEdge e = it.next();
				e.getTarget().removeInEdge(e);
				toRemove.add(e);
			}
			for(MyEdge e : toRemove)
				v.removeOutEdge(e);
			nodes.remove(v);
		} else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setLabel(MyEdge e, String label) {
		if (e.getOwner().equals(this))
			e.setLabel(label);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setLabel(MyNode v, String label) {
		if (v.getOwner().equals(this))
			v.setLabel(label);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public boolean isSpecial(MyEdge e) {
		if (e.getOwner().equals(this)) {
			if (e.getTarget().getInDegree() > 1)
				return true;
			return false;
		} else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setSpecial(MyEdge e, boolean b) {
		if (e.getOwner().equals(this))
			e.setSpecial(b);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setInfo(MyEdge e, Object info) {
		if (e.getOwner().equals(this))
			e.setInfo(info);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setInfo(MyNode v, Object info) {
		if (v.getOwner().equals(this))
			v.setInfo(info);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public int getNumberOfNodes(){
		return nodes.size();
	}

	public Vector<MyNode> getNodes(){
		return nodes;
	}

}
