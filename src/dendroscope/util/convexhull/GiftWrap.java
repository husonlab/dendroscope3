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

import jloda.util.Basic;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Constructs a convex hull via giftwrapping algorithm
 */

public class GiftWrap extends HullAlgorithm {
    /**
     * compute the convex hull of a set of 2D points using the Quick hull algorithm
     *
     * @param input
     * @return points on hull
     */
    public static Set<Point2D> apply2D(List<Point2D> input) {
        GiftWrap giftWrap = new GiftWrap(input);

        Object3dList objects = giftWrap.build2D();

        Set<Point2D> result = new HashSet<Point2D>();

        for (int i = 0; i < objects.size(); i++) {
            Object3d object = objects.elementAt(i);
            result.add(new Point2D.Double(object.centre().x(), object.centre().y()));
        }
        return result;
    }

    public GiftWrap(Point3dObject3d[] pts) {
        super(pts);
    }


    public GiftWrap(List<Point2D> pts) {
        super(convert(pts));
    }

    /**
     * convert java points
     *
     * @param pts
     * @return converted points
     */
    private static Point3dObject3d[] convert(List<Point2D> pts) {
        List<Point3dObject3d> list = new LinkedList<Point3dObject3d>();
        for (Point2D src : pts) {
            list.add(new Point3dObject3d(new Point3d(src.getX(), src.getY(), 1)));
        }
        return list.toArray(new Point3dObject3d[list.size()]);
    }


    int index(Point3d p) {
        for (int i = 0; i < pts.length; i++) {
            if (p == pts[i]) {
                return i;
            }
        }
        return -1;
    }

    protected Point3d search(Edge3d e) {
        int i;
        for (i = 0; pts[i] == e.start || pts[i] == e.end; i++) {
            /* nothing */
        }
        Point3d cand = pts[i];
        HalfSpace candh = new HalfSpace(e.start, e.end, cand);
        for (i = i + 1; i < pts.length; i++) {
            if (pts[i] != e.start && pts[i] != e.end && candh.inside(pts[i])) {
                cand = pts[i];
                candh = new HalfSpace(e.start, e.end, cand);
            }
        }
        return cand;
    }

    protected Point3d search2d(Point3d p) {
        int i;
        i = pts[0] == p ? 1 : 0;
        Point3d cand = pts[i];
        HalfSpace candh = new HalfSpace(p, cand);
        for (i = i + 1; i < pts.length; i++) {
            if (pts[i] != p && candh.inside(pts[i])) {
                cand = pts[i];
                candh = new HalfSpace(p, cand);
            }
        }
        return cand;
    }

    /* bottom point */

    protected Point3d bottom() {
        Point3d bot = pts[0];
        for (int i = 1; i < pts.length; i++) {
            if (pts[i].y() < bot.y()) {
                bot = pts[i];
            }
        }
        return bot;
    }

    public Object3dList build() {
        /* First find a hull edge -- just connect bottommost to second from bottom */
        Point3d bot, bot2; /* bottom point and adjacent point*/
        bot = bottom();
        bot2 = search2d(bot);

        /* intialize the edge stack */
        EdgeStack es = new EdgeStack();
        es.put(bot, bot2);
        es.put(bot2, bot);
        Object3dList faces = new Object3dList(20);
        int tcount = 1;
        Edge3d e = new Edge3d(bot, bot2, tcount);
        e.lastFrame = tcount++;
        faces.addElement(e);

        /* now the main loop -- keep finding faces till there are no more to be found */
        while (!es.isEmpty()) {
            e = es.get();
            Point3d cand = search(e);
            faces.addElement(new Triangle3d(e.start, e.end, cand, tcount++));
            es.putp(e.start, cand);
            es.putp(cand, e.end);
        }
        faces.lastFrame = tcount;
        return faces;
    }


    public Object3dList build2D() {
        /* First find a hull vertex -- just bottommost*/
        Point3d p; /* current hull vertex */
        Point3d bot = bottom(); /* bottom point */

        Object3dList faces = new Object3dList(20);
        int tcount = 1;
        faces.addElement(new Point3dObject3d(bot, tcount++));

        /* now the main loop -- keep finding edges till we get back */

        p = bot;
        do {
            Point3d cand = search2d(p);
            faces.addElement(new Edge3d(p, cand, tcount++));
            p = cand;
        } while (p != bot);
        faces.lastFrame = tcount;
        return faces;
    }

    /**
     * test the two-dimensional algorithm
     *
     * @param args
     */
    public static void main(String[] args) {
        Point2D[] input = new Point2D[]
                {
                        new Point2D.Double(0, 0),
                        new Point2D.Double(0.5, 0.5),
                        new Point2D.Double(1, 0),
                        new Point2D.Double(0, 1),
                        new Point2D.Double(0, 0.1),
                        new Point2D.Double(0, 0.2),

                        new Point2D.Double(1, 1),
                        new Point2D.Double(0.9, 1),
                        new Point2D.Double(0.8, 1),

                        new Point2D.Double(0.1, 0.1)};

        System.err.println("Input: " + Basic.toString(input, ";"));

        Set<Point2D> result = GiftWrap.apply2D(Arrays.asList(input));
        System.err.println("Output: " + Basic.toString(result, ";"));
    }

}


