import java.io.{FileReader, BufferedReader}
import breeze.linalg._
import breeze.numerics._

object Manager {
  // Definimos los mensajes del manager. Al contrario de la versión local,
  // en la versión distribuida los mensajes deben ser serialzados.
  // Usamos el serializador jackson-json, el cual se incorpora a los mensajes
  // al extender del trait Serializador
  case class EnviarIndiceFila(fila: Int, columna: Int) extends Serializador
  case class EnviaColumna(index: Int) extends Serializador
  case class RecibeCoefClustering(index: Int, coef_clustering: Double)
      extends Serializador

  // Método que calcula qué filas le tocan a cada worker de acuerdo al id asignado
  def calculaFilas(n: Int, p: Int, id: Int): List[Int] = {
    var Nrow: Int = n / p

    var filaInicio = id * Nrow
    var filaFinal = 0

    if (id < (p - 1)) {
      filaFinal = filaFinal + ((id + 1) * Nrow - 1)
    } else {
      filaFinal = filaFinal + (n - 1)
    }

    val misFilas = List(filaInicio, filaFinal)

    return misFilas
  }

  // Metodo que lee un archivo de texto dónde se encuentra la matriz
// de adyacencia y la convierte a una matriz

  def obtenMatriz(n: Int): DenseMatrix[Int] = {
    // El manager leerá  la matriz

    val file = new FileReader(
      "/home/milo/Documentos/CAR/2doSemestre/Seminario/github/akka-distribuido-coef-clustering/data/AdjMatrix_big_scala.txt"
    )

    val reader = new BufferedReader(file)

    ///////////////////////////////////////////////////////////////
    val mat1DString = new StringBuilder();

    try {
      var line: String = null
      while ({ line = reader.readLine(); line } != null) {
        mat1DString += ','
        mat1DString ++= line
      }

    } finally {
      reader.close()
    }

    val lamatrixString = mat1DString.toString().split(",").filter(_ != "")

    val lamatrixInt = lamatrixString.map(x => x.toInt)

    val m = new DenseMatrix(n, n, lamatrixInt)

    return m
  }

}

import java.util.{Currency, Locale}

import akka.actor.Actor
import akka.dispatch.RequiresMessageQueue
import akka.dispatch.BoundedMessageQueueSemantics

import Manager._
import Worker._

class Manager(n_workers: Integer, n_nodos: Integer) extends Actor {
  // Esta variable dará el id corresponiente a cada solicitud de los workers
  var id: Integer = 0
// Esta variable servirá como contador para saber en qué momento el manager termina la ejecución de programa
  var contador: Integer = 0

// El managaer tendrá un arreglo de enteros en el que irá almacenando los resultados
// enviados por los workers (en este caso, número de conexiones por nodo)
  var conex_por_nodo: Array[Int] = new Array[Int](n_nodos)

  // Obtenemos la matriz del archivo de texto
  val A = Manager.obtenMatriz(n_nodos)

  override def receive = {

    // Implementamos el comportamiento del manager ante el mensaje "GetId"
    case "GetId" => {
      val milista = Manager.calculaFilas(n_nodos, n_workers, id)
      var miArregloFilas = milista(0) to milista(1) toArray

      // El manager envía al worker el arreglo de filas que debe calcular mediante
      // el mensaje RecibirFila
      sender ! RecibirFila(miArregloFilas)
      id = id + 1

    }

    // Implementamos el comportamiento del manager ante el mensaje EnviarIndiceFila
    case EnviarIndiceFila(fila: Int, columna: Int) => {
      val fila_mat = A(::, fila)

      val columna_mat = A(::, columna)

      // Por cada solicitud del worker, el manager envía la fila y columna de acuerdo a
      // los indices enviados por el worker.
      sender ! RecibirFilaColumna(
        fila_mat.toArray,
        fila,
        columna_mat.toArray,
        columna
      )
    }

    // Implementamos el comportamiento del manager ante el mensaje EnviaColumna
    case EnviaColumna(index: Int) => {
      // El manager envía la columna específica al índice enviado por el worker.
      val columna = A(::, index)
      // Para hacer el envío al worker, el manager lo hace con el mensaje CalculaCoef
      sender ! CalculaCoef(columna.toArray, index)
    }

    // Implementamos el comportamiento del manager ante el mensaje RecibeCoefClustering
    // Con este mensaje, el manager recibe el coeficiente de clustering que calculó el worker
    case RecibeCoefClustering(index: Int, coef_clustering: Double) => {

      println(
        s"Para el nodo ${index} su coeficiente de clustering es ${coef_clustering}"
      )

      contador = contador + 1

      // Cuando el manager ha recibido todos los coeficientes de clustering, termina en programa
      if (contador == n_nodos) {
        println(
          "Todos los workers calcularon sus coeficientes."
        )
        context.system.terminate
      }

    }

  }
}
