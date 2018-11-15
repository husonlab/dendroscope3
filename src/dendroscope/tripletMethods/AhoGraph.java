/**
 * AhoGraph.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.tripletMethods;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 12, 2010
 * Time: 12:41:37 PM
 * To change this template use File | Settings | File Templates.
 */
class AhoGraph {
    private final int leaves;

    public static final int NOT_VISITED = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    final boolean[][] adj;
    final int[] deg;

    public AhoGraph(int n) {
        leaves = n;
        adj = new boolean[n][n];
        deg = new int[n];
    }

    public boolean containsEdge(int a, int b) {
        return (adj[a - 1][b - 1]);
    }

    public void addEdge(int a, int b) {
        if (adj[a - 1][b - 1] == false) {
            deg[a - 1]++;
            deg[b - 1]++;
        }

        //! These things are always added in unison...
        adj[a - 1][b - 1] = adj[b - 1][a - 1] = true;
    }

    public int degree(int a) {
        return deg[a - 1];
    }

//! Inspects if the vertices form two disjoint cliques and, zo ja,
//! returns the partition (LEFT/RIGHT) in the return array
//! otherwise returns null
//! The array should be indexed on [1...leaves]

    public int[] getDoubleClique() {
        //! I should first check if the complement graph is bipartite.
        //! If there are more then 2 components, then it won't be
        //! And then I can check if it is a complete bipartite graph.
        //! That is: everyone on LEFT has degree equal to RIGHT, and
        //! viceversa

        int visited[] = new int[leaves + 1];

        visited[0] = -1000;

        //! We'll start with vertex 1
        boolean success = visit(1, LEFT, visited);

        if (!success) return null;

        int lClique = 0;
        int rClique = 0;

        for (int scan = 1; scan <= leaves; scan++) {
            if (visited[scan] == NOT_VISITED) return null;
            if (visited[scan] == LEFT) lClique++;
            if (visited[scan] == RIGHT) rClique++;
        }

        //! So if we've got here we know how many guys are in the
        //! left clique, and in he right...

        for (int scan = 1; scan <= leaves; scan++) {
            if (visited[scan] == LEFT) {
                if (degree(scan) != (lClique - 1)) return null;
            } else {
                if (degree(scan) != (rClique - 1)) return null;
            }

        }

        return visited;
    }


    private boolean visit(int vertex, int colourWith, int state[]) {
        if (state[vertex] != NOT_VISITED) {
            return state[vertex] == colourWith;
        }

        state[vertex] = colourWith;

        int childCol;
        if (colourWith == LEFT) childCol = RIGHT;
        else childCol = LEFT;

        //! Now, try all COMPLEMENT children...

        for (int scan = 1; scan <= leaves; scan++) {
            if (vertex == scan) continue;
            if (!containsEdge(vertex, scan)) {
                boolean gelukt = visit(scan, childCol, state);
                if (!gelukt) return false;
            }

        }

        return true;
    }


}
