#!/bin/bash

echo "Activamos los CPUS"

CPUS=$1

for ((i = 1; i <= $CPUS; i++)); do

    echo "Activamos el CPU $i"
    fname="/sys/devices/system/cpu/cpu$i/online"
    echo 1 >  "$fname"
done

echo "CPUs activados: $(expr $CPUS + 1)"

cat /sys/devices/system/cpu/online
