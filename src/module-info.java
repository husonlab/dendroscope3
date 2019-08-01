module dendroscope {
    requires transitive jloda;

    requires transitive java.desktop;
    requires transitive com.install4j.runtime;

    requires commons.collections;
    requires nexml;


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

    exports dendroscope.io.nexml;
    exports dendroscope.main;

    opens dendroscope.main;
    opens dendroscope.resources.icons;
    opens dendroscope.resources.images;

}