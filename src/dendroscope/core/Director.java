/*
 * Director.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.core;

import dendroscope.window.MultiViewer;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.*;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.Alert;
import jloda.swing.util.ProgressDialog;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.progress.ProgressCmdLine;
import jloda.util.progress.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * the dendroscope director
 * Daniel Huson
 */
public class Director implements IDirectableViewer, IDirector {
    private int id = 1;
    private IMainViewer viewer;
    private boolean locked = false;

    Document doc;
    boolean docInUpdate = false;
    final private List<IDirectableViewer> viewers = new LinkedList<IDirectableViewer>();
    final private List<IDirectorListener> directorEventListeners = new LinkedList<IDirectorListener>();
    public MessageWindow messageWindow;

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Future future;

    /**
     * create a new director
     *
	 */
    public Director(Document doc) {
        this.doc = doc;
    }

    /**
     * add a viewer to this doc
     *
	 */
    public IDirectableViewer addViewer(IDirectableViewer viewer) {
        if (viewer instanceof IMainViewer)
            this.viewer = (IMainViewer) viewer;

        viewers.add(viewer);
        synchronized (directorEventListeners) {
            directorEventListeners.add(viewer);
        }
        ProjectManager.projectWindowChanged(this, viewer, true);
        return viewer;
    }

    /**
     * does this director (still) contain the named viewer
     *
     * @return true, if director has this viewer
     */
    public boolean containsViewer(IDirectableViewer viewer) {
        return viewers.contains(viewer);
    }

    /**
     * remove a viewer from this doc
     *
	 */
    public void removeViewer(IDirectableViewer viewer) {
        viewers.remove(viewer);
        synchronized (directorEventListeners) {
            directorEventListeners.remove(viewer);
        }
        ProjectManager.projectWindowChanged(this, viewer, false);

        if (viewers.isEmpty())
            ProjectManager.removeProject(this);
    }

    /**
     * returns the list of viewers
     *
     * @return viewers
     */
    public List<IDirectableViewer> getViewers() {
        return viewers;
    }

    /**
     * waits until all viewers are uptodate
     */
    public void WaitUntilAllViewersAreUptoDate() {

        while (!isAllViewersUptodate()) {
			try {
				Thread.sleep(10);
			} catch (Exception ignored) {
			}
        }
    }

    /**
     * returns true, if all viewers are uptodate
     *
     * @return true, if all viewers uptodate
     */
    public boolean isAllViewersUptodate() {
        for (IDirectableViewer viewer : viewers) {
            if (viewer.isUptoDate() == false) {
                //System.err.println("not up-to-date: "+viewer.getTitle()+" "+viewer.getClass().getName());
                return false;
            }
        }
        return true;
    }

    /**
     * notify listeners that viewers should be updated
     *
     * @param what what should be updated?
     */
    public void notifyUpdateViewer(final String what) {
        if (what.equals(TITLE) && messageWindow != null) {
            messageWindow.setTitle("Messages - " + ProgramProperties.getProgramVersion());
        }
        synchronized (directorEventListeners) {
            for (IDirectorListener directorEventListener : directorEventListeners) {
                final IDirectorListener d = directorEventListener;

                try {
					// Put the update into the swing event queue
					SwingUtilities.invokeLater(() -> {
						try {
							d.setUptoDate(false);
							d.updateView(what);
							d.setUptoDate(true);
						} catch (Exception ex) {
							Basic.caught(ex);
							d.setUptoDate(true);
						}
					});
				} catch (Exception ex) {
                    Basic.caught(ex);
                    d.setUptoDate(true);
                }
            }
        }
    }

    /**
     * notify listeners to prevent user input
     */
    public void notifyLockInput() {
        lockUserInput();
        synchronized (directorEventListeners) {
            for (IDirectorListener directorEventListener : directorEventListeners) {
                directorEventListener.lockUserInput();
            }
        }
    }

    /**
     * notify listeners to allow user input
     */
    public void notifyUnlockInput() {
        synchronized (directorEventListeners) {
            for (IDirectorListener directorEventListener : directorEventListeners) {
                directorEventListener.unlockUserInput();
            }
        }
        unlockUserInput();
    }

    /**
     * notify all director event listeners to destroy themselves
     */
    public void notifyDestroyViewer() throws CanceledException {
        synchronized (directorEventListeners) {
            for (IDirectorListener directorEventListener : directorEventListeners) {
                if (directorEventListener != this)
                    directorEventListener.destroyView();
            }
        }

        // now remove all viewers
        while (viewers.size() > 0) {
            removeViewer(viewers.get(0));
        }

        if (future != null && !future.isDone()) {
            try {
                future.cancel(true);
            } catch (Exception ex) {
                // Basic.caught(ex);
            }
            future = null;
        }
    }

    /**
     * execute a command within the swing thread
     *
	 */
    public boolean executeImmediately(final String command) {
        return executeImmediately(command, null);
    }

    /**
     * execute a command in a separate thread
     *
	 */
    public void execute(final String command) {
        execute(command, null);
    }

