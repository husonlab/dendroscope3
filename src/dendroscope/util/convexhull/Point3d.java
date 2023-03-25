/*
 *   Point3d.java Copyright (C) 2023 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.util.convexhull;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;

public class Point3d {
    public double[] v;
    public static Point3d o = new Point3d(0, 0, 0);
    public static Point3d i = new Point3d(1, 0, 0);
    public static Point3d j = new Point3d(0, 1, 0);
    public static final Point3d k = new Point3d(0, 0, 1);
    public static final Point3d ijk = new Point3d(1, 1, 1);

    public Point3d() {
        this.v = new double[3];
    }

    public Point3d(double x, double y, double z) {
        this();
        this.v[0] = x;
        this.v[1] = y;
        this.v[2] = z;
    }

    public static Point3d fromSpherical(double r, double theta, double phi) {
        return new Point3d(r * Math.cos(theta) * Math.cos(phi),
                r * Math.sin(theta) * Math.cos(phi),
                r * Math.sin(phi));
    }

    public static Point3d fromCylindrical(double r, double theta, double y) {
        return new Point3d(r * Math.cos(theta),
                y,
                r * Math.sin(theta));
    }

    public double x() {
        return v[0];
    }

    public double y() {
        return v[1];
    }

    public double z() {
        return v[2];
    }

    public double theta() {
        return Math.atan2(v[0], v[2]);
    }

    public double r() {
        return Math.sqrt(v[0] * v[0] + v[2] * v[2]);
    }


    public String toString() {
        String s = "v[" + (float) v[0] + "," + (float) v[1] + "," + (float) v[2] + "]";
        return s;
    }

    public static Point3d fromString(String s) throws NumberFormatException {
        StringTokenizer st = new StringTokenizer(s, "[,]");
        try {
			st.nextToken(); //get rid of leading v
			double x = Double.valueOf(st.nextToken());
			double y = Double.valueOf(st.nextToken());
			double z = Double.valueOf(st.nextToken());
			return new Point3d(x, y, z);
		} catch (NoSuchElementException e) {
            throw new NumberFormatException();
        }
    }

    public String toVRML() {
        return (float) (x()) + " " + (float) (y()) + " " + (float) (z());
    }

    public Point3d add(Point3d x) {
        Point3d a = new Point3d();
        for (int i = 0; i < 3; i++) {
            a.v[i] = v[i] + x.v[i];
        }
        return a;
    }

    public Point3d subtract(Point3d x) {
        Point3d a = new Point3d();
        for (int i = 0; i < 3; i++) {
            a.v[i] = v[i] - x.v[i];
        }
        return a;
    }

    public Point3d scale(double x) {
        Point3d a = new Point3d();
        for (int i = 0; i < 3; i++) {
            a.v[i] = x * v[i];
        }
        return a;
    }

    public Point3d scale(double x, double y, double z) {
        return new Point3d(x * v[0], y * v[1], z * v[2]);
    }

    public double dot(Point3d x) {
        double d = 0;
        for (int i = 0; i < 3; i++) {
            d += v[i] * x.v[i];
        }
        return d;
    }

    public Point3d normalize() {
        return scale(1 / length());
    }

    public double length() {
        return Math.sqrt(dot(this));
    }


    public Point3d cross(Point3d x) {
        return new Point3d(v[1] * x.v[2] - x.v[1] * v[2],
                v[2] * x.v[0] - x.v[2] * v[0],
                v[0] * x.v[1] - x.v[0] * v[1]);
    }

    private static final Random random = new Random();

    /**
     * Set seed of random number generator
     */
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * Random in unit cube
     */
    public static Point3d random() {
        return new Point3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    /**
     * Random Gaussian
     */
    public static Point3d randomGaussian() {
        return new Point3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
    }

    /**
     * Random in unit sphere
     */
    public static Point3d randomInSphere() {
        Point3d p;
        do {
            p = random().scale(2).subtract(ijk);
        } while (p.dot(p) > 1);
        return p;
    }

    /**
     * Random in unit circle
     */
    public static Point3d randomInCircle() {
        Point3d p;
        do {
            p = random().scale(2).subtract(ijk).scale(1, 1, 0);
        } while (p.dot(p) > 1);
        return p;
    }

    /**
     * Random on unit sphere
     */
    public static Point3d randomOnSphere() {
        return randomInSphere().normalize();
    }

    /**
     * Random on unit circle
     */
    public static Point3d randomOnCircle() {
        return randomInCircle().normalize();
    }

}
