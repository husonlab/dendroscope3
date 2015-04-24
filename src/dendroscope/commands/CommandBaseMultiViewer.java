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

package dendroscope.commands;

import dendroscope.core.Director;
import dendroscope.window.MultiViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.director.IDirector;

/**
 * base-class for multi-viewer commands
 * Daniel Huson, 5.2010
 */
public abstract class CommandBaseMultiViewer extends CommandBase {
    protected MultiViewer multiViewer;

    /**
     * set the director
     *
     * @param dir
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
     * @return
     */
    public Director getDir() {
        return (Director) super.getDir();
    }

}
