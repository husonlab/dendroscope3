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

package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

/*
 *@(#)Matrix3D.java
 *
 * Copyright (c) 1994-1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL or COMMERCIAL purposes and
 * without fee is hereby granted.
 * Please refer to the file http://java.sun.com/copy_trademarks.html
 * for further important copyright and trademark information and to
 * http://java.sun.com/licensing.html for further important licensing
 * information for the Java (tm) Technology.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES").  SUN
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 */

/**
 * A fairly conventional 3D matrix object that can transform sets of
 * 3D points and perform a variety of manipulations on the transform
 */
class Matrix3D {
    double xx, xy, xz, xo;
    double yx, yy, yz, yo;
    double zx, zy, zz, zo;
    static final double pi = 3.14159265;

    /**
     * Create a new unit matrix
     */
    Matrix3D() {
        xx = 1.0f;
        yy = 1.0f;
        zz = 1.0f;
    }

    /**
     * Scale by f in all dimensions
     */
    void scale(double f) {
        xx *= f;
        xy *= f;
        xz *= f;
        xo *= f;
        yx *= f;
        yy *= f;
        yz *= f;
        yo *= f;
        zx *= f;
        zy *= f;
        zz *= f;
        zo *= f;
    }

    /**
     * Scale along each axis independently
     */
    void scale(double xf, double yf, double zf) {
        xx *= xf;
        xy *= xf;
        xz *= xf;
        xo *= xf;
        yx *= yf;
        yy *= yf;
        yz *= yf;
        yo *= yf;
        zx *= zf;
        zy *= zf;
        zz *= zf;
        zo *= zf;
    }

    /**
     * Translate the origin
     */
    void translate(double x, double y, double z) {
        xo += x;
        yo += y;
        zo += z;
    }

    void translate(Point3d t) {
        translate(t.v[0], t.v[1], t.v[2]);
    }

    /**
     * rotate theta degrees about the y axis
     */
    void yrot(double theta) {
        theta *= (pi / 180);
        double ct = Math.cos(theta);
        double st = Math.sin(theta);

        double Nxx = xx * ct + zx * st;
        double Nxy = xy * ct + zy * st;
        double Nxz = xz * ct + zz * st;
        double Nxo = xo * ct + zo * st;

        double Nzx = zx * ct - xx * st;
        double Nzy = zy * ct - xy * st;
        double Nzz = zz * ct - xz * st;
        double Nzo = zo * ct - xo * st;

        xo = Nxo;
        xx = Nxx;
        xy = Nxy;
        xz = Nxz;
        zo = Nzo;
        zx = Nzx;
        zy = Nzy;
        zz = Nzz;
    }

    /**
     * rotate theta degrees about the x axis
     */
    void xrot(double theta) {
        theta *= (pi / 180);
        double ct = Math.cos(theta);
        double st = Math.sin(theta);

        double Nyx = yx * ct + zx * st;
        double Nyy = yy * ct + zy * st;
        double Nyz = yz * ct + zz * st;
        double Nyo = yo * ct + zo * st;

        double Nzx = zx * ct - yx * st;
        double Nzy = zy * ct - yy * st;
        double Nzz = zz * ct - yz * st;
        double Nzo = zo * ct - yo * st;

        yo = Nyo;
        yx = Nyx;
        yy = Nyy;
        yz = Nyz;
        zo = Nzo;
        zx = Nzx;
        zy = Nzy;
        zz = Nzz;
    }

    /**
     * rotate theta degrees about the z axis
     */
    void zrot(double theta) {
        theta *= (pi / 180);
        double ct = Math.cos(theta);
        double st = Math.sin(theta);

        double Nyx = yx * ct + xx * st;
        double Nyy = yy * ct + xy * st;
        double Nyz = yz * ct + xz * st;
        double Nyo = yo * ct + xo * st;

        double Nxx = xx * ct - yx * st;
        double Nxy = xy * ct - yy * st;
        double Nxz = xz * ct - yz * st;
        double Nxo = xo * ct - yo * st;

        yo = Nyo;
        yx = Nyx;
        yy = Nyy;
        yz = Nyz;
        xo = Nxo;
        xx = Nxx;
        xy = Nxy;
        xz = Nxz;
    }

