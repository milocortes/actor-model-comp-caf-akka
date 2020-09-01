/*
Versión distribuida para el cálculo del coeficiente de clustering de una red dirigida con el patrón arquitectónico
Manager-Workers y el modelo de actor.

Para ejecutar el main del manager, quien será el servidor, ejecutar como sigue:

sbt  "-Dakka.remote.artery.canonical.port=2553" "runMain ManagerMain workers dim"

Donde workers es la cantidad de workers y dim es la cantidad de nodos

 */

import akka.actor.{ActorRef, ActorSystem, Props}

object ManagerMain extends App {

  // Inicializamos n_workers y n_nodos con los valores recibidos del usuario
  val n_workers = args(0).toInt
  val n_nodos = args(1).toInt

  // Definimos el sistema de actor para el Manager
  val system: ActorSystem = ActorSystem("ManagerSystem")

  // Creamos al actor manager
  val managerProps: Props = Props(classOf[Manager], n_workers, n_nodos)
  val manager: ActorRef =
    system.actorOf(managerProps, "manager")
}
