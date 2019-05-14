#! /bin/bash
shopt -s expand_aliases
source ~/.bash_aliases.laptop


GIT_MODE=$1 # gp or git pull
set -o verbose
shopt -s expand_aliases
source ~/.bash_aliases
CUR_DIR=$(pwd)
FREQUENT_REPOS=(~/indic-dict/stardict-sanskrit/ ~/indic-dict/stardict-ayurveda/ ~/indic-dict/stardict-sanskrit-kAvya/ ~/indic-dict/stardict-sanskrit-vyAkaraNa/ )
INFREQUENT_REPOS=(~/indic-dict/stardict-tamil/ ~/indic-dict/stardict-malayalam/ ~/indic-dict/stardict-kannada/ ~/indic-dict/stardict-telugu/  ~/indic-dict/stardict-pali/ ~/indic-dict/stardict-hindi/ ~/indic-dict/stardict-marathi/ ~/indic-dict/stardict-oriya/ ~/indic-dict/stardict-panjabi/ ~/indic-dict/stardict-sinhala/ ~/indic-dict/stardict-assamese/ ~/indic-dict/stardict-nepali/ ~/indic-dict/stardict-english/  )
REPOS=(${FREQUENT_REPOS[@]} ${INFREQUENT_REPOS[@]})
for repo in "${REPOS[@]}"; 
do echo processing: $repo;
	cd $repo
	echo doing: $GIT_MODE
	$GIT_MODE
done;
cd $CUR_DIR
