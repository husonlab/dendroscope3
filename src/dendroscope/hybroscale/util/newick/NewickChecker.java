/*
 *   NewickChecker.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.util.newick;

import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;

public class NewickChecker extends Observable {

    private Hashtable<Integer, Integer> sqBracketToSqBracket, bracketToBracket;
    private Hashtable<String, Integer> labelToIndex;
    private final char[] metaCharacters = {'(', ')', '[', ']', ':', ',', ';', '#'};

    public boolean checkString(String s, int offset) {

        labelToIndex = new Hashtable<String, Integer>();
        sqBracketToSqBracket = new Hashtable<Integer, Integer>();
        bracketToBracket = new Hashtable<Integer, Integer>();

        parseParseSquaredBrackets(s);
        String err = parseBrackets(s);

        if (err.isEmpty())
            err = parseLabels();

        if (err.isEmpty()) {
            setChanged();
            notifyObservers(err);
            return true;
        } else {
            err = err.concat("|" + offset);
            setChanged();
            notifyObservers(err);
            return false;
        }

    }

    private void parseParseSquaredBrackets(String s) {
        Vector<Integer> openPos = new Vector<Integer>();
        for (int index = 0; index < s.length(); index++) {
            if (s.charAt(index) == '[')
                openPos.add(index);
            else if (s.charAt(index) == ']') {
                sqBracketToSqBracket.put(openPos.lastElement(), index);
                openPos.remove(openPos.lastElement());
            }
        }
    }

    private String parseBrackets(String s) {
        Vector<Integer> openPos = new Vector<Integer>();
        String label = "";
        int startIndex = -1;
        for (int index = 0; index < s.length(); index++) {
            if (index == s.length() - 1) {
                if (s.charAt(index) != ';')
                    return -1 + "|" + -1 + "|Missing terminal semicolon!";
            } else if (s.charAt(index) == ';')
                return -1 + "|" + -1 + "|Only one newick-string per line!";
            else if (s.charAt(index) == ' ') {
                return index + "|" + (index + 1) + "|No white-spaces permitted!";
            } else if (s.charAt(index) == '(') {
                openPos.add(index);
                labelToIndex.put(label, startIndex);
                label = "";
                startIndex = -1;
            } else if (s.charAt(index) == ')') {
                if (openPos.isEmpty())
                    return index + "|" + (index + 1) + "|Missing opening bracket!";
                bracketToBracket.put(openPos.lastElement(), index);
                openPos.remove(openPos.lastElement());
                labelToIndex.put(label, startIndex);
                label = "";
                startIndex = -1;
            } else if (s.charAt(index) == '[') {
                if (sqBracketToSqBracket.containsKey(index))
                    index = sqBracketToSqBracket.get(index);
                else
                    return index + "|" + (index + 1) + "|Missing closing squared bracket!";
            } else if (s.charAt(index) == ',') {
                labelToIndex.put(label, startIndex);
                label = "";
                startIndex = -1;
            } else {
                if (startIndex == -1)
                    startIndex = index;
                label = label.concat("" + s.charAt(index));
            }
        }
        if (!label.isEmpty())
            labelToIndex.put(label, startIndex);
        if (!openPos.isEmpty())
            return openPos.get(0) + "|" + (openPos.get(0) + 1) + "|Missing closing bracket!";
        return "";
    }

    private String parseLabels() {
        for (String label : labelToIndex.keySet()) {
            String[] content = new NewickParser().parseLabel(label);
            int startIndex = labelToIndex.get(label);
            if (content[0] != null) {
                for (int i = 0; i < content[0].length(); i++) {
                    char c = content[0].charAt(i);
                    if (isMetaCharacter(c))
                        return startIndex + "|" + (startIndex + label.length())
                                + "|Invalid Label - illegal character '" + c;
                }
            }
            if (content[1] != null) {
                if (content[1].isEmpty())
                    return startIndex + "|" + (startIndex + label.length()) + "|Invalid HTag";
                if (content[1].charAt(0) != 'H')
                    return startIndex + "|" + (startIndex + label.length()) + "|Invalid HTag - missing 'H'";
                if (content[1].length() < 2)
                    return startIndex + "|" + (startIndex + label.length()) + "|Invalid HTag - missing digit";
                for (int i = 1; i < content[1].length(); i++) {
                    char c = content[1].charAt(i);
                    if (!Character.isDigit(c))
                        return startIndex + "|" + (startIndex + label.length())
                                + "|Invalid HTag - value must be a digit";
                }
            }
            if (content[2] != null) {
                try {
                    Double.parseDouble(content[2]);
                } catch (NumberFormatException e) {
                    return startIndex + "|" + (startIndex + label.length()) + "|Invalid Weight - value is not a double";
                }
            }
            if (content[3] != null) {
                if (content[3].charAt(content[3].length() - 1) != ']')
                    return startIndex + "|" + (startIndex + label.length()) + "|Invalid Info - missing character ']'";
            }
        }
        return "";
    }

    private boolean isMetaCharacter(char c1) {
        for (char c2 : metaCharacters) {
            if (c2 == c1)
                return true;
        }
        return false;
    }
}
