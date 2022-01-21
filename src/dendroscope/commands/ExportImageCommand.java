/*
 * ExportImageCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands;

import dendroscope.core.Document;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.export.*;
import jloda.swing.util.ResourceManager;
import jloda.util.FileUtils;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * export image command
 * Daniel Huson, 6.2010
 */
public class ExportImageCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Export Image...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Export the tree or network to an image file";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     */
    public void apply(NexusStreamParser np) throws Exception {
        final MultiViewer multiViewer = (MultiViewer) getViewer();

        np.matchIgnoreCase("exportimage file=");
        final String fileName = np.getWordFileNamePunctuation();

        final java.util.List<String> tokens = np.getTokensRespectCase(null, ";");

        String format = np.findIgnoreCase(tokens, "format=", "bmp svg gif png jpg pdf file-suffix", "file-suffix");
        final boolean visibleOnly = np.findIgnoreCase(tokens, "visibleOnly=", "true false", "false").equals("true");
        final boolean text2shapes = np.findIgnoreCase(tokens, "textAsShapes=", "true false", "false").equals("true");
        final String title = np.findIgnoreCase(tokens, "title=", null, "none");
        final boolean replace = np.findIgnoreCase(tokens, "replace=", "true false", "false").equals("true");
        np.checkFindDone(tokens);

        final File file = new File(fileName);
        if (replace == false && file.exists())
            throw new IOException("exportimage: File exists: " + fileName + ", use REPLACE=true to overwrite");


        if (format.equals("file-suffix")) {
			format = FileUtils.getFileSuffix(fileName);
        }

        try {
            ExportGraphicType graphicsType;
            if (format.equalsIgnoreCase("eps")) {
                graphicsType = new EPSExportType();
                ((EPSExportType) graphicsType).setDrawTextAsOutlines(!text2shapes);
            } else if (format.equalsIgnoreCase("bmp"))
                graphicsType = new RenderedExportType();
            else if (format.equalsIgnoreCase("png"))
                graphicsType = new PNGExportType();
            else if (format.equalsIgnoreCase("pdf"))
                graphicsType = new PDFExportType();
            else if (format.equalsIgnoreCase("svg"))
                graphicsType = new SVGExportType();
            else if (format.equalsIgnoreCase("gif"))
                graphicsType = new GIFExportType();
            else if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg"))
                graphicsType = new JPGExportType();
            else
                throw new IOException("Unsupported graphics format: " + format);

            JPanel panel;
            JScrollPane scrollPane = null;

            if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 1) {
                TreeViewer treeViewer = multiViewer.getTreeGrid().getSelectedOrAllIterator().next();
                panel = treeViewer;
                scrollPane = treeViewer.getScrollPane();
            } else {
                if (format.equals("pdf") || format.equals("eps"))
                    panel = multiViewer.getTreeGrid().getAsJPanel();
                else
                    panel = multiViewer.getTreeGrid();
            }
            graphicsType.writeToFile(file, panel, scrollPane, !visibleOnly);
            System.err.println("Exported image to file: " + file);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final MultiViewer viewer = (MultiViewer) getViewer();
        final Document doc = viewer.getDir().getDocument();

        // setup a good default name: the file name plus .eps:
        String fileName = "Untitled";
        if (doc.getFile() != null)
            fileName = doc.getFile().getPath();

        //  viewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ExportImageDialog saveImageDialog;
        if (viewer.getTreeGrid().getNumberOfPanels() == 1 || viewer.getTreeGrid().getNumberOfSelectedViewers() == 1) {
            saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, true, true, false, ev);
        } else {
            saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, true, false, false, ev);
        }
        String command = saveImageDialog.displayDialog();
        if (command != null)
            execute(command);
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    public String getSyntax() {
        return "exportimage file=<filename> [format={BMP|PNG|GIF|JPG|SVG|PDF|file-suffix}] [textasshapes=bool] [visibleonly={false|true}] [title=title] [replace={false|true}] ;";
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
