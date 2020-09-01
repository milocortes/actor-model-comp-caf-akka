# Implementación local con CAF
Se presenta el código de la implementación local con CAF.

### Compilación del código

```
make
```
### Ejecución del programa

Con el archivo ``` run-exec-caf.sh``` realizamos la ejecución del programa con distintos números de cores para una matriz de adyacencia de 1000 X 1000. El argumento que recibe el archivo es la cantidad total de cores de nuestro equipo. Antes de correr el script debe dejar disponible un solo core. 

```
./run-exec-caf.sh num_cores
```