# Hackathon 2017 @ ICM : Wielkie Wyzwania Programistyczne

(www) http://akademia.icm.edu.pl/szkolenia/hackathon-map-the-gap

### Download (use Your ICM login/password)
(zip file:) https://git.icm.edu.pl/pk322266/MapTheGap/repository/archive.zip  
(repository:) git clone https://git.icm.edu.pl/pk322266/MapTheGap.git MapTheGap  

### Build & Run
- **manual** (make sure to prepare nodes.txt file)  
(eg. @Cray): module load java  
(build): javac -d bin -sourcepath src -cp lib/PCJ-5.0.0.SNAPSHOT-bin.jar:lib/opencsv-3.8.jar src/MapTheGap.java  
--> for more info, to the above command add: -Xlint:unchecked  
(run): java -cp bin:lib/PCJ-5.0.0.SNAPSHOT-bin.jar:lib/opencsv-3.8.jar MapTheGap [warszawa|polska]  

- **using RunJob.sh script** (Slurm)  
sh RunJob.sh <#nodes>  <#tasks per node> [warszawa|polska]  

### Project directory structure
- **bin/** (placeholder for compiled code)
- **data/** (input data files)
    - LTE1800_2016_12_27.csv : all BTS points (converted .xlsx -> .csv)
    - bp_konkurs_2_20160912_Warszawa.csv : Blind spots (only Warsaw)
- **lib/** (libraries, see lib/Info.txt)
- **maps/** (maps & visualisations)
    - GoogleMap_template.js : template of interactive Google map
- **src/** (project source files)  
