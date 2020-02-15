/*
 *   MySparseGraph.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.util.sparseGraph;

import dendroscope.hybroscale.util.graph.MyNode;

public class MySparseGraph {

	public MySparseNode newNode(MyNode v) {
		MySparseNode vCopy = new MySparseNode(this, v.getLabel());
		vCopy.setInfo(v.getInfo());

		return vCopy;
	}

	public MySparseNode newNode() {
		MySparseNode newNode = new MySparseNode(this, null);
		return newNode;
	}

	public void newEdge(MySparseNode v, MySparseNode w) {
		v.addChild(w);
		w.setParent(v);
	}

	public void deleteNode(MySparseNode v) {
		if (v.getOwner().equals(this)) {
			for(MySparseNode c : v.getChildren())
				c.setParent(null);
			MySparseNode p = v.getParent();
			if(p != null)
				p.removeChild(v);
			v.setParent(null);
		} else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setLabel(MySparseNode v, String label) {
		if (v.getOwner().equals(this))
			v.setLabel(label);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setInfo(MySparseNode v, Object info) {
		if (v.getOwner().equals(this))
			v.setInfo(info);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

}
