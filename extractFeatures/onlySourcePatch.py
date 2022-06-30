import os

def findAddedDeletedLines(fpath):
    print(fpath)
    f = open(fpath, "r", encoding='utf-8', errors='ignore')
    file_lines=f.read().split("\n") 
    addedLines = ""
    deletedLines = ""
    file_ext=""
    for i in range(len(file_lines)):
        line=file_lines[i]        
        if(line[0:3]=='---' ):
            ln_parts=line.split('.')        
            file_ext=ln_parts[len(ln_parts)-1]
            if(file_ext in ['c', 'java', 'cpp', 'h', 'cc', 'hpp', 'py']):
                #if(file_ext not in file_extensions):                
                while(line[0:4]!='diff' and i<len(file_lines)):                                    
                    #--------------------------------------------
                    if(len(line)>2):
                        if(line[0]=='-' and line[1]!='-' and line[1:].strip()[0:2]!='//'):
                            deletedLines += line[1:].strip() +'\n'
                        elif(line[0]=='+' and line[1]!='+' and line[1:].strip()[0:2]!='//'):
                            addedLines += line[1:].strip() +'\n'
                    #--------------------------------------------
                    
                    line=file_lines[i]
                    i+=1 
            
    #if(file_ext!=""):
    #    if(file_ext!='java'):
    #        file_ext = 'cpp'
    #print(len(line_num), line_num)
    #print(len(line_type), line_type)    

    return "py", addedLines, deletedLines

def main(): 
    fileContent = open("ssNames.txt", "r", encoding='utf-8').read()
    #print("Length of File Content", len(fileContent))
    ssNames = fileContent.split('\n')

    for ss in ssNames:
        if(len(ss.strip()) == 0): #Ignore the last empty line....
            break
        commitFiles = os.listdir("ss_patch/"+ss+"/")
        #commit_hash=open("commit_sha_"+ss+".txt", "r", encoding='utf-8').read().split("\n") 
        count=0
        num_commits=len(commitFiles)
        for c in commitFiles:
            count+=1
            if(count%50 == 0):
                #clear_output(wait=True)
                print("Working for:", ss, "(", count, "/", num_commits, ")", c)      

            fpath="ss_patch/"+ss+"/"+c            
            if(len(fpath)>0 and os.path.exists(fpath)):
                file_ext, addedLines, deletedLines = findAddedDeletedLines(fpath)

                with open("patch_source/"+ss+"/added/"+c[0:-3]+file_ext, "w", encoding="utf8") as text_file:
                                                text_file.write(addedLines)

                with open("patch_source/"+ss+"/deleted/"+c[0:-3]+file_ext, "w", encoding="utf8") as text_file:
                                                text_file.write(deletedLines)

if __name__ == "__main__":
    main()