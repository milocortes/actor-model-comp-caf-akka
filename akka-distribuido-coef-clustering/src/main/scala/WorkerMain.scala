/*

 Versión distribuida para el cálculo del coeficiente de clustering de una red dirigida con el patrón arquitectónico
 Manager-Workers y el modelo de actor.

 Para ejecutar el main del worker, quien será el cliente, ejecutar como sigue:

 sbt  "-Dakka.remote.artery.canonical.port=2552" "runMain WorkerMain workers dim"

 Donde workers es la cantidad de workers y dim es la cantidad de nodos

 */

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Success}

object WorkerMain extends App {

  // Inicializamos n_workers y n_nodos con los valores recibidos del usuario
  val n_workers = args(0).toInt
  val n_nodos = args(1).toInt

  // Definimos el sistema de actor para el Manager
  val system: ActorSystem = ActorSystem("WorkerSystem")

  // Abrimos la conexión con el server en la dirección y puerto indicado
  val path =
    "akka://ManagerSystem@127.0.0.1:2553/user/manager"

  // Definimos el tiempo de espera del futuro
  implicit val timeout: Timeout = Timeout(5, SECONDS)

  // En caso que se encuentre al actor manager en la dirección dada,
  // generamos n_workers  workers.

  system.actorSelection(path).resolveOne().onComplete {
    case Success(manager) =>
      val workerGenerator = for (i <- 0 until n_workers) yield {
        system.actorOf(
          Props(classOf[Worker], manager, n_nodos),
          name = "Worker_" + i
        )
      }
      // Por cada worker generado, se envía el mensaje "Inicia"
      for (worker <- workerGenerator) {
        worker ! "Inicia"
      }

    case Failure(e) => println(e)
  }
}
