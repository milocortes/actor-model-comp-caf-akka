import org.saddle._
import java.io.{FileReader, BufferedReader}

object Manager {

  // Definimos los mensajes del manager
  case class EnviarIndiceFila(index: Int, limiteSup: Int, limiteInf: Int)
  case class EnviaColumna(index: Int)
  case class RecibeCoefClustering(index: Int, coef_clustering: Double)

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
  def obtenMatriz(n: Int): Mat[Int] = {
    // El manager leerá  la matriz
    val file = new FileReader(
      "/home/milo/Documentos/CAR/2doSemestre/Seminario/scripts/tesina/scala/crearModelo/files/AdjMatrix_big_scala.txt"
    )

    val reader = new BufferedReader(file)

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

    val m = Mat(n, n, lamatrixInt)

    return m
  }
}

import akka.actor._

import Manager._
import Worker._

class Manager(n_workers: Integer, n_nodos: Integer) extends Actor {

  // Esta variable dará el id corresponiente a cada solicitud de los workers
  var id: Integer = 0
  // Esta variable servirá como contador para saber en qué momento el manager termina la ejecución de programa
  var contador: Integer = 0

  // El manager tendrá un arreglo de enteros en el que irá almacenando los resultados
  // enviados por los workers (en este caso, número de conexiones por nodo)
  var conex_por_nodo: Array[Int] = new Array[Int](n_nodos)

  // Obtenemos la matriz del archivo de texto
  val A = Manager.obtenMatriz(n_nodos)

  override def receive = {

    // Implementamos el comportamiento del manager ante el mensaje "Listo para la ejecución"
    case "Listo para la ejecución" => {

      val milista = Manager.calculaFilas(n_nodos, n_workers, id)
      // El manager responde al worker con una lista de dos elementos, uno corresponiente a la fila
      // de inicio y a la fila final que debe calcular el worker
      sender ! StartFilas(milista)

      id = id + 1
    }

    // Implementamos el comportamiento del manager ante el mensaje EnviarIndiceFila
    case EnviarIndiceFila(index: Int, limiteSup: Int, limiteInf: Int) => {
      val fila = A.row(index)

      // Por cada columna de la matriz de adyacencia, el manager las envía al worker
      // mediante el mensaje RecibirFila

      for (i <- 0 to (n_nodos - 1)) {
        val columna = A.col(i)

        sender ! RecibirFila(fila, index, columna, i)

      }

    }

    // Implementamos el comportamiento del manager ante el mensaje EnviaColumna
    case EnviaColumna(index: Int) => {
      // El manager envía la columna específica al índice enviado por el worker.
      val columna = A.col(index)
      // Para hacer el envío al worker, el manager lo hace con el mensaje CalculaCoef
      sender ! CalculaCoef(columna, index)
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
        println("Todos los workers calcularon sus coeficientes.")
        context.system.terminate
      }

    }

  }

}
