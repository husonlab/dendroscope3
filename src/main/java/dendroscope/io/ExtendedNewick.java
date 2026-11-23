/*
 * ExtendedNewick.java Copyright (C) 2026 Daniel H. Huson
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

import jloda.graph.Edge;
import jloda.graph.IllegalSelfEdgeException;
import jloda.graph.Node;
import jloda.graph.NodeIntArray;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeNetworkIOUtils;
import jloda.util.Basic;
import jloda.util.NumberUtils;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import jloda.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * reads and writes trees and rooted networks in the extended Newick format that Dendroscope has always used
 * <p>
 * This is the Newick code of jloda.phylo.PhyloTree in the jloda library that Dendroscope was built with until 2025,
 * moved here without change to what it reads and writes when Dendroscope moved to jloda3. The NewickIO of jloda3
 * reads a different dialect: it rejects a bracketed edge label after an edge weight, a:1.0[label], which is where
 * Dendroscope writes edge labels, and by default it turns numbers on internal nodes into confidence values rather
 * than node labels. Only the branches of the old code that apply when PhyloTree.SUPPORT_RICH_NEWICK is false are
 * kept, as Dendroscope never sets it.
 * <p>
 * Daniel Huson, 2003, 11.2026
 */
public class ExtendedNewick {
	public static boolean WARN_HAS_MULTILABELS = true;
	public static final String COLLAPSED_NODE_SUFFIX = "{+}";

	private static final String punctuationCharacters = "),;:[";

	private boolean allowMultiLabeledNodes = true;
	private boolean hideCollapsedSubTreeOnWrite = false;
	private boolean inputHasMultiLabels = false;
	private final boolean cleanLabelsOnWrite;

	private int outputNodeNumber = 0;
	private int outputEdgeNumber = 0;
	private NodeIntArray outputNodeReticulationNumberMap;  // global number of the reticulate nodes
	private int outputReticulationNumber;

	public ExtendedNewick() {
		cleanLabelsOnWrite = ProgramProperties.get("cleanTreeLabelsOnWrite", false);
	}

	/**
	 * which items to write
	 */
	public record OutputFormat(boolean weights, boolean confidenceAsNodeLabel, boolean confidenceUsingColon,
							   boolean probabilityUsingColon, boolean edgeLabelsAsComments) {
	}

	/**
	 * Given a string representation of a tree, returns the tree.
	 *
	 * @param str String
	 * @return tree PhyloTree
	 */
	public static PhyloTree valueOf(String str) throws IOException {
		var tree = new PhyloTree();
		new ExtendedNewick().parseBracketNotation(tree, str, true);
		return tree;
	}

	/**
	 * Produces a string representation of the tree in bracket notation, with edge weights
	 */
	public static String toBracketString(PhyloTree tree) {
		try (var w = new StringWriter()) {
			new ExtendedNewick().write(tree, w, true);
			return w.toString();
		} catch (Exception ex) {
			Basic.caught(ex);
			return "();";
		}
	}

	/**
	 * Produces a string representation of the tree in bracket notation
	 */
	public static String toBracketString(PhyloTree tree, boolean showWeights) {
		return toBracketString(tree, new OutputFormat(showWeights, false, false, false, false));
	}

	/**
	 * Produces a string representation of the tree in bracket notation
	 */
	public static String toBracketString(PhyloTree tree, OutputFormat format) {
		try (var sw = new StringWriter()) {
			new ExtendedNewick().write(tree, sw, format);
			return sw.toString();
		} catch (Exception ex) {
			Basic.caught(ex);
			return "();";
		}
	}

	/**
	 * reads a line and then parses it as a rooted tree or network in Newick format
	 *
	 * @param r the reader
	 */
	public void read(PhyloTree tree, Reader r) throws IOException {
		final BufferedReader br;
		if (r instanceof BufferedReader)
			br = (BufferedReader) r;
		else
			br = new BufferedReader(r);
		parseBracketNotation(tree, br.readLine(), true, true);
	}

	/**
	 * Parses a tree or network in Newick notation, and sets the root, if desired
	 */
	public void parseBracketNotation(PhyloTree tree, String str, boolean rooted) throws IOException {
		parseBracketNotation(tree, str, rooted, true);
	}

