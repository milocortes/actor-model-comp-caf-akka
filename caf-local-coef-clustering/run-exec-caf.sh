#!/bin/bash

CPUS=$1

# Desactivamos los cores 


for ((i = 1; i < $CPUS; i++)); do
    if (( $i == 0 ))
	then
	echo "El CPU 0 ya está activado".
	./time_processing.sh $i
    fi
    ./activate-cpu.sh $i
    ./time_processing.sh $i

done

