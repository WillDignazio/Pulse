#!/bin/bash
##
## Pulse Bootstrapping Scripts
##
VERSION="1.0"

if [ -z "$PULSE_HOME" ]; then
    echo "PULSE_HOME is not set, aborting.";
    exit 1;
fi

if [ -z "$PULSE_CONFIG" ]; then
    echo "PULSE_CONFIG is not set, aborting.";
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
    fn_path="$PULSE_HOME/build/runtime.classpath"

    if [ -f "$fn_path" ]; then
        fn_classpath_content=$(cat "$PULSE_HOME/build/runtime.classpath")
    else
        echo "Unable to find runtime.classpath, maybe run gradle writeClasspath?"
    fi

    PULSE_CLASSPATH="$PULSE_CLASSPATH:$fn_classpath_content"
    JVM_OPTS="$JVM_OPTS -Dlog4j.properties=$PULSE_CONFIG/log4j.properties"
    "$JAVA" $JVM_OPTS -classpath $PULSE_CLASSPATH net.digitalbebop.Pulse
}

launch_pulse
