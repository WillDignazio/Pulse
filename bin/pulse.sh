#!/bin/bash
##
## Pulse Bootstrapping Scripts
##
VERSION="1.0"

if [ -z "$PULSE_HOME" ]; then
    echo "PULSE_HOME is not set, aborting.";
    exit 1;
fi

#
# We want to make sure we're using a predictable java
# environment. Look for JAVA_HOME, and then within it
# grab the java executable.
#
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set, aborting.";
    exit 1;
else
    for java in "$JAVA_HOME/bin/java"; do
        if [ -x "$java" ]; then
            JAVA="$java"
            break;
        fi
    done
fi

launch_pulse()
{
    PULSE_CLASSPATH="$PULSE_CLASSPATH:$PULSE_HOME/build/libs/pulse-$VERSION.jar"

    "$JAVA" $JVM_OPTS -classpath $PULSE_CLASSPATH net.digitalbebop.Pulse
}

launch_pulse
