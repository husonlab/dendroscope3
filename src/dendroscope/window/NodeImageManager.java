/*
 * NodeImageManager.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.window;

import jloda.graph.Node;
import jloda.swing.graphview.NodeImage;
import jloda.swing.graphview.NodeView;
import jloda.util.Basic;
import jloda.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * manage the images that are to be displayed at the nodes
 * Daniel Huson, 7.2007
 */
public class NodeImageManager {
    private final TreeViewer viewer;
    private final Map<String, NodeImage> name2image;
    private static final int defaultImageHeight = 50;
    private static final byte defaultImageLayout = NodeView.RADIAL;

    /**
     * constructor
     *
     * @param viewer
     */
    public NodeImageManager(TreeViewer viewer) {
        this.viewer = viewer;
        name2image = new HashMap<String, NodeImage>();
    }

    /**
     * load images from directory
     *
     * @param dir
     * @return number of images loaded
     */
    public int loadImagesFromDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
					if (FileUtils.isImageFile(file))
						try {
							NodeImage image = new NodeImage(file, viewer);
							image.setHeight(defaultImageHeight);
							image.setLayout(defaultImageLayout);
							String name = file.getName();
							int p = name.lastIndexOf(".");
							if (p > 0)
								name = name.substring(0, p);
							name2image.put(name, image);
						} catch (IOException e) {
                            Basic.caught(e);
                            System.err.println("file: " + file.getName());
                        }
                }
                System.err.println("Images loaded: " + name2image.size());
            }
        }
        return name2image.size();
    }

    /**
     * returns an image for the named taxon, or null
     *
     * @param name
     * @return image or null
     */
    public NodeImage getImage(String name) {
        if (name2image.keySet().contains(name))
            return name2image.get(name);
        else {
            double bestScore = 0;
            String bestName = null;
            for (String fileName : name2image.keySet()) {
                double score = getAlignmentScore(name.toLowerCase(), fileName.toLowerCase());
                int threshold = Math.min(name.length(), fileName.length());
                threshold = 0;
                if (score > threshold && score > bestScore) {
                    bestScore = score;
                    bestName = fileName;
                }
            }
            if (bestName != null) {
                System.err.println("Taxon '" + name + "' mapped to image '" + bestName + "', score: " + bestScore);
                if (!name2image.keySet().contains(name))
                    name2image.put(name, name2image.get(bestName)); // cache tbis

                return name2image.get(bestName);
            } else {
                name2image.put(name, null); // cache tbis
                System.err.println("Taxon '" + name + "' NO IMAGE FOUND, best score: " + bestScore);
                return null;
            }
        }
    }

    /**
     * computes max alignment score of two strings. Linear gap penalty of -1, mismatch -1, match 2
     *
     * @param name1
     * @param name2
     * @return best score
     */
    private double getAlignmentScore(String name1, String name2) {
        int m = name1.length() + 1;
        int n = name2.length() + 1;
        int[][] M = new int[m][n];

        for (int i = 0; i < m; i++)
            M[i][0] = 0;

        for (int j = 0; j < n; j++)
            M[0][j] = 0;

        int best = 0;
        for (int i = 1; i < m; i++)
            for (int j = 1; j < n; j++) {
                M[i][j] = Math.max(0, Math.max(
                        M[i - 1][j] - 1,
                        Math.max(
                                M[i][j - 1] - 1,
                                M[i - 1][j - 1] + (name1.charAt(i - 1) == name2.charAt(j - 1) ? 2 : -1)))

                );
                if (M[i][j] > best)
                    best = M[i][j];
            }
        return best;
    }


    /**
     * applies all images that can be applied
     */
    public void applyImagesToNodes() {
        if (name2image.size() > 0) {
            for (Node v = viewer.getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
                String name = viewer.getPhyloTree().getLabel(v);
                if (name != null) {
                    NodeImage image = getImage(name);
                    if (image != null) {
                        viewer.getNV(v).setImage(image);
                    }
                }
            }
        }
    }

    /**
     * sets the image height
     *
     * @param defaultImageHeight
     */
    public void setDefaultImageHeight(int defaultImageHeight) {
        for (Node v = viewer.getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
            if ((viewer.getSelectedNodes().size() == 0 || viewer.getSelected(v)) && viewer.getNV(v).getImage() != null) {
                viewer.getNV(v).getImage().setHeight(defaultImageHeight);
            }
        }
    }

    /**
     * sets the image layout to north south east or west
     *
     * @param layout
     */
    public void setDefaultImageLayout(String layout) {
        byte imageLayout;
        if (layout.equalsIgnoreCase("radial"))
            imageLayout = NodeView.RADIAL;

        else if (layout.equalsIgnoreCase("north"))
            imageLayout = NodeView.NORTH;
        else if (layout.equalsIgnoreCase("south"))
            imageLayout = NodeView.SOUTH;
        else if (layout.equalsIgnoreCase("west"))
            imageLayout = NodeView.WEST;
        else
            imageLayout = NodeView.EAST;
        for (Node v = viewer.getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
            if ((viewer.getSelectedNodes().size() == 0 || viewer.getSelected(v)) && viewer.getNV(v).getImage() != null) {
                viewer.getNV(v).getImage().setLayout(imageLayout);
            }
        }
    }
}
