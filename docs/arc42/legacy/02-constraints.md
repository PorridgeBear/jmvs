# 02 Constraints

## Technical

| Constraint | Detail | Evidence |
|---|---|---|
| Java version | README: Java 11 "appears to be the latest that will support the JARs". `Files.readString` (Java 11) is used; no toolchain is pinned in Gradle. | README; `JMVSPluginRibbons` |
| Java3D 1.x binaries | Vendored jars `j3dcore`, `j3dutils`, `vecmath` in `app/libs`, loaded via `fileTree("libs")`. Not resolved from a repository. | `build.gradle.kts` |
| JOGL / GlueGen | `jogl-all`, `gluegen-rt` plus **macOS universal natives only**; README says to add jars for other architectures. | `app/libs`, README |
| Gradle | Wrapper-based `application` plugin (Gradle 7.5-era init); entry `com.adcworks.jmvs.App`. | `build.gradle.kts`, `gradle/wrapper` |
| Other deps | Guava 31.0.1 (declared; no usage found **[Assumption: unused]**), JUnit 5.8.2. | `build.gradle.kts` |
| UI toolkit | AWT `Frame`/`Panel`/`MenuBar`/`FileDialog`, with Swing `JLabel`/`JPanel` for the status bar (heavyweight/lightweight mixing). | `App`, `JMVSPanel` |
| Working directory | Ribbons loads shaders from relative path `src/main/resources/phong.*`; tests load `src/test/resources/1crn.cif`. Must run from `app/`. | `JMVSPluginRibbons`, `CIFReaderTest` |

## Organisational / conventions

| Constraint | Detail |
|---|---|
| Single maintainer, hobby project, no CI, no `.github/` | Repo listing |
| Input format | PDBx/mmCIF only | README "all-new loader" |
| Behavioural parity with RasMol | Bonding algorithm, radii, CPK indices copied from RasMol | `Model`, `elements.properties` |
| Licensing | `Sphere`, `Primitive`, `GeomBuffer` carry Sun Microsystems 1996–98 copyright headers (Java3D utility code); redistribution terms must be checked **[Assumption]** | file headers |

## Conventions seen

Package root `com.adcworks.jmvs`; configuration via `.properties` on the classpath; mixed tabs/spaces; `System.out` logging.
