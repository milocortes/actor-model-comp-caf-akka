#!/bin/bash

CORES=$1

for i in  100 250 500 1000

do


echo "************************************"
echo "Cores "$CORES
echo "************************************"
echo "Actores "$i
echo "************************************"

for j in {1..10};
do
echo "Iteración "$j

### Tiempos de ejecución para el proyecto de CAF
output_time="$( TIMEFORMAT='%R,%U,%S,%P';time ( ./caf-actor-car-V00 $i 1000 ) 2>&1 1>/dev/null )"

echo "Tiempo de ejecución ${output_time}"

### Guardaremos la salida NumProc, lenguaje, Real time, User time, System time, CPU percent

echo "$CORES,$i,$j,C++-CAF,$output_time">>output_time.txt

done

done
