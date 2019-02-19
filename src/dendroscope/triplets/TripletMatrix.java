/**
 * TripletMatrix.java 
 * Copyright (C) 2019 Daniel H. Huson
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
package dendroscope.triplets;

import java.util.Vector;

/**
 * This class encode triplets
 *
 * @author celine scornavacca, 6.2010
 */

public class TripletMatrix {

    private Vector<Vector<Vector<Integer>>> GroupOfTriplets;
    private int dim; // the dimension of the matrix dim * dim * dim


    public void setDim(int dimTrees) {
        dim = dimTrees;
        GroupOfTriplets = new Vector<>();
        GroupOfTriplets.setSize(dimTrees);
        for (int i = 0; i < dimTrees; ++i) {
            GroupOfTriplets.setElementAt(new Vector<Vector<Integer>>(), i);
            GroupOfTriplets.get(i).setSize(dimTrees);
            for (int j = 0; j < dimTrees; ++j) {
                GroupOfTriplets.get(i).setElementAt(new Vector<Integer>(), j);
                GroupOfTriplets.get(i).get(j).setSize(dimTrees);
                for (int z = 0; z < dimTrees; ++z)
                    GroupOfTriplets.get(i).get(j).set(z, 0);
            }
        }
    }


    public void deleteMatrix() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                (GroupOfTriplets.get(i).get(j)).removeAllElements();
            }
            (GroupOfTriplets.get(i)).removeAllElements();
        }
        (GroupOfTriplets).removeAllElements();
    }


    int getDim() {
        return dim;
    }

    int getValue(int i, int j, int z) {
        if (i <= j)
            return GroupOfTriplets.get(i).get(j).get(z);
        else
            return GroupOfTriplets.get(j).get(i).get(z);
    }

    void setValue(int i, int j, int z, int value) {
        if (i <= j)
            GroupOfTriplets.get(i).get(j).set(z, value);
        else
            GroupOfTriplets.get(j).get(i).set(z, value);
    }


    public void addOne(int i, int j, int z) {
        setValue(i, j, z, getValue(i, j, z) + 1);
    }

    void eraseTriplet(int i, int j, int z) {
        setValue(i, j, z, 0);
    }


    void print() {

        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                for (int z = 0; z < dim; z++) {
                    if (GroupOfTriplets.get(i).get(j).get(z) != 0) {
                        System.out.println(i + "," + j + "|" + z);
                    }
                }
            }
        }
    }


    void copy(TripletMatrix R) {
        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                for (int z = 0; z < dim; z++) {
                    if (getValue(i, j, z) != 0)
                        R.setValue(i, j, z, getValue(i, j, z));

                }
            }
        }
    }

}


    