	/**
	 * Parses a tree or network in Newick notation, and sets the root, if desired
	 */
	public void parseBracketNotation(PhyloTree tree, String str, boolean rooted, boolean doClear) throws IOException {
		if (doClear)
			tree.clear();

		if (!StringUtils.checkSingleQuotes(str, 1000))
			throw new IOException("String contains problematic single quotes");

		inputHasMultiLabels = false;

		var seen = new HashMap<String, Node>();

		parseBracketNotationRec(tree, seen, 0, null, 0, str);

		if (tree.getNumberOfNodes() > 0) {
			final var v = tree.getFirstNode();
			if (rooted) {
				tree.setRoot(v);
			} else {
				if (isUnlabeledDiVertex(tree, v))
					tree.setRoot(tree.delDivertex(v).getSource());
				else tree.setRoot(v);
			}
		}

		// post process any reticulate nodes
		postProcessReticulate(tree);
	}

	/**
	 * recursively do the work
	 *
	 * @param seen  set of seen labels
	 * @param depth distance from root
	 * @param v     parent node
	 * @param pos   current position in string
	 * @param str   string
	 * @return new current position
	 */
	private int parseBracketNotationRec(PhyloTree tree, Map<String, Node> seen, int depth, Node v, int pos, String str) throws IOException {
		for (pos = StringUtils.skipSpaces(str, pos); pos < str.length(); pos = StringUtils.skipSpaces(str, pos + 1)) {
			var w = tree.newNode();
			String label = null;
			if (str.charAt(pos) == '(') {
				pos = parseBracketNotationRec(tree, seen, depth + 1, w, pos + 1, str);
				if (str.charAt(pos) != ')')
					throw new IOException("Expected ')' at position " + pos);
				pos = StringUtils.skipSpaces(str, pos + 1);
				while (pos < str.length() && punctuationCharacters.indexOf(str.charAt(pos)) == -1) {
					var pos0 = pos;
					var buf = new StringBuilder();
					var inQuotes = false;
					while (pos < str.length() && (inQuotes || punctuationCharacters.indexOf(str.charAt(pos)) == -1)) {
						if (str.charAt(pos) == '\'')
							inQuotes = !inQuotes;
						else
							buf.append(str.charAt(pos));
						pos++;
					}
					label = buf.toString().trim();

					if (!label.isEmpty()) {
						if (!isAllowMultiLabeledNodes() && seen.containsKey(label) && PhyloTreeNetworkIOUtils.findReticulateLabel(label) == null)
						// if label already used, make unique, unless this is a reticulate node
						{
							if (label.startsWith("'") && label.endsWith("'") && label.length() > 1)
								label = label.substring(1, label.length() - 1);
							// give first occurrence of this label the suffix .1
							final Node old = seen.get(label);
							if (old != null) // change label of node
							{
								tree.setLabel(old, label + ".1");
								seen.put(label, null); // keep label in, but null indicates has changed
								seen.put(label + ".1", old);
								inputHasMultiLabels = true;
							}

							var t = 1;
							String labelt;
							do {
								labelt = label + "." + (++t);
							} while (seen.containsKey(labelt));
							label = labelt;
						}
						seen.put(label, w);
					}
					tree.setLabel(w, label);
					if (label.isEmpty())
						throw new IOException("Expected label at position " + pos0);
				}
			} else // everything to next ) : or , is considered a label:
			{
				if (tree.getNumberOfNodes() == 1)
					throw new IOException("Expected '(' at position " + pos);
				var pos0 = pos;
				final var buf = new StringBuilder();
				boolean inQuotes = false;
				while (pos < str.length() && (inQuotes || punctuationCharacters.indexOf(str.charAt(pos)) == -1)) {
					if (str.charAt(pos) == '\'')
						inQuotes = !inQuotes;
					else
						buf.append(str.charAt(pos));
					pos++;
				}
				label = buf.toString().trim();

				if (label.startsWith("'") && label.endsWith("'") && label.length() > 1)
					label = label.substring(1, label.length() - 1).trim();

				if (!label.isEmpty()) {
					if (!isAllowMultiLabeledNodes() && seen.containsKey(label) && PhyloTreeNetworkIOUtils.findReticulateLabel(label) == null) {
						// give first occurrence of this label the suffix .1
						var old = seen.get(label);
						if (old != null) // change label of node
						{
							tree.setLabel(old, label + ".1");
							seen.put(label, null); // keep label in, but null indicates has changed
							seen.put(label + ".1", old);
							inputHasMultiLabels = true;
							if (WARN_HAS_MULTILABELS)
								System.err.println("multi-label: " + label);
						}

						var t = 1;
						String labelt;
						do {
							labelt = label + "." + (++t);
						} while (seen.containsKey(labelt));
						label = labelt;
					}
					seen.put(label, w);
				}
				tree.setLabel(w, label);
				if (label.isEmpty())
					throw new IOException("Expected label at position " + pos0);
			}
			Edge e = null;
			if (v != null)
				e = tree.newEdge(v, w);

			// detect and read embedded bootstrap values:
			pos = StringUtils.skipSpaces(str, pos);

			// read edge weights
			var didReadWeight = false;

			for (var which = 0; which < 3; which++) {
				if (pos < str.length() && str.charAt(pos) == ':') { // edge weight is following
					pos = StringUtils.skipSpaces(str, pos + 1);
					if (pos < str.length() && str.charAt(pos) == ':') {
						continue;
					}
					var pos0 = pos;
					var numberStr = StringUtils.getStringUptoDelimiter(str, pos0, punctuationCharacters);
					if (!NumberUtils.isDouble(numberStr))
						throw new IOException("Expected number at position " + pos0 + " (got: '" + numberStr + "')");
					pos = pos0 + numberStr.length();
					var value = Math.max(0, Double.parseDouble(numberStr));
					switch (which) {
						case 0 -> {
							if (e != null) {
								tree.setWeight(e, value);
								didReadWeight = true;
							}
						}
						case 1 -> {
							if (e != null) {
								tree.setConfidence(e, value);
							}
						}
						case 2 -> {
							if (e != null) {
								tree.setProbability(e, value);
							}
						}
					}

				}
				break; // don't allow confidence or probability (PhyloTree.SUPPORT_RICH_NEWICK is false in Dendroscope)
			}

			// adjust edge weights for reticulate edges
			if (e != null) {
				try {
					if (label != null && PhyloTreeNetworkIOUtils.isReticulateNode(label)) {
						// if an instance of a reticulate node is marked ##, then we will set the weight of the edge to the node to a number >0
						// to indicate that edge should be drawn as a tree edge
						if (PhyloTreeNetworkIOUtils.isReticulateAcceptorEdge(label)) {
							if (!didReadWeight || tree.getWeight(e) <= 0) {
								tree.setWeight(e, 0.000001);
							}
						} else {
							if (tree.getWeight(e) > 0)
								tree.setWeight(e, 0.0);
						}
					}
				} catch (IllegalSelfEdgeException e1) {
					Basic.caught(e1);
				}
			}

			// now i should be pointing to a ',', a ')' or '[' (for a label)
			if (pos >= str.length()) {
				if (depth == 0)
					return pos; // finished parsing tree
				else
					throw new IOException("Unexpected end of line");
			}
			if (str.charAt(pos) == '[') // edge label
			{
				int x = str.indexOf('[', pos + 1);
				int j = str.indexOf(']', pos + 1);
				if (j == -1 || (x != -1 && x < j))
					throw new IOException("Error in edge label at position: " + pos);
				if (e != null)
					tree.setLabel(e, str.substring(pos + 1, j));
				pos = j + 1;
			}
			if (str.charAt(pos) == ';' && depth == 0)
				return pos; // finished parsing tree
			else if (str.charAt(pos) == ')')
				return pos;
			else if (str.charAt(pos) != ',')
				throw new IOException("Unexpected '" + str.charAt(pos) + "' at position " + pos);
		}
		return -1;
	}

