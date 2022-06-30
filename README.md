# bicDetectionSF
**SourceToXML** Directory contains some sample files extracted in our investigation. 
**runExperiment** contains necessary source files to run/reproduce the experiment with new subjec systems. 
To extract source code syntax pattern features from GitHub Repositories perform the following steps in **./runExperiment** directory: 
#### Inputs #### 
ssNames.txt (Names of the subject systems) and gitURLs.txt (URLs of the subject systems) 

Generate necessary directories by taking names from a text file: ssNames.txt
```bash
python generateSSNames.py
```
Generate source code patch files
```bash
bash generatePatch.sh
```
Extract only the source code change lines. 
```bash
python onlySourcePatch.py
```
Convert extracted source code lines to XML representation
```bash
bash SourceToXML.sh
```

Following Jupyter Notebook files works for: 
1. **Preparing datasets and doing some basic preprocessing:** XML_to_Dataset.ipynb, save_gs_tp_ts_datasets.ipynb, PrepareDatasetPython.ipynb, joinDatasets.ipynb, and XML_to_Dataset_TS.ipynb
2. **Ranking the features:** FetureImportance.ipynb
3. **Run buggy commit detection detection models:** runML.ipynb
4. **Run PyExplainer Tool:** runPyExplainer-gs-best.ipynb, runPyExplainer-tp-best.ipynb

#### Contact Information: 
**Md Nadim**, Ph.D. Student, Computer Science, USASK, Canada. 
Email: **mnadims.cse@gmail.com**