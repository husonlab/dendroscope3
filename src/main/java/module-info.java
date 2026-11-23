module dendroscope {
    requires transitive jloda_core;
    requires transitive jloda_swing;
    requires jloda_fx;
    requires javafx.graphics;

    // the GitHub update check, provided by the github-updater library that the installers bundle
    uses jloda.fx.service.UpdateService;

    requires org.apache.commons.collections4;
    requires nexml;
    requires java.desktop;

    exports dendroscope.commands;
    exports dendroscope.commands.autumn;
    exports dendroscope.commands.collapse;
    exports dendroscope.commands.compute;
    exports dendroscope.commands.consensus;
    exports dendroscope.commands.draw;
    exports dendroscope.commands.formatting;
    exports dendroscope.commands.go;
    exports dendroscope.commands.mul;
    exports dendroscope.commands.select;

    exports dendroscope.io.nexml;
    exports dendroscope.main;

    opens dendroscope.main;
    opens dendroscope.resources.icons;
    opens dendroscope.resources.images;

}