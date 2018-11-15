/**
 * Triple.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.algorithms.gallnet;

import dendroscope.consensus.Cluster;

import java.util.BitSet;
import java.util.Comparator;

/**
 * a triple of three disjoint sets
 * Daniel Huson, 10.2007
 */
public class Triple {
    private BitSet A;
    private BitSet B;
    private BitSet C;

    /**
     * constructs a new triple consisting of three empty sets
     */
    public Triple() {
        A = new BitSet();
        B = new BitSet();
        C = new BitSet();
    }

    /**
     * constructs a new ordered triple consisting of sets A, B, C
     *
     * @param A
     * @param B
     * @param C
     */
    public Triple(BitSet A, BitSet B, BitSet C) {
        setTriple(A, B, C);
    }

    /**
     * computes the intersection triple, if clusters are incompatible, otherwise null
     *
     * @param cluster1
     * @param cluster2
     * @return intersection triple or null
     */
    public static Triple computeIncompatibilityTriple(BitSet cluster1, BitSet cluster2) {
        Triple triple = new Triple(Cluster.setminus(cluster1, cluster2), Cluster.intersection(cluster1, cluster2),
                Cluster.setminus(cluster2, cluster1));
        if (triple.A.cardinality() > 0 && triple.B.cardinality() > 0 && triple.C.cardinality() > 0
                && !triple.A.equals(triple.B) && !triple.A.equals(triple.C) && !triple.B.equals(triple.C))
            return triple;
        else
            return null;
    }

    /**
     * sets the triple
     *
     * @param A
     * @param B
     * @param C
     */
    public void setTriple(BitSet A, BitSet B, BitSet C) {
        int a = A.nextSetBit(0);
        int b = B.nextSetBit(0);
        int c = C.nextSetBit(0);

        if (a == b || a == c || b == c)
            throw new RuntimeException("Triple: sets share minimal element: " + a + " " + b + " " + c);

        if (a < b && b < c) {
            this.A = A;
            this.B = B;
            this.C = C;
        } else if (a < c && c < b) {
            this.A = A;
            this.B = C;
            this.C = B;
        } else if (b < a && a < c) {
            this.A = B;
            this.B = A;
            this.C = C;
        } else if (b < c && c < a) {
            this.A = B;
            this.B = C;
            this.C = A;
        } else if (c < a && a < b) {
            this.A = C;
            this.B = A;
            this.C = B;
        } else // if(c<b && b<a)
        {
            this.A = C;
            this.B = B;
            this.C = A;
        }
    }


    /**
     * get a comparator for two ordered triples. Order by minimal size of three sets first,
     * so  triple of size 5,3,6 comes before 8,2,9
     *
     * @return comparator
     */
    public static Comparator<Triple> getComparator() {
        return new Comparator<Triple>() {
            public int compare(Triple o1, Triple o2) {

                int min1 = Math.min(o1.getA().cardinality(), Math.min(o1.getB().cardinality(), o1.getC().cardinality()));
                int min2 = Math.min(o2.getA().cardinality(), Math.min(o2.getB().cardinality(), o2.getC().cardinality()));
                if (min1 > min2)
                    return -1;
                else if (min2 > 1)
                    return 1;


                int c = Cluster.compare(o1.getA(), o2.getA());
                if (c != 0)
                    return c;
                c = Cluster.compare(o1.getB(), o2.getB());
                if (c != 0)
                    return c;
                c = Cluster.compare(o1.getC(), o2.getC());
                return c;
            }
        };
    }

    public BitSet getA() {
        return A;
    }

    public BitSet getB() {
        return B;
    }

    public BitSet getC() {
        return C;
    }


    /**
     * gets string representation
     *
     * @return string
     */
    public String toString() {
        return A.toString() + " | " + B.toString() + " | " + C.toString();
    }

}