	/**
	 * post processes a tree that really describes a reticulate network
	 */
	private void postProcessReticulate(PhyloTree tree) {
		// determine all the groups of reticulate nodes
		final var reticulateNumber2Nodes = new HashMap<String, List<Node>>(); // maps each reticulate-node prefix to the set of all nodes that have it

		for (var v : tree.nodes()) {
			var label = tree.getLabel(v);
			if (label != null && !label.isEmpty()) {
				var reticulateLabel = PhyloTreeNetworkIOUtils.findReticulateLabel(label);
				if (reticulateLabel != null) {
					tree.setLabel(v, PhyloTreeNetworkIOUtils.removeReticulateNodeSuffix(label));
					var list = reticulateNumber2Nodes.computeIfAbsent(reticulateLabel, k -> new ArrayList<>());
					list.add(v);
				}
			}
		}

		// collapse all instances of a reticulate node into one node
		for (var reticulateNumber : reticulateNumber2Nodes.keySet()) {
			final var list = reticulateNumber2Nodes.get(reticulateNumber);
			if (!list.isEmpty()) {
				Node u = null;

				for (var v : list) {
					if (u == null) {
						u = v;
					} else {
						if (tree.getLabel(v) != null) {
							if (tree.getLabel(u) == null)
								tree.setLabel(u, tree.getLabel(v));
							else if (!tree.getLabel(u).equals(tree.getLabel(v)))
								tree.setLabel(u, tree.getLabel(u) + "," + tree.getLabel(v));
						}

						for (var e : v.adjacentEdges()) {
							final Edge f;
							if (e.getSource() == v) { /// attach child of v below u
								f = tree.newEdge(u, e.getTarget());
							} else { // attach parent of v above u
								f = tree.newEdge(e.getSource(), u);
							}
							if (tree.hasEdgeWeights() && tree.getEdgeWeights().containsKey(e))
								tree.setWeight(f, tree.getWeight(e));
							if (tree.hasEdgeConfidences() && tree.getEdgeConfidences().containsKey(e))
								tree.setConfidence(f, tree.getConfidence(e));
							if (tree.hasEdgeProbabilities() && tree.getEdgeProbabilities().containsKey(e))
								tree.setProbability(f, tree.getProbability(e));
							if (tree.isTransferAcceptorEdge(e)) {
								tree.setTransferAcceptor(f, true);
							}
							if (tree.isReticulateEdge(e))
								tree.setReticulate(f, true);
							tree.setLabel(f, tree.getLabel(e));
						}
						tree.deleteNode(v);
					}
				}

				var transferAcceptorEdge = new Single<Edge>();
				for (var e : u.inEdges()) {
					tree.setReticulate(e, true);
					if (tree.getWeight(e) > 0) {
						if (transferAcceptorEdge.isNull())
							transferAcceptorEdge.set(e);
						else {
							tree.setWeight(e, 0.0);
							System.err.println("Warning: node has more than one transfer-acceptor edge, will only use first");
						}
					}
				}
				if (transferAcceptorEdge.isNotNull()) {
					u.inEdgesStream(false).filter(e -> e != transferAcceptorEdge.get()).forEach(e -> tree.setWeight(e, -1.0));
				}
			}
		}
	}

