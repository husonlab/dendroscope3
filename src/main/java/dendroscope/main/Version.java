/*
 * Version.java Copyright (C) 2026 Daniel H. Huson
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
package dendroscope.main;

/**
 * Maintains the version string
 * <p>
 * The version is not written here: a release build sets it (mvn versions:set) and passes it to the program.
 *
 * @author huson
 * Date: 18.12.2003, 11.2026
 */
public class Version {
	public static final String NAME = "Dendroscope";
	public static final String VERSION = resolveVersion();
	public static final String SHORT_DESCRIPTION = NAME + " (version " + VERSION + ")";
	// the GitHub repository; releases and the update manifest are under <HOME_URL>/releases
	public static final String HOME_URL = "https://github.com/husonlab/dendroscope3";
	// the GitHub Pages site, published from docs/
	public static final String WEBSITE_URL = "https://husonlab.github.io/dendroscope3";
	public static final String MANUAL_URL = WEBSITE_URL + "/manual.pdf";

	private static String resolveVersion() {
		// set by the installer's launcher (jpackage --java-options -Dapp.version=...), whether the program is
		// launched from the class path or the module path
		var sys = System.getProperty("app.version");
		if (sys != null && !sys.isBlank())
			return sys;
		// when running the jar built by Maven: the Implementation-Version of its manifest
		var pkg = Version.class.getPackage();
		var v = (pkg != null) ? pkg.getImplementationVersion() : null;
		return (v != null && !v.isBlank()) ? v : "dev";
	}
}