    /**
     * execute a command within the swing thread
     *
	 */
    public boolean executeImmediately(final String command, CommandManager commandManager) {
        //System.err.println("executing " + command);
        try {
            if (doc.getProgressListener() == null) {
                ProgressListener progressListener = new ProgressCmdLine();
                doc.setProgressListener(progressListener);
            }
            if (commandManager != null)
                commandManager.execute(command);
            if (!locked) {
                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();
            }
            return true;
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED EXECUTE");
        } catch (Exception ex) {
            Basic.caught(ex);
            new Alert("Command failed: " + ex.getMessage());
        }
        return false;
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
	 */
    public void execute(final String command, final CommandManager commandManager) {
        Component parentComponent;
        Object parent = commandManager.getParent();
        if (parent instanceof IDirectableViewer)
            parentComponent = ((IDirectableViewer) parent).getFrame();
        else if (parent instanceof JDialog)
            parentComponent = (JDialog) parent;
        else
            parentComponent = getParent();
        execute(command, commandManager, parentComponent);
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
	 */
    public void execute(String command0, final CommandManager commandManager, final Component parent) {
        final String command;
        if (command0.startsWith("!"))
            command = command0.substring(1);
        else {
            command = command0;
            System.err.println("Executing: " + command);
        }

        if (docInUpdate == true) // shouldn't happen!
            System.err.println("Warning: execute(" + command + "): concurrent execution");
        notifyLockInput();

        for (int countTries = 0; countTries < 2; countTries++) {
            try {
				future = executorService.submit(() -> {
					docInUpdate = true;
					ProgressDialog progressDialog = new ProgressDialog("", "", parent);
					progressDialog.setDebug(Basic.getDebugMode());
					progressDialog.setCloseOnCancel(false);
					doc.setProgressListener(progressDialog);

					try {
						if (commandManager != null)
							commandManager.execute(command);
					} catch (CanceledException ex) {
						System.err.println("USER CANCELED EXECUTE");
					} catch (Exception ex) {
						// Basic.caught(ex);
						new Alert(getParent(), "Execute failed: " + ex.getMessage());
					}

					notifyUpdateViewer(Director.ALL);
					WaitUntilAllViewersAreUptoDate();
					try {
						notifyUnlockInput();
					} catch (Exception ex) {
						// Basic.caught(ex);
					}
					doc.getProgressListener().close();
					doc.setProgressListener(null);
					docInUpdate = false;
				});
                break;
            } catch (RejectedExecutionException ex) {
                //Basic.caught(ex);
                executorService.shutdown();
                executorService = Executors.newCachedThreadPool();
            }
        }
    }

    /**
     * returns the parent viewer
     *
     * @return viewer
     */
    public Component getParent() {
        return viewer.getFrame();
    }

    /**
     * returns a viewer of the given class
     *
     * @return viewer of the given class, or null
     */
    public IDirectableViewer getViewerByClass(Class aClass) {
        for (IDirectableViewer viewer : getViewers()) {
            if (viewer.getClass().equals(aClass))
                return viewer;
        }
        return null;
    }


    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public Document getDocument() {
        return doc;
    }

    public void setDocument(Document doc) {
        this.doc = doc;
    }

    /**
     * close everything directed by this director
     */
    public void close() throws CanceledException {
        notifyDestroyViewer();
        if (future != null)
            future.cancel(true);
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdownNow();
    }

    public void updateView(String what) {

    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        locked = true;
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        locked = false;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
    }

    /**
     * set uptodate state
     *
	 */
    public void setUptoDate(boolean flag) {
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return true;
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return null;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return doc.getTitle();
    }

    public void setTitle(String title) {
        doc.setTitle(title);
    }

    public IMainViewer getMainViewer() {
        return viewer;
    }

    /**
     * are we currently updating the document?
     *
     * @return true, if are in update
     */
    public boolean isInUpdate() {
        return docInUpdate;
    }

    /**
     * set the dirty flag and update all window titles to show astrix
     *
	 */
    public void setDirty(boolean dirty) {
        if (viewer instanceof MultiViewer) {
            if (dirty && !doc.isDocumentIsDirty()) {
                doc.setDocumentIsDirty(true);
                notifyUpdateViewer(TITLE);
            }
        }
    }

    public boolean getDirty() {
        return doc.isDocumentIsDirty();
    }


    /**
     * opens a new project
     *
     * @return new project
     */
    public static Director newProject(int rows, int cols) {
        Document doc = new Document();
        Director dir = new Director(doc);
        dir.setID(ProjectManager.getNextID());
        try {
            MultiViewer viewer = new MultiViewer(dir, rows, cols);
            ProjectManager.addProject(dir, viewer);
            viewer.getFrame().setVisible(true);
        } catch (Exception e) {
            Basic.caught(e);
        }
        return dir;
    }

    public void setMainViewer(IMainViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return getMainViewer().getCommandManager();
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "Director";
    }

    @Override
    public boolean isInternalDocument() {
        return false;
    }

    @Override
    public void setInternalDocument(boolean invisible) {
    }
}
