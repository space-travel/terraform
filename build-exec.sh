#!/bin/bash
$GRAALVM_HOME/bin/native-image -H:+ReportUnsupportedElementsAtRuntime --no-server -jar terraform.jar
