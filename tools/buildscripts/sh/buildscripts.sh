#!/usr/bin/env bash
# source this file to make these helper functions available

# usage:  build_version_jar <priority> <output dir>
# priority < 100 => will be listed first
# priority < 1000 => will be listed before qbt libraries
# most other things should be > 10000
function build_version_jar {
    PRIORITY="$1"
    DEST_DIR="$2"

    TMP_DIR="$(mktemp -d)"
    function cleanup {
        rm -rf "$TMP_DIR"
    }
    trap cleanup EXIT

    # produce the version jar to capture our CV
    mkdir -p "$TMP_DIR/META-INF/qbt"
    RELEASE_STRING="$PRIORITY $PACKAGE_NAME (cv $PACKAGE_CUMULATIVE_VERSION)"
    if [[ -n "$QBT_ENV_RELEASE_STRING" ]]; then
        RELEASE_STRING="$RELEASE_STRING release $QBT_ENV_RELEASE_STRING"
    fi

    echo "$RELEASE_STRING" > "$TMP_DIR/META-INF/qbt/version"
    jar cMf "$DEST_DIR/${PACKAGE_NAME}-version.jar" -C "$TMP_DIR" META-INF
}
