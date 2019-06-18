module dendroscope {
    requires transitive jloda;

    requires transitive java.desktop;
    requires transitive com.install4j.runtime;

    requires commons.collections;
    requires nexml;

    exports dendroscope.algorithms.clusternet;
    exports dendroscope.algorithms.gallnet;
    exports dendroscope.algorithms.levelknet;
    exports dendroscope.algorithms.levelknet.cass;
    exports dendroscope.algorithms.levelknet.leo;
    exports dendroscope.algorithms.levelknet.tmp;
    exports dendroscope.algorithms.utils;
    exports dendroscope.anticonsensus;
    exports dendroscope.autumn;
    exports dendroscope.autumn.hybridnetwork;
    exports dendroscope.autumn.hybridnumber;
    exports dendroscope.commands;
    exports dendroscope.commands.autumn;
    exports dendroscope.commands.collapse;
    exports dendroscope.commands.compute;
    exports dendroscope.commands.consensus;
    exports dendroscope.commands.draw;
    exports dendroscope.commands.formatting;
    exports dendroscope.commands.go;
    exports dendroscope.commands.hybrid;
    exports dendroscope.commands.hybroscale;
    exports dendroscope.commands.mul;
    exports dendroscope.commands.select;
    exports dendroscope.consensus;
    exports dendroscope.core;
    exports dendroscope.dialogs.input;
    exports dendroscope.drawer;
    exports dendroscope.dtl;
    exports dendroscope.embed;
    exports dendroscope.hybrid;
    exports dendroscope.hybroscale.controller;
    exports dendroscope.hybroscale.model;
    exports dendroscope.hybroscale.model.attachNetworks;
    exports dendroscope.hybroscale.model.cmpAllMAAFs;
    exports dendroscope.hybroscale.model.cmpMinNetworks;
    exports dendroscope.hybroscale.model.parallelization;
    exports dendroscope.hybroscale.model.reductionSteps;
    exports dendroscope.hybroscale.model.treeObjects;
    exports dendroscope.hybroscale.model.util;
    exports dendroscope.hybroscale.rerooting;
    exports dendroscope.hybroscale.terminals;
    exports dendroscope.hybroscale.util;
    exports dendroscope.hybroscale.util.graph;
    exports dendroscope.hybroscale.util.lcaQueries;
    exports dendroscope.hybroscale.util.newick;
    exports dendroscope.hybroscale.util.sparseGraph;
    exports dendroscope.hybroscale.view;
    exports dendroscope.io;
    exports dendroscope.io.nexml;
    exports dendroscope.main;
    exports dendroscope.multnet;
    exports dendroscope.progs;
    exports dendroscope.resources.icons;
    exports dendroscope.resources.images;
    exports dendroscope.tanglegram;
    exports dendroscope.tripletMethods;
    exports dendroscope.triplets;
    exports dendroscope.util;
    exports dendroscope.util.convexhull;
    exports dendroscope.window;

    opens dendroscope.main;
    opens dendroscope.resources.icons;
    opens dendroscope.resources.images;

}