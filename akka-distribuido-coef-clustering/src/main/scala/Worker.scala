import breeze.linalg._
import breeze.numerics._
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef
import Manager._

object Worker {
  // Definimos los mensajes del worker. Al contrario de la versión local,
  // en la versión distribuida los mensajes deben ser serialzados.
  // Usamos el serializador jackson-json, el cual se incorpora a los mensajes
  // al extender del trait Serializador

  case class StartFilas(filas: List[Int]) extends Serializador
  case class RecibirFila(fila: Array[Int]) extends Serializador
  case class RecibirFilaColumna(
      fila: Array[Int],
      indexFila: Int,
      columna: Array[Int],
      indexColumna: Int
  ) extends Serializador
  case class CalculaCoef(columna: Array[Int], indexColumna: Int)
      extends Serializador
}

import breeze.linalg._
import breeze.numerics._
import akka.actor.{Actor, ActorRef}

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import Manager._
import Worker._

class Worker(manager: ActorRef, val n: Integer) extends Actor {
  // Este arreglo de enteros contendrá las filas que calculará cada worker
  var miArregloFilas: Array[Int] = _

  // Este arreglo de enteros contendrá el arreglo en 1D de la matriz A2 parcial del worker
  val A2_1D_Par = ArrayBuffer[Int]()

  // Este contador se utiliza para conocer en qué momento el worker comienza a realizar el cálculo de
  // A3ii y de coeficiente de clustering
  var contadorWorker: Int = 0

  var coeficientesEnviados: Int = 0

  override def receive = {
    // Implementamos el comportamiento del manager ante el mensaje "Inicia"
    case "Inicia" => {
      // Para la versión distribuida, utilizamos Futuros. En Akka, hay dos formas
      // de recibir una respuesta de un Actor. La primera es al enviar un mensaje
      // del tipo actor ! msg, que funciona únicamente si el emisor original fue
      // un actor. La segunda es mediante Futuros. Usando actor ? msg para enviar un
      // mensaje nos regresará un futuro. De esta forma, enviaremos el mensaje y
      // esperaremos el resultado real más tarde. Esto causará que el hilo actual se
      // bloqué y espere al actor hasta que haya completado el Futuro con la respuesta.
      // Las operaciones bloqueantes son Await.result y Await.ready, lo que facilita
      // identificar en qué parte se realizó el bloqueo.

      // Definimos el tiempo de espera del futuro
      implicit val timeout = Timeout(10 seconds)
      // Enviamos el mensaje al manager y obtendremos un futuro.
      val future = manager ? "GetId"
      // El resultado del futuro estará en la variable result
      val result =
        Await.result(future, timeout.duration).asInstanceOf[RecibirFila]

      // Cuando está listo el resultado, obtenemos la fila que ha enviado el manager
      miArregloFilas = result.fila

      // Por cada elemento en el vector de filas, se solicitará al manager las
      // columnas para calcular el producto punto de este por cada vector columna
      // de la matriz de adyacencia
      for (i <- 0 until result.fila.length) {
        for (j <- 0 to (n - 1)) {
          var fila = result.fila(i)
          println(s"Para la fila ${fila} solicito la columna ${j}")

          //El worker solicita al manager el mensaje EnviarIndiceFila, el cual regresa el vector fila y columna
          //de los indices enviados por el worker. Con dichos vectores, el worker realiza el producto punto y almancena
          //el resultado en el arreglo A2_1D_Par

          val future_indice_fila_columna = manager ? EnviarIndiceFila(fila, j)
          val result_indice_fila_columna =
            Await
              .result(
                future_indice_fila_columna,
                timeout.duration
              )
              .asInstanceOf[RecibirFilaColumna]
          // Convertimos en vector el arreglo de enteros recibido
          var fila_vector = DenseVector(result_indice_fila_columna.fila)
          // Convertimos en vector el arreglo de enteros recibido
          var columna_vector = DenseVector(result_indice_fila_columna.columna)

          val suma_conexiones = sum(fila_vector)

          // Obtenemos el producto punto de dos vectores y lo guardamos en A2_1D_Par
          val valor = fila_vector dot columna_vector
          A2_1D_Par += valor

          contadorWorker = contadorWorker + 1

          // Cuando el worker ha realizado todos los productos punto, puede comenzar a realizar el cálculo del coefiente
          if (contadorWorker == (miArregloFilas.size * n)) {

            //Por cada una de las filas de este arreglo, el worker solicitará la fila correspondiente al manager
            //para calcular A3ii y la cantidad de conexiones del nodo. Con esto, puede calcular el coeficiente
            //de clustering del nodo. El resultado lo envía al manager mediante el mensaje RecibeCoefClustering

            for (fila <- miArregloFilas) {

              // Enviamos al manager el mensaje EnviaColumna para recibir un futuro
              val future_envia_columnas = manager ? EnviaColumna(fila)
              val result_envia_columnas =
                Await
                  .result(future_envia_columnas, timeout.duration)
                  .asInstanceOf[CalculaCoef]

              // Con el resultado del futuro, obtenemos el índice de la columna que está enviando
              // el manager.
              var filaIndex =
                miArregloFilas.indexOf(result_envia_columnas.indexColumna)

              // Convertimos el arreglo A2_1D_Par en una matriz con dimensión n_filas X n
              var A2_Par =
                new DenseMatrix(miArregloFilas.size, n, A2_1D_Par.toArray)
              // Convertimos en vector el arreglo de enteros que está enviando el manager
              var columna_vector = DenseVector(result_envia_columnas.columna)

              // Obtenemos el producto punto para calcular A3ii
              val valor = A2_Par(filaIndex, ::).t dot columna_vector
              // Obtenemos la cantidad de conexiones del nodo
              val suma_conexiones = sum(columna_vector)
              // Calculamos el coeficiente de clustering
              val coef_clustering
                  : Double = valor.toDouble / (suma_conexiones * (suma_conexiones - 1)).toDouble
              // Enviamos el resultado al manager mediante el mensaje RecibeCoefClustering
              manager ! RecibeCoefClustering(
                result_envia_columnas.indexColumna,
                coef_clustering
              )

              coeficientesEnviados += 1
              // Cuando el worker ha enviado todos sus coeficientes, termina su ejecución
              if (coeficientesEnviados == miArregloFilas.size) {
                println("El worker ha terminado de calcular sus coeficientes.")
                context.system.terminate

              }

            }
          }

        }
      }
    }

  }

}
