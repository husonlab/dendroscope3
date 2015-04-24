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

package dendroscope.io;

import jloda.util.ProgramProperties;

import java.io.StringWriter;

/**
 * output of distance in nexus format
 * Daniel Huson, 10.2011
 */
public class DistanceOutput {
    /**
     * write as nexus block
     *
     * @param distances
     * @param names
     * @return nexus distance block
     */
    public static String toNexusString(double[][] distances, String[] names) {
        StringWriter writer = new StringWriter();
        writer.write("#nexus\n");
        if (ProgramProperties.getProgramName() != null && ProgramProperties.getProgramName().length() > 0)
            writer.write("[!Computed by " + ProgramProperties.getProgramName() + "]\n");
        writer.write("begin taxa;dimensions ntax=" + names.length + ";end;\n");
        writer.write("begin distances;\n");
        writer.write("format labels triangle=both diagonal;\n");
        writer.write("matrix\n");
        for (int i = 0; i < distances.length; i++) {
            writer.write(String.format("\t'%s'\t", names[i].replaceAll("\\[", "").replaceAll("\\]", "")));
            for (int j = 0; j < distances.length; j++) {
                writer.write(String.format(" %2.2f", distances[i][j]));
            }
            writer.write("\n");
        }
        writer.write(";\nend;\n");
        return writer.toString();
    }

    /**
     * write as simple matrix
     *
     * @param distances
     * @param names
     * @return simple matrix
     */
    public static String toSimpleString(double[][] distances, String[] names) {
        String result = "";
        for (int i = 0; i < distances.length; i++) {
            result += String.format("%20s", names[i]);
            for (int j = 0; j < distances[0].length; j++) {
                result += String.format(" %2.2f", distances[i][j]);
            }
            result += "\n";
        }
        return result;
    }
}
