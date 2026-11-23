# `lib/` — in-project Maven repository

Libraries that Dendroscope needs and that are not published in any public Maven repository. `pom.xml` declares this
directory as the repository `dendroscope-lib`, so Maven resolves them like any other dependency, also on a build
machine that has nothing installed.

| artifact | what |
|---|---|
| `org.nexml:nexml:2015.07.19` | The NeXML Java API (`org.nexml.model`), used to read and write `.nexml` files (`dendroscope.io.nexml`). This is the jar that was kept as `jars/nexml.jar` until 2026; it was built on 19 July 2015 from a modified copy of the NeXML sources, so it is not replaceable by a later NeXML release without testing. Its JPMS module name is `nexml` (automatic, derived from the file name). |

To add a library, put the jar and a minimal pom under `lib/<groupId as path>/<artifactId>/<version>/`, named
`<artifactId>-<version>.jar` and `<artifactId>-<version>.pom`. Jars are ignored by git elsewhere, but not here.
