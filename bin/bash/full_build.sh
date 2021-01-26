#!/bin/bash

# This is invoked from travis.yml scripts to autobuild dict packages for distribution.

# STARDICT_SANSKRIT_SCALA=`dirname $0`
PATH_TO_JARS=~/dict-tools
BABYLON_BINARY=${BABYLON_BINARY:-/usr/lib/stardict-tools/babylon}

if [ -f "$BABYLON_BINARY" ] 
  then echo "$BABYLON_BINARY exists." 
elif [ -f "/usr/bin/stardict-babylon" ]
  then BABYLON_BINARY=/usr/bin/stardict-babylon
fi

# Set default value
GITHUB_TOKEN=${3:-NONE}
java -cp "$PATH_TO_JARS/bin/artifacts/dict-tools.jar" stardict_sanskrit.commandInterface makeIndicStardictTar  --urlBase=$1 --${2/DICTS=/dictPattern=}  --babylonBinary=$BABYLON_BINARY --${GITHUB_TOKEN/GITHUB_TOKEN=/githubToken=}
