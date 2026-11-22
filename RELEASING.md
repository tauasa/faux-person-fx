# Releasing

A release is a GitHub release with one runnable jar per platform. Nothing
goes to Maven Central.

1. On a branch, set `<version>` in `pom.xml` to the release (no `-SNAPSHOT`),
   merge it through a pull request, and pull `main`.
2. Build the jars:

   ```bash
   ./release-jars.sh
   ```

   It runs the tests on this machine, then builds
   `target/release/faux-person-fx-<version>-<platform>.jar` for
   `windows-x64`, `linux-x64`, `linux-aarch64`, `macos-x64` and
   `macos-aarch64`, checks each holds only its own platform's JavaFX
   libraries, and writes `SHA256SUMS`. It refuses a `-SNAPSHOT` version.
3. Tag `main` and publish, with the jars and the checksums:

   ```bash
   git tag -a v<version> -m "faux-person-fx <version>"
   git push origin v<version>
   gh release create v<version> target/release/* --title "faux-person-fx <version>" --notes-file <notes>
   ```

4. On a branch, set `<version>` to the next `-SNAPSHOT` and merge it.

Only the jar for the platform you build on has been started by the script's
tests. Start the others on their own platforms when you can.
