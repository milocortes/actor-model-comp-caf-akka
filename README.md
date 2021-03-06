# Comparativo del desempeño de los frameworks CAF (C++ Actor Framework) y Akka

Se propuso una solución parallela para el cálculo del coeficiente de clustering de los nodos de una red no dirigida, utilizando el patrón arquitectónico Manager-Workers  y el modelo de actor. Se implementó esta solución con los frameworks CAF y Akka. Para evaluar el desempeño de las dos implementaciones, realizamos una prueba a nivel local y otra de forma distribuida. Para la primera obtenemos que la implementación en Akka tiene un mejor desempeño en términos de tiempo ejecución, además de presentar speedup y eficiencia superior a la implementación en CAF. Para la prueba distribuida los resultados muestran que la implementación en CAF presenta un mejor desempeño tanto en tiempo de ejecución, speedup y eficiencia cuando agregamos un nodo worker adicional, mientras que en Akka empeora los tres indicadores. 

En este repositorio se encuentran dichas implementaciones. En cada sección se muestra la forma de compilar y ejecutar los programas.

![](compara-timeex-local.png) 
![](comp-speedud-local.png) 
![](comp-eficiencia.png) 
![](comp-distribuido.png) 