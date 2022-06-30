#!/bin/bash

#Extracting Patch for each Commits from subject system: 'ss'. 
while IFS= read -r ss
do
	#echo "Working for: "$ss
	cd "ss/"$ss;
	while IFS= read -r commit
		do	
			echo "Working:"$ss": "$commit
			srcml ../../patch_source/$ss/added/$commit.cpp -o ../../source_xml/$ss/added/$commit.cpp.xml
			srcml ../../patch_source/$ss/deleted/$commit.cpp -o ../../source_xml/$ss/deleted/$commit.cpp.xml
			
		done < ../../qt_no_duplicates.txt
	cd "../../"
done < ssNames.txt






