import org.saddle._
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef
import Manager._

object Worker {

  // Definimos los mensajes del worker
  case class StartFilas(filas: List[Int])
  case class RecibirFila(
      fila: Vec[Int],
      indexFila: Int,
      columna: Vec[Int],
      indexColumna: Int
  )
  case class CalculaCoef(columna: Vec[Int], indexColumna: Int)
}

import akka.actor.{Actor, ActorRef, PoisonPill}
import Manager._
import Worker._
import org.saddle._

class Worker(manager: ActorRef, val n: Integer) extends Actor {

  // Este arreglo de enteros contendrá las filas que calculará cada worker
  var miArregloFilas: Array[Int] = _
  // Este arreglo de enteros contendrá el arreglo en 1D de la matriz A2 parcial del worker
  val A2_1D_Par = ArrayBuffer[Int]()
  // Este contador se utiliza para conocer en qué momento el worker comienza a realizar el cálculo de
  // A3ii y de coeficiente de clustering
  var contadorWorker: Int = 0

  override def receive = {

    // Implementamos el comportamiento del worker ante el mensaje "Inicia"
    case "Inicia" => {
      // EL worker envía al manager el mensaje "Listo para la ejecución"
      manager ! "Listo para la ejecución"
    }

    // Implementamos el comportamiento del worker ante el mensaje StartFilas
    case StartFilas(filas: List[Int]) => {
      // En este mensaje, el worker recibe la fila de inicio y final para el
      // cálculo del coeficiente de clustering

      miArregloFilas = filas(0) to filas(1) toArray

      // Por cada una de estas filas, el worker envía al manager el mensaje
      // EnviarIndiceFila para recibir cada una de las columnas de la matriz de adyancencia

      for (fila <- miArregloFilas) {
        manager ! EnviarIndiceFila(fila, filas(1), filas(0))
      }

    }

    // Implementamos el comportamiento del worker ante el mensaje RecibirFila
    case RecibirFila(
        fila: Vec[Int],
        indexFila: Int,
        columna: Vec[Int],
        indexColumna: Int
        ) => {

      // Obtenemos el producto punto de dos vectores y lo guardamos en A2_1D_Par
      val valor = fila dot columna

      A2_1D_Par += valor

      contadorWorker = contadorWorker + 1

      // Cuando el worker ha realizado todos los productos punto, puede comenzar a realizar el cálculo de A3ii y del coefiente
      if (contadorWorker == (miArregloFilas.size * n)) {
        for (fila <- miArregloFilas) {
          manager ! EnviaColumna(fila)
        }
      }

    }

    // Implementamos el comportamiento del worker ante el mensaje CalculaCoef

    // En este mensaje el worker recibe una fila del manager
    // para calcular A3ii y la cantidad de conexiones del nodo. Con esto, puede calcular el coeficiente
    // de clustering del nodo. El resultado lo envía al manager mediante el mensaje RecibeCoefClustering

    case CalculaCoef(columna: Vec[Int], index: Int) => {
      var filaIndex = miArregloFilas.indexOf(index)
      var A2_Par = Mat(miArregloFilas.size, n, A2_1D_Par.toArray)

      // Obtenemos el producto punto para calcular A3ii
      val valor = A2_Par.row(filaIndex) dot columna

      // Obtenemos la cantidad de conexiones del nodo
      val suma_conexiones = columna.sum

      // Calculamos el coeficiente de clustering
      val coef_clustering
          : Double = valor.toDouble / (suma_conexiones * (suma_conexiones - 1)).toDouble

      // Enviamos el resultado al manager mediante el mensaje RecibeCoefClustering
      manager ! RecibeCoefClustering(index, coef_clustering)
    }

  }

}
