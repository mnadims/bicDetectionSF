python generateSSNames.py
#Clone the respective repositories in the ss directory!!!
bash generatePatch.sh
python onlySourcePatch2-SCG.py
bash SourceToXML.sh
python XML_to_GML.py