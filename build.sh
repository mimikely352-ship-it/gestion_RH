#!/usr/bin/env bash
# Wrapper qui force JAVA_HOME sur un JDK 17 pour cette invocation d'Ant,
# sans toucher au JAVA_HOME global de la machine.
# Surcharge via SIRH_JDK17_HOME si le JDK 17 est installe a un autre endroit.
set -e
export JAVA_HOME="${SIRH_JDK17_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"
ant "$@"
