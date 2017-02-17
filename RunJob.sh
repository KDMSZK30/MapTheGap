#!/bin/bash -l
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <number of nodes> <number of tasks/node> [warszawa|polska]"
    exit
fi
echo "Running job on $1 nodes, $2 tasks/node.."
sbatch <<EOT
#!/bin/bash -l
#SBATCH --job-name  MtG
#SBATCH --output run.out
#SBATCH --error  run.err
#SBATCH --nodes $1
#SBATCH --ntasks-per-node $2
#SBATCH --time 12:00:00

module load java
srun hostname > nodes.txt

javac -d bin -sourcepath src -cp lib/PCJ-5.0.0.SNAPSHOT-bin.jar:lib/opencsv-3.8.jar:lib/graphhopper-core-0.8.2.jar:lib/graphhopper-osmreader-0.8.2.jar:lib/graphhopper-tools-0.8.2.jar src/MapTheGap.java
time srun -N $1 -n $1 -c $2 java -cp bin:lib/PCJ-5.0.0.SNAPSHOT-bin.jar:lib/opencsv-3.8.jar:lib/graphhopper-core-0.8.2.jar:lib/graphhopper-osmreader-0.8.2.jar:lib/graphhopper-tools-0.8.2.jar MapTheGap $3
EOT