    /**
     * Multiply this matrix by a second: M = M*R
     */
    void mult(Matrix3D rhs) {
        double lxx = xx * rhs.xx + yx * rhs.xy + zx * rhs.xz;
        double lxy = xy * rhs.xx + yy * rhs.xy + zy * rhs.xz;
        double lxz = xz * rhs.xx + yz * rhs.xy + zz * rhs.xz;
        double lxo = xo * rhs.xx + yo * rhs.xy + zo * rhs.xz + rhs.xo;

        double lyx = xx * rhs.yx + yx * rhs.yy + zx * rhs.yz;
        double lyy = xy * rhs.yx + yy * rhs.yy + zy * rhs.yz;
        double lyz = xz * rhs.yx + yz * rhs.yy + zz * rhs.yz;
        double lyo = xo * rhs.yx + yo * rhs.yy + zo * rhs.yz + rhs.yo;

        double lzx = xx * rhs.zx + yx * rhs.zy + zx * rhs.zz;
        double lzy = xy * rhs.zx + yy * rhs.zy + zy * rhs.zz;
        double lzz = xz * rhs.zx + yz * rhs.zy + zz * rhs.zz;
        double lzo = xo * rhs.zx + yo * rhs.zy + zo * rhs.zz + rhs.zo;

        xx = lxx;
        xy = lxy;
        xz = lxz;
        xo = lxo;

        yx = lyx;
        yy = lyy;
        yz = lyz;
        yo = lyo;

        zx = lzx;
        zy = lzy;
        zz = lzz;
        zo = lzo;
    }

    /**
     * Reinitialize to the unit matrix
     */
    void unit() {
        xo = 0;
        xx = 1;
        xy = 0;
        xz = 0;
        yo = 0;
        yx = 0;
        yy = 1;
        yz = 0;
        zo = 0;
        zx = 0;
        zy = 0;
        zz = 1;
    }

    /**
     * Transform nvert points from v into tv.  v contains the input
     * coordinates in doubleing point.  Three successive entries in
     * the array constitute a point.  tv ends up holding the transformed
     * points as integers; three successive entries per point
     */
    void transform(double v[], int tv[], int nvert) {
        double lxx = xx, lxy = xy, lxz = xz, lxo = xo;
        double lyx = yx, lyy = yy, lyz = yz, lyo = yo;
        double lzx = zx, lzy = zy, lzz = zz, lzo = zo;
        for (int i = nvert * 3; (i -= 3) >= 0; ) {
            double x = v[i];
            double y = v[i + 1];
            double z = v[i + 2];
            tv[i] = (int) (x * lxx + y * lxy + z * lxz + lxo);
            tv[i + 1] = (int) (x * lyx + y * lyy + z * lyz + lyo);
            tv[i + 2] = (int) (x * lzx + y * lzy + z * lzz + lzo);
        }
    }

    /**
     * Apply transformation to an array of points
     */
    void transform(Point3d v[]) {
        double lxx = xx, lxy = xy, lxz = xz, lxo = xo;
        double lyx = yx, lyy = yy, lyz = yz, lyo = yo;
        double lzx = zx, lzy = zy, lzz = zz, lzo = zo;
        for (int i = 0; i < v.length; i++) {
            double x = v[i].v[0];
            double y = v[i].v[1];
            double z = v[i].v[2];
            v[i].v[0] = (x * lxx + y * lxy + z * lxz + lxo);
            v[i].v[1] = (x * lyx + y * lyy + z * lyz + lyo);
            v[i].v[2] = (x * lzx + y * lzy + z * lzz + lzo);
        }
    }

    /**
     * Apply transformation to a  points
     */
    void transform(Point3d p) {
        double lxx = xx, lxy = xy, lxz = xz, lxo = xo;
        double lyx = yx, lyy = yy, lyz = yz, lyo = yo;
        double lzx = zx, lzy = zy, lzz = zz, lzo = zo;
        double x = p.v[0];
        double y = p.v[1];
        double z = p.v[2];
        p.v[0] = (x * lxx + y * lxy + z * lxz + lxo);
        p.v[1] = (x * lyx + y * lyy + z * lyz + lyo);
        p.v[2] = (x * lzx + y * lzy + z * lzz + lzo);
    }

    public String toString() {
        return ("[" + xx + "," + xy + "," + xz + "," + xo + "\n "
                + yx + "," + yy + "," + yz + "," + yo + "\n "
                + zx + "," + zy + "," + zz + "," + zo + "]");
    }
}
