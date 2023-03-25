/*
 * IntegerVariable.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.util;

/**
 * a mutable integer
 * Daniel Huson, 9.2008
 */
public class IntegerVariable {
    private Integer value;

    public IntegerVariable() {
        value = 0;
    }

    public IntegerVariable(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int setValue(int value) {
        this.value = value;
        return value;
    }

    public String toString() {
        return value.toString();
    }

    public static IntegerVariable valueOf(String str) {
        return new IntegerVariable(Integer.parseInt(str));
    }

    public int increment() {
        return setValue(getValue() + 1);
    }

    public int decrement() {
        return setValue(getValue() - 1);
    }
}
