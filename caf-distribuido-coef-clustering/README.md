# Implementación distribuida con CAF
Se presenta el código de la implementación distribuida con CAF.

### Compilación de código

```
make
```
### Ejecución del programa

La implementación distribuida está compuesta por un servidor (Manager) y un Cliente (Worker). Para iniciar el servidor y comenzar a despachar las solicitudes de los workers, corra el siguiente comando:

```
./distributed-caf-car-V00 -s -w workerTotal -d nodos
```
Para iniciar el modo cliente:

```
./distributed-caf-car-V00  -w workerTotal -d nodos
```

Donde:

* ```-s``` indica que se está en modo server
* ```-d``` es la bandera de cantidad de nodos
* ```-w ```es la bandera de cantidad de workers
* ```nodos``` es la cantidad de nodos
* ```workerTotal``` es la cantidad total de workers a generar
* ```worker``` es la cantidad parcial de workers generados