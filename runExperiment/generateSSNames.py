#To extract system names from gitURL
from pathlib import Path

#fileLoc = "I:/01 Programming Data Backup/GitHub Repos/BIC Detection/New/"
fileLoc = ""
ssNames = ""
gURLs = open(fileLoc+"gitURLs.txt", "r").read().split('\n')
for g in gURLs:
    if(len(g)>0): #Ignore the last empty line....
        ss = g.split('/')[-1].split('.')[0]
        ssNames += ss+"\n"
        Path("ss_patch/"+ss).mkdir(parents=True, exist_ok=True)        
        Path("patch_source/"+ss+"/added/").mkdir(parents=True, exist_ok=True)
        Path("patch_source/"+ss+"/deleted/").mkdir(parents=True, exist_ok=True)
        Path("source_xml/"+ss+"/added/").mkdir(parents=True, exist_ok=True)
        Path("source_xml/"+ss+"/deleted/").mkdir(parents=True, exist_ok=True)
        Path("xml_gml/"+ss+"/added/").mkdir(parents=True, exist_ok=True)
        Path("xml_gml/"+ss+"/deleted/").mkdir(parents=True, exist_ok=True)
    
#print(ssNames)
with open('ssNames.txt', 'w') as f:
    f.write(ssNames)