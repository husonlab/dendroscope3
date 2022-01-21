/*
 * Partition.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.consensus;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 * a weighted partition of numbers
 * Daniel Huson, 10.2007
 */
public class Partition {
    int weight;
    BitSet[] blocks;

    /**
     * construct a new partition
     */
    public Partition() {
        weight = 0;
        blocks = new BitSet[0];
    }

    /**
     * construct a new partition
     *
     * @param blocks to use
     */
    public Partition(BitSet[] blocks) {
        weight = 0;
        this.blocks = new BitSet[blocks.length];
        System.arraycopy(blocks, 0, this.blocks, 0, blocks.length);
        Arrays.sort(this.blocks, getBitSetComparator());
    }

    /**
     * add a new block to the partition
     *
     * @param block
     */
    public void addBlock(BitSet block) {
        BitSet[] newblocks = new BitSet[blocks.length + 1];
        System.arraycopy(blocks, 0, newblocks, 0, blocks.length);
        newblocks[blocks.length] = block;
        Arrays.sort(newblocks, getBitSetComparator());
        blocks = newblocks;
    }

    /**
     * clear the partition
     */
    public void clear() {
        blocks = new BitSet[0];
        weight = 0;
    }

    /**
     * get the number of blocks
     *
     * @return size
     */
    public int size() {
        return blocks.length;
    }

    /**
     * get the weight
     *
     * @return weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * set the weight
     *
     * @param weight
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * get a iterator over all blocks
     *
     * @return iterator
     */
    public Iterator iterator() {
        return new Iterator() {
            int i = 0;

            public boolean hasNext() {
                return i < blocks.length;
            }

            public Object next() {
                return blocks[i++];
            }

            public void remove() {
            }
        };
    }

    /**
     * gets a bit set comparator
     *
     * @return bit set comparator
     */
    private static Comparator<BitSet> getBitSetComparator() {
        return new Comparator<BitSet>() {
            public int compare(BitSet b1, BitSet b2) {
                int i1 = b1.nextSetBit(0);
                int i2 = b2.nextSetBit(0);
                while (i1 != -1 && i2 != -1) {
                    if (i1 < i2)
                        return -1;
                    else if (i1 > i2)
                        return 1;
                    i1 = b1.nextSetBit(i1 + 1);
                    i2 = b2.nextSetBit(i2 + 1);
                }
                if (i1 < i2)
                    return -1;
                else if (i1 > i2)
                    return 1;
                else return 0;
            }
        };
    }

    /**
     * comparator by descending weight
     *
     * @return comparator
     */
    public static Comparator<Partition> getComparatorByDescendingWeight() {
        return new Comparator<Partition>() {
            public int compare(Partition b1, Partition b2) {

                if (b1.getWeight() > b2.getWeight())
                    return -1;
                else if (b1.getWeight() < b2.getWeight())
                    return 1;
                else {
                    int top = Math.min(b1.blocks.length, b2.blocks.length);

                    for (int i = 0; i < top; i++) {
                        int c = getBitSetComparator().compare(b1.blocks[i], b2.blocks[i]);
                        if (c != 0)
                            return c;
                    }
                    if (b1.blocks.length < b2.blocks.length)
                        return -1;
                    else if (b1.blocks.length > b2.blocks.length)
                        return 1;
                    else
                        return 0;
                }

            }

        };
    }

    /**
     * comparator by blocks
     *
     * @return comparator
     */
    public static Comparator<Partition> getComparatorByBlocks() {
        return new Comparator<Partition>() {
            public int compare(Partition b1, Partition b2) {
                int top = Math.min(b1.blocks.length, b2.blocks.length);

                for (int i = 0; i < top; i++) {
                    int c = getBitSetComparator().compare(b1.blocks[i], b2.blocks[i]);
                    if (c != 0)
                        return c;
                }
                if (b1.blocks.length < b2.blocks.length)
                    return -1;
                else if (b1.blocks.length > b2.blocks.length)
                    return 1;
                else
                    return 0;
            }
        };
    }

    /**
     * are two partitions equal ignoring weights
     *
     * @param part
     * @return true, if equal
     */
    public boolean equals(Partition part) {
        return getComparatorByBlocks().compare(this, part) == 0;
    }

    /**
     * as string
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < blocks.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(blocks[i]);
        }
        buf.append(": ").append(getWeight());
        return buf.toString();
    }

    /**
     * determines whether partition is compatible will all given splits. This means that one part of each split
     * is contained in one of the partition parts
     *
     * @param splits
     * @return true, if compatible with all splits
     */
    public boolean compatibleWithAllSplits(SplitSystem splits) {
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            if (!compatibleWith(split))
                return false;
        }
        return true;
    }

    /**
     * is partition compatible with this split, i.e. does one of the parts contain one of the split parts?
     *
     * @param split
     * @return true, if compatible
     */
    private boolean compatibleWith(Split split) {
        for (BitSet block : blocks) {
            if (Cluster.contains(block, split.getA()) || Cluster.contains(block, split.getB()))
                return true;
        }
        return false;
    }

    /**
     * gets a copy of the blocks
     *
     * @return blocks
     */
    public BitSet[] getBlocks() {
        BitSet[] result = new BitSet[blocks.length];
        System.arraycopy(blocks, 0, result, 0, blocks.length);
        return result;
    }

    /**
     * gets a copy of block p
     *
     * @param p
     * @return copy of block p
     */
    public BitSet getBlock(int p) {
        return (BitSet) blocks[p].clone();
    }

    /**
     * returns the split partition contained in the partition, or null
     *
     * @param split
     * @return split part or null
     */
    public BitSet[] getSplitPartsContainedInPartition(Split split) {
        if (blocks.length == 2 && (Cluster.equals(split.getA(), blocks[0]) || Cluster.equals(split.getA(), blocks[1]))) {
            BitSet[] result = new BitSet[2];
            result[0] = (BitSet) split.getA().clone();
            result[1] = (BitSet) split.getB().clone();
            return result;

        }
        for (BitSet block : blocks) {
            if (Cluster.contains(block, split.getA()))
                return new BitSet[]{split.getA()};
            else if (Cluster.contains(block, split.getB()))
                return new BitSet[]{split.getB()};
        }
        return null;

    }
}