	/**
	 * Writes a tree in bracket notation
	 *
	 * @param w            the writer
	 * @param writeWeights write edge weights or not
	 */
	public void write(PhyloTree tree, Writer w, boolean writeWeights) throws IOException {
		write(tree, w, writeWeights, false);
	}

	/**
	 * Writes a tree in bracket notation
	 *
	 * @param w                         the writer
	 * @param writeWeights              write edge weights or not
	 * @param writeEdgeLabelsAsComments write edge labels in brackets after the edge weight
	 */
	public void write(PhyloTree tree, Writer w, boolean writeWeights, boolean writeEdgeLabelsAsComments) throws IOException {
		write(tree, w, new OutputFormat(writeWeights, false, false, false, writeEdgeLabelsAsComments), null, null);
	}

	public void write(PhyloTree tree, Writer w, OutputFormat format) throws IOException {
		write(tree, w, format, null, null);
	}

	/**
	 * Writes a tree in bracket notation. Uses extended bracket notation to write reticulate network
	 *
	 * @param w             the writer
	 * @param nodeId2Number if non-null, will contain node-id to number mapping after call
	 * @param edgeId2Number if non-null, will contain edge-id to number mapping after call
	 */
	public void write(PhyloTree tree, Writer w, OutputFormat format, Map<Integer, Integer> nodeId2Number, Map<Integer, Integer> edgeId2Number) throws IOException {
		outputNodeNumber = 0;
		outputEdgeNumber = 0;

		if (tree.hasReticulateEdges()) {
			// following two lines enable us to write reticulate networks in Newick format
			outputNodeReticulationNumberMap = tree.newNodeIntArray();
			outputReticulationNumber = 0;
		}

		if (tree.getNumberOfEdges() > 0) {
			if (tree.getRoot() == null) {
				// as the old PhyloTree did, make the node of highest degree the root of an unrooted tree
				var root = tree.getFirstNode();
				for (Node v = root; v != null; v = v.getNext()) {
					if (v.getDegree() > root.getDegree())
						root = v;
				}
				tree.setRoot(root);
			}
			writeRec(tree, w, tree.getRoot(), null, format, nodeId2Number, edgeId2Number, getLabelForWriting(tree, tree.getRoot()));
		} else if (tree.getNumberOfNodes() == 1) {
			w.write("(" + getLabelForWriting(tree, tree.getFirstNode()) + ");");
			if (nodeId2Number != null)
				nodeId2Number.put(tree.getFirstNode().getId(), 1);
		} else
			w.write("();");

		if (outputNodeReticulationNumberMap != null)
			outputNodeReticulationNumberMap.clear();
	}

