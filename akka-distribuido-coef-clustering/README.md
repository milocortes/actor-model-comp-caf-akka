# Implementación distribuida con Akka
Se presenta el código de la implementación distribuida con Akka.


### Ejecución del programa

La implementación distribuida está compuesta por un servidor (Manager) y un Cliente (Worker). Para ejecutar el main del manager, quien será el servidor, ejecutar como sigue:

```
sbt  "-Dakka.remote.artery.canonical.port=2553" "runMain ManagerMain workers dim"
```
 Para ejecutar el main del worker, quien será el cliente, ejecutar como sigue:
 
```
 sbt  "-Dakka.remote.artery.canonical.port=2552" "runMain WorkerMain workers dim"
```

Donde ```workers``` es la cantidad de workers y ```dim``` es la cantidad de nodos.