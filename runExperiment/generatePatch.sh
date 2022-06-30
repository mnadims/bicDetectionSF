#!/bin/bash

#Extracting Patch for each Commits from subject system: 'ss'. 
while IFS= read -r ss
do
	#echo "Working for: "$ss
	cd "ss/"$ss;
	for commit in $(git rev-list --all)
	do
		echo "Working:"$ss": "$commit
		mkdir -p "../../ss_patch/"$ss
		git format-patch -1 $commit --stdout> "../../ss_patch/"$ss"/"$commit".txt";

	done
	cd "../../"
done < ssNames.txt