	/**
	 * Recursively writes a tree in bracket notation
	 */
	private void writeRec(PhyloTree tree, Writer outs, Node v, Edge e, OutputFormat format, Map<Integer, Integer> nodeId2Number, Map<Integer, Integer> edgeId2Number, String nodeLabel) throws IOException {
		if (nodeId2Number != null)
			nodeId2Number.put(v.getId(), ++outputNodeNumber);

		if (!isHideCollapsedSubTreeOnWrite() || tree.getLabel(v) == null || !tree.getLabel(v).endsWith(COLLAPSED_NODE_SUFFIX)) {
			if (v.getOutDegree() > 0) {
				outs.write("(");
				boolean first = true;
				for (Edge f : v.outEdges()) {
					if (edgeId2Number != null)
						edgeId2Number.put(f.getId(), ++outputEdgeNumber);

					if (first)
						first = false;
					else
						outs.write(",");

					final Node w = f.getTarget();

					if (tree.isReticulateEdge(f)) {
						boolean isAcceptorEdge = tree.isTransferAcceptorEdge(f);

						if (outputNodeReticulationNumberMap.get(w) == null) {
							outputNodeReticulationNumberMap.set(w, ++outputReticulationNumber);
							final String label;
							if (tree.getLabel(w) != null)
								label = getLabelForWriting(tree, w) + makeReticulateNodeLabel(isAcceptorEdge, outputNodeReticulationNumberMap.get(w));
							else
								label = makeReticulateNodeLabel(isAcceptorEdge, outputNodeReticulationNumberMap.get(w));

							writeRec(tree, outs, w, f, format, nodeId2Number, edgeId2Number, label);
						} else {
							String label;
							if (tree.getLabel(w) != null)
								label = getLabelForWriting(tree, w) + makeReticulateNodeLabel(isAcceptorEdge, outputNodeReticulationNumberMap.get(w));
							else
								label = makeReticulateNodeLabel(isAcceptorEdge, outputNodeReticulationNumberMap.get(w));

							outs.write(label);
							outs.write(getEdgeString(tree, format, f));
						}
					} else
						writeRec(tree, outs, w, f, format, nodeId2Number, edgeId2Number, getLabelForWriting(tree, w));
				}
				outs.write(")");
			}
			if (nodeLabel != null && !nodeLabel.isEmpty())
				outs.write(nodeLabel);
		}

		if (e != null) {
			outs.write(getEdgeString(tree, format, e));
		}
	}

