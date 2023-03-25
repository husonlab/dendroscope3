/*
 *   CommandBaseMultiViewer.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands;

import dendroscope.core.Director;
import dendroscope.window.MultiViewer;
import jloda.swing.commands.CommandBase;
import jloda.swing.director.IDirector;

/**
 * base-class for multi-viewer commands
 * Daniel Huson, 5.2010
 */
public abstract class CommandBaseMultiViewer extends CommandBase {
    protected MultiViewer multiViewer;

    /**
     * set the director
     *
	 */

    public void setDir(IDirector dir) {
        super.setDir(dir);
        if (dir.getMainViewer() instanceof MultiViewer)
            multiViewer = (MultiViewer) dir.getMainViewer();
        else
            multiViewer = null;
    }

    /**
     * get the director
     *
	 */
    public Director getDir() {
        return (Director) super.getDir();
    }

}
