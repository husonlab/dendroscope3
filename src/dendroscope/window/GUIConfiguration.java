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

package dendroscope.window;

import dendroscope.main.DendroscopeProperties;
import jloda.gui.MenuConfiguration;
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
        menuConfig.defineMenuBar("File;Edit;Select;Options;Algorithms;Layout;View;Window;");

        menuConfig.defineMenu("File", "New...;Open...;@Open Recent;|;Add From File...;Enter Trees or Networks...;|;Save;Save As...;" +
                "Export...;|;Export Image...;|;Page Setup...;Print...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");

        menuConfig.defineMenu("Edit", "Copy;Copy Image;Paste;|;Find...;Find Again;Replace...;|;Reroot;Midpoint Root;|;Swap Subtrees;Rotate Subtrees;Reorder Subtrees...;"
                + "|;Delete Taxa;Contract Edges;|;Unlock Edge Lengths;|;Format...;");

        menuConfig.defineMenu("Select", "@Advanced Selection;|;All Panels;No Panels;Invert Panels;|;Select All;Select Nodes;Select Edges;|;Select Short Edges...;Select Long Edges...;|;" +
                "From Previous Window;|;Deselect All;Deselect Nodes;Deselect Edges;|;" +
                "Select Labeled Nodes;Select Leaves;|;" +
                "Select Root;Select Non-Terminal;Select Special;|;Invert Selection;|;Scroll To Selection;|;List Selected Taxa;");

        menuConfig.defineMenu("Advanced Selection", "Select Subnetwork;Select Induced Network;Select LSA Induced Network;Select Spanned Edges;");

        menuConfig.defineMenu("Options", "@Advanced Options;|;Collapse;Uncollapse;Uncollapse Subtree;|;Collapse Complement;Collapse At Level...;|;" +
                (DendroscopeProperties.ALLOW_IMAGES ? "|;Load Taxon Images...;Set Image Size...;Set Image Layout...;" : "")
                + "|;Next Tree;Next Page;Previous Tree;Previous Page;|;Go to Tree...;|;Set Tree Name...;");
        menuConfig.defineMenu("Advanced Options", "Extract Subnetwork...;Extract Induced Network...;Extract LSA Induced Network...;");


        menuConfig.defineMenu("Network Layout", "Layout Optimizer 2010;Layout Optimizer 2009;Layout Optimizer 2008;|;Layout Optimizer None;");

        menuConfig.defineMenu("Algorithms", "@Advanced Algorithms;@Multi-Labeled Tree To Network;|;Strict Consensus...;Loose Consensus...;" +
                        "Majority Consensus...;|;LSA Consensus...;Primordial Consensus...;|;Cluster Network Consensus...;Level-k Network Consensus...;"
                        + "Galled Network Consensus...;|;Hybridization Networks...;Hybridization Networks (Binary Trees)...;Reroot by Hybridization Number...;|;"
                        + "Tanglegram...;"
                        + (ProgramProperties.get("enable_experimental", false) ? "|;Merge Isomorphic Induced...;|;" +
                        "Refine...;Subtree Reduction...;Cluster Reduction...;|;Test for Duplicates;" : "")
        );

        menuConfig.defineMenu("Advanced Algorithms", "Hybridization Number...;Hybridization Number (Binary Trees)...;rSPR Distance (Binary Trees)...;|;DTL Reconciliation...;"
                + "|;Hardwired Cluster Distance...;Softwired Cluster Distance...;Displayed Trees Distance...;Tripartition Distance...;Nested Labels Distance...;Path Multiplicity Distance...;"
                + "|;Distance To Root...;|;Topological Constraints...;Network Properties...;|;Simplistic...;");

        menuConfig.defineMenu("Multi-Labeled Tree To Network", "MUL to Network, Cluster-based...;MUL to Network, HOLM 2006...;MUL to Network, Level-k-based...;|;MUL to Contracted Tree...;");

        menuConfig.defineMenu("Layout", "Draw Rectangular Phylogram;Draw Rectangular Cladogram;|;Draw Slanted Cladogram;|;"
                + "Draw Circular Phylogram;Draw Circular Cladogram;Draw Inner Circular Cladogram;|;" +
                "Draw Radial Phylogram;Draw Radial Cladogram;|;" +
                "Ladderize Left;Ladderize Right;Ladderize Random;|;@Network Layout;|;Align Taxa;|;Connect Taxa;Disconnect All;");

        menuConfig.defineMenu("View", "Increase Font Size;Decrease Font Size;|;" +
                "Set Grid...;Less Panels;More Panels;|;Show Scroll Bars;Show Borders;|;Show Scale Bar;|;Zoom To Fit;Fully Contract;Fully Expand;" +
                "Set Scale...;|;Use Magnifier;Magnify All Mode;|;Show Node Labels;Show Edge Labels;|;Label Edges By Weights;|;" +
                "Sparse Labels;Radial Labels;|;Reposition Labels;");

        menuConfig.defineMenu("Window", "About...;How to Cite...;Website...;|;Register...;|;Set Window Size...;|;" +
                "Command-line Syntax;Command Input...;|;Message Window...;");
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