	/**
	 * the weight, confidence and probability of an edge, and its label as a comment, as requested by the format
	 */
	private String getEdgeString(PhyloTree tree, OutputFormat format, Edge e) {
		var buf = new StringBuilder();
		var colons = 0;
		if (format.weights() && tree.getWeight(e) != -1.0) {
			if (tree.getEdgeWeights().containsKey(e)) {
				buf.append(StringUtils.removeTrailingZerosAfterDot(String.format(":%.8f", tree.getWeight(e))));
				colons++;
			}
		}
		if (format.confidenceUsingColon() && tree.hasEdgeConfidences() && tree.getEdgeConfidences().containsKey(e)) {
			while (colons < 2) {
				buf.append(":");
				colons++;
			}
			buf.append(StringUtils.removeTrailingZerosAfterDot(String.format("%.8f", tree.getConfidence(e))));
		}
		if (format.probabilityUsingColon() && tree.hasEdgeProbabilities() && tree.getEdgeProbabilities().containsKey(e)) {
			while (colons < 3) {
				buf.append(":");
				colons++;
			}
			buf.append(StringUtils.removeTrailingZerosAfterDot(String.format("%.8f", tree.getProbability(e))));
		}
		if (format.edgeLabelsAsComments() && tree.getLabel(e) != null) {
			buf.append("[").append(getLabelForWriting(tree, e)).append("]");
		}
		return buf.toString();
	}

	/**
	 * get the label to be used for writing. Will have single quotes, if label contains punctuation character or white space
	 */
	private String getLabelForWriting(PhyloTree tree, Node v) {
		var label = cleanLabelsOnWrite ? StringUtils.getCleanLabelForNewick(tree.getLabel(v)) : tree.getLabel(v);
		if (label != null) {
			for (int i = 0; i < label.length(); i++) {
				if (punctuationCharacters.indexOf(label.charAt(i)) != -1 || Character.isWhitespace(label.charAt(i)))
					return "'" + label + "'";
			}
		}
		return label;
	}

	/**
	 * get the label to be used for writing. Will have single quotes, if label contains punctuation character or white space
	 */
	private String getLabelForWriting(PhyloTree tree, Edge e) {
		var label = cleanLabelsOnWrite ? StringUtils.getCleanLabelForNewick(tree.getLabel(e)) : tree.getLabel(e);
		if (label != null) {
			for (int i = 0; i < label.length(); i++) {
				if (punctuationCharacters.indexOf(label.charAt(i)) != -1 || Character.isWhitespace(label.charAt(i)))
					return "'" + label + "'";
			}
		}
		return label;
	}

	/**
	 * the label of a reticulate node, #Hn, or ##Hn for the target of a transfer-acceptor edge
	 * (as PhyloTreeNetworkIOUtils.makeReticulateNodeLabel, which is not public)
	 */
	private static String makeReticulateNodeLabel(boolean asAcceptorEdgeTarget, int number) {
		if (asAcceptorEdgeTarget)
			return "##H" + number;
		else
			return "#H" + number;
	}

	/**
	 * is v an unlabeled node of degree 2? (an empty label counts as none, a blank one does not, as in the old PhyloTree)
	 */
	private static boolean isUnlabeledDiVertex(PhyloTree tree, Node v) {
		return v.getDegree() == 2 && (tree.getLabel(v) == null || tree.getLabel(v).isEmpty());
	}

	public boolean isAllowMultiLabeledNodes() {
		return allowMultiLabeledNodes;
	}

	public void setAllowMultiLabeledNodes(boolean allowMultiLabeledNodes) {
		this.allowMultiLabeledNodes = allowMultiLabeledNodes;
	}

	/**
	 * hide collapsed subtrees on write?
	 */
	public boolean isHideCollapsedSubTreeOnWrite() {
		return hideCollapsedSubTreeOnWrite;
	}

	/**
	 * hide collapsed subtrees on write?
	 */
	public void setHideCollapsedSubTreeOnWrite(boolean hideCollapsedSubTreeOnWrite) {
		this.hideCollapsedSubTreeOnWrite = hideCollapsedSubTreeOnWrite;
	}

	/**
	 * did the last tree parsed contain the same label more than once (only detected when multi-labeled nodes are not allowed)?
	 */
	public boolean isInputHasMultiLabels() {
		return inputHasMultiLabels;
	}
}
