/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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


    
