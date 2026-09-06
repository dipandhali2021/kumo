# Third-party notices

App source is MIT-licensed (LICENSE). No external photo or complete dictionary dataset is bundled. The 50 starter words are concise educational sample data, not an import of JMdict.

## Build dependencies
- AndroidX / Jetpack, Google Material Icons, Kotlin/kotlinx.coroutines, Gradle: Apache License 2.0.
- JUnit 4 (tests): Eclipse Public License 1.0.
- org.json JSON-java (JVM tests only): JSON license, https://github.com/stleary/JSON-java .

Review resolved dependency notices before release. Apache: https://www.apache.org/licenses/LICENSE-2.0 ; EPL: https://www.eclipse.org/legal/epl-v10.html .

## Bundled Japanese fonts
Zen Kaku Gothic New and Zen Old Mincho use SIL Open Font License 1.1. Original regular/bold static TTFs and full copyright/license notices are fetched from google/fonts at immutable revision 5e35378e6bda803962ee6fd257e444a7d459660d. Expected Git blob hashes are checked before packaging. Fonts and notices ship under APK assets/fonts; About displays both complete notices. Fonts are not covered by the app's MIT license.

Sources:
- https://github.com/google/fonts/tree/5e35378e6bda803962ee6fd257e444a7d459660d/ofl/zenkakugothicnew
- https://github.com/google/fonts/tree/5e35378e6bda803962ee6fd257e444a7d459660d/ofl/zenoldmincho

## Optional vocabulary downloads
Provider: https://jlpt-vocab-api.vercel.app/ by wkei. https://github.com/wkei/jlpt-vocab-api/blob/main/data-source/README.md identifies Jonathan Waller's Tanos JLPT resources: https://www.tanos.co.uk/jlpt/ . No provider dataset or source code is bundled. Explicitly requested responses are cached privately for offline study. Public API availability does not establish a blanket redistribution license; provider data is not MIT-licensed by this app. Verify upstream terms before redistributing downloaded dictionaries or publishing a store release. Levels are third-party study classifications, not official JLPT vocabulary specifications.

If adding JMdict-derived data, follow EDRDG's applicable attribution/share-alike requirements: https://www.edrdg.org/edrdg/licence.html .
