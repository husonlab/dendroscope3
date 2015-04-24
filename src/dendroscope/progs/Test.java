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
