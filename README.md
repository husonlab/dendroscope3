# Dendroscope 3

Dendroscope is an interactive desktop program for drawing, editing and comparing rooted phylogenetic trees and
networks.

## Features

- Eight drawings of trees and networks: rectangular, slanted, circular and inner circular cladograms, rectangular and
  circular phylograms, and radial cladograms and phylograms
- Navigation of large trees, with up to hundreds of thousands of taxa
- Rerooting, rotating subtrees, collapsing, and formatting nodes, edges and labels
- Consensus trees, and rooted phylogenetic networks computed from a set of trees: cluster networks, galled networks,
  minimum (level-k) networks and hybridization networks
- Tanglegrams of two trees or networks
- Reads and writes Newick (with the extended Newick notation for rooted networks), Nexus and NeXML, and its own
  `.dendro` format; exports drawings as images (PNG, SVG, PDF and others)
- A command-line mode, for running Dendroscope from scripts

## Installation

Download the installer for your computer from the latest release:

https://github.com/husonlab/dendroscope3/releases/latest

The installers include the Java runtime, so Java does not have to be installed separately.

- **macOS**: `Dendroscope-<version>-macos-aarch64.dmg` for Apple silicon (M1 and later),
  `Dendroscope-<version>-macos-x86_64.dmg` for Intel. Open the DMG file and drag Dendroscope into the Applications
  folder.
- **Windows**: `Dendroscope-<version>.msi`.
- **Linux**: the Debian package `dendroscope_<version>_amd64.deb` (or `_arm64.deb`), or the archive
  `Dendroscope-<version>-linux-x86_64.tar.gz` (or `-linux-aarch64.tar.gz`), which can be unpacked and run without
  administrator privileges.

**Help → Check For Updates...** in the program looks for a newer release and offers to install it.

Earlier releases were published on the server of the University of Tübingen; releases are now published here on
GitHub.

## Documentation

The [user manual](https://husonlab.github.io/dendroscope3/manual.pdf) describes the program and its commands.
The release notes are in [`release-notes/`](release-notes).

## Building from source

Dendroscope is built with Maven and needs a JDK, version 17 or later. It depends on the
[jloda3](https://github.com/husonlab/jloda3) library, which is not published in a Maven repository, so install that
first:

```bash
git clone https://github.com/husonlab/jloda3.git
(cd jloda3 && mvn install -DskipTests)
git clone https://github.com/husonlab/dendroscope3.git
cd dendroscope3
mvn package
```

Then run the program with

```bash
mvn javafx:run
```

`mvn package` puts the program in `target/` and the libraries it needs in `target/dependency/`. The installers are
built from these by a separate release workflow.

## Citation

If you use Dendroscope in your research, please cite:

Daniel H. Huson and Celine Scornavacca. Dendroscope 3: An interactive tool for rooted phylogenetic trees and networks.
Systematic Biology 61(6):1061–1067 (2012), [doi:10.1093/sysbio/sys062](https://doi.org/10.1093/sysbio/sys062).

## License

Dendroscope is distributed under the GNU General Public License v3 (see [LICENSE](LICENSE)).

## Contact

Daniel H. Huson  
University of Tübingen  
Institute for Bioinformatics and Medical Informatics
