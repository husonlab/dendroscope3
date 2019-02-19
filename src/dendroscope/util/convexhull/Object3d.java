/**
 * Object3d.java 
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
package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

/**
 * interface for 3d objects that can be rendered on screen
 */
public interface Object3d {
    public abstract Point3d centre();

    public abstract void setCentre(Point3d c);

    public abstract void transform(Matrix3D T);

    public abstract String id();

    public void select(int n);

}
