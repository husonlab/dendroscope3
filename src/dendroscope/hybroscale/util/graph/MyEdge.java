/*
 *   MyEdge.java Copyright (C) 2020 Daniel H. Huson
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

public class MyEdge {

    private String label;
    private Object info;
    private MyNode source, target;
    private MyGraph owner;
    private Double weight;
    private boolean isSpecial, isCritical = false, isShifted = false;

    public MyEdge(MyGraph owner, MyNode source, MyNode target) {
        this.owner = owner;
        this.source = source;
        this.target = target;
        weight = 1.0;
        isSpecial = false;
    }

    public MyNode getSource() {
        return source;
    }

    public MyNode getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Object getInfo() {
        return info;
    }

    public void setInfo(Object info) {
        this.info = info;
    }

    public MyGraph getOwner() {
        return owner;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setSpecial(boolean b) {
        isSpecial = b;
    }

    public boolean isSpecial() {
        return isSpecial;
    }

    public String getNewickString() {
        String s = "";
        if (weight != null)
            s = s.concat(":" + weight);
        return s;
    }

    public String getMyNewickString() {
        String s = "";
        if (weight != null)
            s = s.concat(":" + weight);
        if (info != null) {
            s = s.concat("[" + processInfo(info.toString()) + "]");
        }
        return s;
    }

    private String processInfo(String edgeLabel) {
        String s = edgeLabel;
        s = s.replace("[", "'");
        s = s.replace("]", "'");
        return s;
    }

    public void setCritical(boolean b) {
        isCritical = b;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public boolean isShifted() {
        return isShifted;
    }

    public void setShifted(boolean isShifted) {
        this.isShifted = isShifted;
    }

}
