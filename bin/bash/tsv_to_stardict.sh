#!/bin/sh
PATH_TO_SANSKRITNLPJAVA=~/sanskritnlpjava
BABYLON_BINARY=${BABYLON_BINARY:-/usr/lib/stardict-tools/babylon}
PATH_TO_JARS=~/dict-tools
java -cp "$PATH_TO_JARS/bin/artifacts/dict-tools.jar" stardict_sanskrit.commandInterface makeStardict --dictPattern=$1 --babylonBinary=$BABYLON_BINARY
