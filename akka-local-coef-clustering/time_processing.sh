#!/bin/bash

CORES=$1

for i in 100 250 500 1000

do

echo "************************************"
echo "Cores "$CORES
echo "************************************"
echo "Actores "$i
echo "************************************"


for j in {1..10};
do
echo "Iteración "$j

### Tiempos de ejecución para el proyecto de scala

sbt -Dsbt.task.timings=true  "run ${i} 1000"  > resultado.txt

output_time="$(cat resultado.txt | grep "clusteringakka / Compile / run    " | awk '{print $8}')"
### Guardaremos la salida NumProc, lenguaje, Real time, User time, System time, CPU percent

echo "$CORES,$i,$j,Scala-Akka,$output_time">>output_time.txt

done

done
