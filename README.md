# Pulse

## Configuration

Add the following line to your bashrc:

export PULSE_HOME=<path_to_source>/pulse
export PULSE_CONFIG=$PULSE_HOME/config

(then source ~/.bashrc if you just added them)

## Building

gradle idea	# For IDEA Intellij
gradle build
gradle writeClasspath

## Running

./bin/pulse.sh
