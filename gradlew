#!/bin/sh

# Gradle wrapper script for POSIX systems

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Resolve the project root directory
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# Download and run Gradle if not present
GRADLE_VERSION="8.2"
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "/tmp/gradle-${GRADLE_VERSION}.zip"
    unzip -q "/tmp/gradle-${GRADLE_VERSION}.zip" -d "$GRADLE_HOME"
    rm "/tmp/gradle-${GRADLE_VERSION}.zip"
fi

GRADLE_EXEC=$(find "$GRADLE_HOME" -name "gradle" -type f | head -1)

exec "$GRADLE_EXEC" "$@"
