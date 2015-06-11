/**
 * ReplaceMaxCommonChains.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ReplaceMaxCommonChains {

    public void run(HybridTree t1, Vector<Vector<Node>> t1Chains,
                    HybridTree t2, Vector<Vector<Node>> t2Chains, ReplacementInfo rI) {

        Hashtable<String, Vector<String>> t1LabelToChainString = new Hashtable<>();
        createChainStrings(t1, t1Chains, t1LabelToChainString);

        Hashtable<String, Vector<String>> t2LabelToChainString = new Hashtable<>();
        createChainStrings(t2, t2Chains, t2LabelToChainString);

        Hashtable<Vector<Node>, Vector<Vector<String>>> chainToCommonChains = new Hashtable<>();
        findCommonChains(t2, t2Chains, t1LabelToChainString,
                chainToCommonChains);

        replaceCommonChains(t1, t2, chainToCommonChains, rI);

    }

    private void replaceCommonChains(
            HybridTree t1,
            HybridTree t2,
            Hashtable<Vector<Node>, Vector<Vector<String>>> chainToCommonChains,
            ReplacementInfo rI) {

        Hashtable<String, Node> t1LeafLabelToParent = new Hashtable<>();
        Iterator<Node> itNode = t1.computeSetOfLeaves().iterator();
        while (itNode.hasNext()) {
            Node v = itNode.next();
            String label = t1.getLabel(v);
            t1LeafLabelToParent.put(label,
                    v.getInEdges().next().getSource());
        }

        Hashtable<String, Node> t2LeafLabelToParent = new Hashtable<>();
        itNode = t2.computeSetOfLeaves().iterator();
        while (itNode.hasNext()) {
            Node v = itNode.next();
            String label = t2.getLabel(v);
            t2LeafLabelToParent.put(label,
                    v.getInEdges().next().getSource());
        }

        for (Vector<Node> chain : chainToCommonChains.keySet()) {
            Vector<Vector<String>> allChainLabels = chainToCommonChains
                    .get(chain);

            for (Vector<String> chainLabels : allChainLabels) {
                t1.replaceCommonChain(
                        t1LeafLabelToParent.get(chainLabels.get(0)),
                        chainLabels);
                t2.replaceCommonChain(
                        t2LeafLabelToParent.get(chainLabels.get(0)),
                        chainLabels);

                rI.putStartLabelToChain(chainLabels.lastElement(), chainLabels);
                rI.putStartLabelToEndLabel(chainLabels.lastElement(),
                        chainLabels.firstElement());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void findCommonChains(HybridTree t2, Vector<Vector<Node>> t2Chains,
                                  Hashtable<String, Vector<String>> t1LabelToChainString,
                                  Hashtable<Vector<Node>, Vector<Vector<String>>> chainToCommonChains) {

        for (Vector<Node> chain : t2Chains) {

            Vector<String> commonChain = new Vector<>();

            String s = "";
            int k = 0;

            Vector<Node> childs = t2.getLeaves(chain.get(0));
            if (childs.size() == 2) {
                Node child1 = childs.get(0);
                Node child2 = childs.get(1);

                String label1 = t2.getLabel(child1);
                String label2 = t2.getLabel(child2);

                boolean b1 = t1LabelToChainString.containsKey(label1);
                boolean b2 = t1LabelToChainString.containsKey(label2);

                if (!b1 && !b2)
                    k = 1;
                else if (b1 && !b2) {
                    s = s.concat("+" + label1 + "+");
                    commonChain.add(label1);
                    k = 1;
                } else if (!b1 && b2) {
                    s = s.concat("+" + label2 + "+");
                    commonChain.add(label2);
                    k = 1;
                } else {
                    Node c = t2.getLeaves(chain.get(1)).get(0);
                    String l = t2.getLabel(c);

                    String s1 = "+" + label1 + "+" + l + "+";
                    String s2 = "+" + label2 + "+" + l + "+";

                    if (t1LabelToChainString.containsKey(l)) {
                        Vector<String> chainStrings = t1LabelToChainString
                                .get(l);

                        for (String chainString : chainStrings) {

                            b1 = chainString.contains(s1);
                            b2 = chainString.contains(s2);

                            if (b1) {
                                s = s.concat(s1);
                                commonChain.add(label1);
                                commonChain.add(l);
                                break;
                            } else if (b2) {
                                s = s.concat(s2);
                                commonChain.add(label2);
                                commonChain.add(l);
                                break;
                            }

                        }

                    }
                    k = 2;
                }
            }

            for (int i = k; i < chain.size(); i++) {

                childs = t2.getLeaves(chain.get(i));

                if (childs.size() == 1) {
                    Node child = childs.get(0);
                    String label = t2.getLabel(child);

                    if (t1LabelToChainString.containsKey(label)) {

                        Vector<String> chainStrings = t1LabelToChainString
                                .get(label);
                        boolean chainEnded = true;

                        for (String chainString : chainStrings) {
                            String newS;
                            if (s.length() == 0)
                                newS = "+" + label + "+";
                            else
                                newS = s + label + "+";
                            if (chainString.contains(newS)) {
                                if (s.length() == 0)
                                    s = "+" + s.concat(label) + "+";
                                else
                                    s = s.concat(label + "+");
                                commonChain.add(label);
                                chainEnded = false;
                                break;
                            }
                        }
                        if (chainEnded || i == chain.size() - 1) {
                            if (commonChain.size() > 2) {
                                if (chainToCommonChains.containsKey(chain)) {
                                    Vector<Vector<String>> vec = (Vector<Vector<String>>) chainToCommonChains
                                            .get(chain).clone();
                                    chainToCommonChains.remove(chain);
                                    vec.add((Vector<String>) commonChain
                                            .clone());
                                    chainToCommonChains.put(chain, vec);
                                } else {
                                    Vector<Vector<String>> vec = new Vector<>();
                                    vec.add((Vector<String>) commonChain
                                            .clone());
                                    chainToCommonChains.put(chain, vec);
                                }
                            }
                            s = "";
                            commonChain.clear();
                        }
                    } else
                        break;
                } else
                    System.err.println("ERROR: NODE HAS TWO CHILDREN!");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createChainStrings(HybridTree t, Vector<Vector<Node>> chains,
                                    Hashtable<String, Vector<String>> labelToChainString) {

        for (Vector<Node> chain : chains) {
            Node v = chain.get(0);
            Vector<Node> leaves = t.getLeaves(v);
            for (Node l : leaves) {
                String s = createChainString(t, t.getLabel(l), chain,
                        labelToChainString);

                if (!labelToChainString.containsKey(t.getLabel(l))) {
                    Vector<String> vec = new Vector<>();
                    vec.add(s);
                    labelToChainString.put(t.getLabel(l), vec);
                } else {
                    Vector<String> vec = (Vector<String>) labelToChainString
                            .get(t.getLabel(l)).clone();
                    labelToChainString.remove(t.getLabel(l));
                    vec.add(s);
                    labelToChainString.put(t.getLabel(l), vec);
                }

            }

        }
    }

    @SuppressWarnings("unchecked")
    private String createChainString(HybridTree t, String label,
                                     Vector<Node> chain,
                                     Hashtable<String, Vector<String>> labelToChainString) {
        String s = "+" + label;
        for (int i = 1; i < chain.size(); i++) {
            Node l = (t.getLeaves(chain.get(i))).get(0);
            s = s.concat("+" + t.getLabel(l));
        }
        s = s.concat("+");
        for (int i = 1; i < chain.size(); i++) {
            Node l = (t.getLeaves(chain.get(i))).get(0);
            if (!labelToChainString.containsKey(t.getLabel(l))) {
                Vector<String> vec = new Vector<>();
                vec.add(s);
                labelToChainString.put(t.getLabel(l), vec);
            } else {
                Vector<String> vec = (Vector<String>) labelToChainString.get(
                        t.getLabel(l)).clone();
                labelToChainString.remove(t.getLabel(l));
                vec.add(s);
                labelToChainString.put(t.getLabel(l), vec);
            }
        }
        return s;
    }
}
