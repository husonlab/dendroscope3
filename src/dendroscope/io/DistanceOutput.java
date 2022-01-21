/*
 * DistanceOutput.java Copyright (C) 2022 Daniel H. Huson
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
