#!/bin/bash
# STARDICT_SANSKRIT_SCALA=`dirname $0`
PATH_TO_JARS=~/dict-tools
BABYLON_BINARY=${BABYLON_BINARY:-/usr/lib/stardict-tools/babylon}
GITHUB_TOKEN=${GITHUB_TOKEN:-NONE}
java -cp "$PATH_TO_JARS/bin/artifacts/dict-tools.jar" stardict_sanskrit.commandInterface makeIndicStardictTar  --urlBase=$1 --${2/DICTS=/dictPattern=}  --babylonBinary=$BABYLON_BINARY --githubToken=$GITHUB_TOKEN

