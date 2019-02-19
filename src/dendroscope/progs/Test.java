/**
 * Test.java 
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
package dendroscope.progs;

import java.util.Random;

/**
 * DESCRIPTION
 * Daniel Huson, DATE
 */
public class Test {
    public static void main(String[] args) {

        String a = "aaa";
        String b = "aaa";

        String c = (new String("aaa"));
        String d = "a" + "a";
        d += "a";

        System.err.println("a==b?" + (a == b));
        System.err.println("a==c?" + (a == c));
        System.err.println("a==d?" + (a == d));

        System.err.println("a eq b?" + (a.equals(b)));
        System.err.println("a eq c?" + (a.equals(c)));
        System.err.println("a eq d?" + (a.equals(d)));

        {
            long start = System.currentTimeMillis();

            int count = 0;

            Random rand = new Random();

            rand.setSeed(1);
            for (int i = 0; i < 10000000; i++) {
                String p = String.valueOf(rand.nextLong());
                String q = p + "x";

                if (p.hashCode() == q.hashCode() && p.equals(q))
                    count++;

            }
            System.err.println("Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
        }

        {
            long start = System.currentTimeMillis();
            int count = 0;

            Random rand = new Random();
            rand.setSeed(1);

            for (int i = 0; i < 10000000; i++) {
                String p = String.valueOf(rand.nextLong());
                String q = p + "x";

                if (p.equals(q))
                    count++;

            }
            System.err.println("Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
        }


    }
}
