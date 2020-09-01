/*
Versión local para el cálculo del coeficiente de clustering de una red dirigida con el patrón arquitectónico
Manager-Workers y el modelo de actor.

Ejecutar como sigue:

sbt  "run workers dim"

Donde workers es la cantidad de workers y dim es la cantidad de nodos

 */
import akka.actor.{ActorRef, ActorSystem, Props, PoisonPill}
import Worker._
import Manager._
import org.saddle._

import java.io.{FileReader, BufferedReader}

object Main extends App {

  // Inicializamos n_workers y n_nodos con los valores recibidos del usuario
  val n_workers = args(0).toInt
  val n_nodos = args(1).toInt

  // Definimos el sistema de actor
  val system: ActorSystem = ActorSystem("ManagerSystem")

  // Creamos al actor manager
  val managerProps: Props = Props(classOf[Manager], n_workers, n_nodos)
  val manager: ActorRef = system.actorOf(managerProps, "manager")

  // Generamos tantos workers como el valor recibido en n_workers. Inmediatamete que son
  // creados los workers, comienzan a solicitar sus filas al manager

  val workerGenerator = for (i <- 0 until n_workers) yield {
    system
      .actorOf(Props(classOf[Worker], manager, n_nodos), name = "Worker_" + i)
  }

  // Por cada worker generado, se envía el mensaje "Inicia"
  for (worker <- workerGenerator) {
    worker ! "Inicia"
  }

}
