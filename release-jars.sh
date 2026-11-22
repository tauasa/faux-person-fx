#!/usr/bin/env bash
# Builds one runnable jar per platform into target/release/, then checks each
# holds that platform's JavaFX native libraries and no other platform's.
#
#   ./release-jars.sh
#
# The tests run once, on this machine's platform. The other jars are built
# with -Djavafx.platform, which tells the openjfx poms whose natives to fetch,
# and skip the tests, since their natives can't load here.
set -euo pipefail
cd "$(dirname "$0")"

version=$(mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout)
javafx=$(mvn -q -B help:evaluate -Dexpression=javafx.version -DforceStdout)
case "$version" in
  *-SNAPSHOT) echo "pom.xml is at $version; set a release version first." >&2; exit 1 ;;
esac

# release name : openjfx platform
platforms=(
  "windows-x64:win"
  "linux-x64:linux"
  "linux-aarch64:linux-aarch64"
  "macos-x64:mac"
  "macos-aarch64:mac-aarch64"
)

out=target/release
echo "Testing $version on this machine ..."
mvn -q -B clean verify
rm -rf "$out"
mkdir -p "$out"

for entry in "${platforms[@]}"; do
  name=${entry%%:*}
  fx=${entry#*:}
  jar="$out/faux-person-fx-$version-$name.jar"
  echo "Building $jar ..."
  mvn -q -B -DskipTests -Djavafx.platform="$fx" package
  cp target/faux-person-fx.jar "$jar"

  # The jar must carry javafx-graphics for this platform and for no other.
  # javafx-graphics-<version>.jar, with no platform, is an empty placeholder
  # every platform's build carries, so it's left out of the check.
  libs=$(unzip -Z1 "$jar" | grep -E '^BOOT-INF/lib/javafx-graphics-' \
    | grep -vx "BOOT-INF/lib/javafx-graphics-$javafx.jar" || true)
  want="BOOT-INF/lib/javafx-graphics-$javafx-$fx.jar"
  if [ "$libs" != "$want" ]; then
    echo "$jar has the wrong JavaFX natives:" >&2
    echo "  expected: $want" >&2
    echo "  found:    ${libs:-none}" >&2
    exit 1
  fi
done

(cd "$out" && shasum -a 256 ./*.jar | sed 's| \./| |' > SHA256SUMS)
echo
ls -l "$out"
