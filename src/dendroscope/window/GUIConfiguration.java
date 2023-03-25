/*
 *   GUIConfiguration.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.window;

import dendroscope.main.DendroscopeProperties;
import jloda.swing.window.MenuConfiguration;
import jloda.util.ProgramProperties;

/**
 * configuration for menu and toolbar
 * Daniel Huson, 7.2010
 */
public class GUIConfiguration {

    /**
     * get the menu configuration
     *
     * @return menu configuration
     */
    public static MenuConfiguration getMenuConfiguration() {
        MenuConfiguration menuConfig = new MenuConfiguration();
        menuConfig.defineMenuBar("File;Edit;Select;Options;Algorithms;Layout;View;Window;Help;");

        menuConfig.defineMenu("File", "New...;Open...;@Open Recent;|;Add From File...;Enter Trees or Networks...;|;Save;Save As...;" +
                "Export...;|;Export Image...;|;Page Setup...;Print...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");

        menuConfig.defineMenu("Edit", "Copy;Copy Image;Paste;|;Find...;Find Again;Replace...;|;Reroot;Midpoint Root;|;Swap Subtrees;Rotate Subtrees;Reorder Subtrees...;"
                + "|;Delete Taxa;Contract Edges;Contract Low Support Edges;|;Unlock Edge Lengths;|;Format...;");

        menuConfig.defineMenu("Select", "@Advanced Selection;|;All Panels;No Panels;Invert Panels;|;Select All;Select Nodes;Select Edges;|;Select Short Edges...;Select Long Edges...;|;" +
                "From Previous Window;|;Deselect All;Deselect Nodes;Deselect Edges;|;" +
                "Select Labeled Nodes;Select Leaves;|;" +
                "Select Root;Select Non-Terminal;Select Special;|;Invert Selection;|;Scroll To Selection;|;List Selected Taxa;");

        menuConfig.defineMenu("Advanced Selection", "Select Subnetwork;Select Induced Network;Select LSA Induced Network;Select Spanned Edges;");

        menuConfig.defineMenu("Options", "@Advanced Options;Internal Node Labels Interpreted As Edge Labels;|;Collapse;Uncollapse;Uncollapse Subtree;|;Collapse Complement;Collapse At Level...;|;" +
                (DendroscopeProperties.ALLOW_IMAGES ? "|;Load Taxon Images...;Set Image Size...;Set Image Layout...;" : "")
                + "|;Next Tree;Next Page;Previous Tree;Previous Page;|;Go to Tree...;|;Set Tree Name...;");
        menuConfig.defineMenu("Advanced Options", "Extract Subnetwork...;Extract Induced Network...;Extract LSA Induced Network...;");


        menuConfig.defineMenu("Network Layout", "Layout Optimizer 2010;Layout Optimizer 2009;Layout Optimizer 2008;|;Layout Optimizer None;");

        menuConfig.defineMenu("Algorithms", "@Advanced Algorithms;@Multi-Labeled Tree To Network;|;Strict Consensus...;Loose Consensus...;" +
                "Majority Consensus...;|;LSA Consensus...;Primordial Consensus...;|;Cluster Network Consensus...;Level-k Network Consensus...;"
                + "Galled Network Consensus...;|;Hybridization Networks...;Reroot by Hybridization Number...;"
                + ProgramProperties.getIfEnabled("allow-hybroscale", "Reroot by Hybridization Number(Hybroscale)...;")
                + "|;Tanglegram...;"
                + (ProgramProperties.get("enable_experimental", false) ? "|;Merge Isomorphic Induced...;|;" +
                "Refine...;Subtree Reduction...;Cluster Reduction...;|;Test for Duplicates;" : "")
        );

        menuConfig.defineMenu("Advanced Algorithms", "Hybridization Number...;|;"
                + "|;Hardwired Cluster Distance...;Softwired Cluster Distance...;Displayed Trees Distance...;Tripartition Distance...;" +
                "Nested Labels Distance...;Path Multiplicity Distance...;"
                + "|;Distance To Root...;|;Topological Constraints...;Network Properties...;|;Simplistic...;");

        menuConfig.defineMenu("Multi-Labeled Tree To Network", "MUL to Network, Cluster-based...;MUL to Network, HOLM 2006...;" +
                "MUL to Network, Level-k-based...;|;MUL to Contracted Tree...;");

        menuConfig.defineMenu("Layout", "Draw Rectangular Phylogram;Draw Rectangular Cladogram;|;Draw Slanted Cladogram;|;"
                + "Draw Circular Phylogram;Draw Circular Cladogram;Draw Inner Circular Cladogram;|;" +
                "Draw Radial Phylogram;Draw Radial Cladogram;|;" +
                "Ladderize Left;Ladderize Right;Ladderize Random;|;@Network Layout;|;Align Taxa;|;Connect Taxa;Disconnect All;");

        menuConfig.defineMenu("View", "Increase Font Size;Decrease Font Size;|;" +
                "Set Grid...;Less Panels;More Panels;|;Show Scroll Bars;Show Borders;|;Show Scale Bar;|;Zoom To Fit;Fully Contract;Fully Expand;" +
                "Set Scale...;|;Use Magnifier;Magnify All Mode;|;Show Node Labels;Show Edge Labels;|;Label Edges By Weights;|;" +
                "Sparse Labels;Radial Labels;|;Reposition Labels;");

        menuConfig.defineMenu("Window", "Set Window Size...;|;Command-line Syntax;Command Input...;|;Message Window...;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Website...;Reference Manual...;|;Check For Updates...;");

        return menuConfig;
    }

    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBarConfiguration() {
        return "Previous Tree;Next Tree;|;Open...;Enter Trees or Networks...;Save;Print...;Export Image...;|;Find...;|;Set Grid...;Show Borders;Show Scroll Bars;|;" +
                "Format...;|;Expand Vertical;Contract Vertical;Expand Horizontal;Contract Horizontal;|;" +
                "Rotate Left;Rotate Right;Flip Horizontally;|;Connect Taxa;|;Zoom To Fit;Fully Contract;Fully Expand;|;" +
                "Use Magnifier;|;" +
                "Draw Rectangular Phylogram;Draw Rectangular Cladogram;Draw Slanted Cladogram;Draw Circular Phylogram;Draw Circular Cladogram;" +
                "Draw Inner Circular Cladogram;Draw Radial Phylogram;Draw Radial Cladogram;|;" +
                "Ladderize Left;Ladderize Right;Ladderize Random;|;Reroot;Swap Subtrees;|;" +
                "Decrement Auxiliary Parameter;Increment Auxiliary Parameter;";
    }

    /**
     * get the configuration of the node popup menu
     *
     * @return config
     */
    public static String getNodePopupConfiguration() {
        return "Edit Node Label...;Format...;Show Node Labels;|;Copy Node Labels;|;" +
                "Select Subnetwork;|;Swap Subtrees;Rotate Subtrees;|;Reorder Subtrees...;";
    }

    /**
     * get the configuration of the edge popup menu
     *
     * @return config
     */
    public static String getEdgePopupConfiguration() {
        return "Edit Edge Label...;Format...;|;Show Edge Labels;|;Copy Edge Labels;";
    }

    /**
     * gets the canvas popup configuration
     *
     * @return config
     */
    public static String getPanelPopupConfiguration() {
        return "Select All;Deselect All;|;Zoom To Fit;|;Show Scroll Bars;Show Borders;|;Set Tree Name...;";
    }
}
