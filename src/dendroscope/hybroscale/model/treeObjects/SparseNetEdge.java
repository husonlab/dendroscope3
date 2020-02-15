/*
 *   SparseNetEdge.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.treeObjects;

import java.util.HashSet;
import java.util.Vector;

public class SparseNetEdge {

	private HashSet<Integer> edgeIndex = new HashSet<Integer>();
	private HashSet<Integer> indices = new HashSet<Integer>();
	private SparseNetNode source, target;

	private boolean solid = false;

	public SparseNetEdge(SparseNetNode source, SparseNetNode target) {
		this.source = source;
		this.target = target;
	}

	public SparseNetNode getSource() {
		return source;
	}

	public SparseNetNode getTarget() {
		return target;
	}

	public void addIndices(HashSet<Integer> newIndices) {
		indices.addAll(newIndices);
	}

	public void addIndices(Vector<Integer> newIndices) {
		indices.addAll(newIndices);
	}

	public void addIndex(int newIndex) {
		indices.add(newIndex);
	}

	public void removeIndex(int index) {
		indices.remove(index);
	}

	public HashSet<Integer> getIndices() {
		return indices;
	}

	public void removeEdgeIndex(int index){
		edgeIndex.remove(index);
	}

	public void addEdgeIndex(int index){
		edgeIndex.add(index);
	}

	public void addEdgeIndices(HashSet<Integer> indices){
		edgeIndex.addAll(indices);
	}

	public HashSet<Integer> getEdgeIndex() {
		return edgeIndex;
	}

	public void setEdgeIndex(HashSet<Integer> edgeIndex) {
		this.edgeIndex = edgeIndex;
	}

	public String toMyString() {
		return getSource().getLabel()+" -> "+getTarget().getLabel();
	}

	public void setSolid(boolean b) {
		solid = b;
	}

	public boolean isSolid() {
		return solid;
	}

}
