/*
 * ExtendedNewickTest.java Copyright (C) 2026 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.io;

import dendroscope.core.TreeData;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * tests that Dendroscope reads and writes its extended Newick format as it did before the move to jloda3
 * <p>
 * Unless a test says otherwise, the expected string is what the old jloda library read and wrote for the same input
 * (jloda.phylo.PhyloTree in the jloda.jar that Dendroscope 3.8.12 was built with on 19 Dec 2025), recorded on
 * 23 Nov 2026 with a probe against that jar. The one systematic difference: the old PhyloGraph did not store an edge
 * weight equal to the default of 1 while no other weight had been stored yet, so a:1 came out as a; jloda3 stores it.
 * Daniel Huson, 11.2026
 */
class ExtendedNewickTest {
	/**
	 * as Save in Newick format writes a tree: edge weights, and edge labels in brackets after them
	 */
	private static String readAndWrite(String newick) throws IOException {
		var tree = ExtendedNewick.valueOf(newick);
		return ExtendedNewick.toBracketString(tree, new ExtendedNewick.OutputFormat(true, false, false, false, true)) + ";";
	}

	@Test
	void edgeLabelsAfterWeightsAndInternalNodeLabels() throws IOException {
		var input = "((a:1.5[e1],b:2[e2])90:0.5[x],(c,d)'node lbl':1)root;";
		assertEquals(input, readAndWrite(input));

		var tree = ExtendedNewick.valueOf(input);
		// a number on an internal node stays a node label; it does not become a confidence value
		assertTrue(tree.nodeStream().anyMatch(v -> "90".equals(tree.getLabel(v))));
		assertFalse(tree.hasEdgeConfidences());
		assertEquals(3, tree.edgeStream().filter(e -> tree.getLabel(e) != null).count());
	}

	@Test
	void bootstrapValuesAndSmallWeights() throws IOException {
		var input = "((a:0.1,b:0.25)80:0.3,(c:1,d:0.000001)100:2.5);";
		assertEquals(input, readAndWrite(input));
	}

	@Test
	void reticulateNode() throws IOException {
		var input = "((a,(b)#H1),(#H1,c));";
		// the second instance of the reticulate node is re-attached last, so it follows c
		assertEquals("((a,(b)#H1:0),(c,#H1:0));", readAndWrite(input));

		var network = ExtendedNewick.valueOf(input);
		assertEquals(7, network.getNumberOfNodes());
		assertEquals(7, network.getNumberOfEdges());
		assertEquals(2, network.getNumberReticulateEdges());
	}

	@Test
	void transferAcceptorEdge() throws IOException {
		// the old jloda wrote ((a,(b:2)##H1:1),(c:1,#H1)); losing a:1 (see above)
		assertEquals("((a:1,(b:2)##H1:1),(c:1,#H1));", readAndWrite("((a:1,(b:2)##H1:1),(#H1:3,c:1));"));
	}

	@Test
	void multiLabeledLeavesAreKeptByDefault() throws IOException {
		assertEquals("((a,a),(b,c));", readAndWrite("((a,a),(b,c));"));
	}

	@Test
	void multiLabeledLeavesAreNumberedWhenNotAllowed() throws IOException {
		var newick = new ExtendedNewick();
		newick.setAllowMultiLabeledNodes(false);
		var tree = new jloda.phylo.PhyloTree();
		newick.parseBracketNotation(tree, "((a,a),(b,c));", true);
		assertTrue(newick.isInputHasMultiLabels());
		assertEquals("((a.1,a.2),(b,c))", ExtendedNewick.toBracketString(tree, false));
	}

	@Test
	void treeDataReadsAndWritesTheDendroscopeDialect() throws IOException {
		// the two-argument parseBracketNotation and toBracketString of PhyloTree are overridden by TreeData;
		// the old jloda gave ((a,b:2)90,c), losing a:1 (see above)
		var tree = new TreeData();
		tree.parseBracketNotation("((a:1[x],b:2)90,c);", true);
		assertEquals("((a:1,b:2)90,c)", tree.toBracketString());
		assertEquals("((a,b)90,c)", tree.toBracketString(false));
	}

	@Test
	void malformedInputIsAnError() {
		assertThrows(IOException.class, () -> ExtendedNewick.valueOf("((a,b),c"));
		assertThrows(IOException.class, () -> ExtendedNewick.valueOf("((a,b):x,c);"));
	}
}
