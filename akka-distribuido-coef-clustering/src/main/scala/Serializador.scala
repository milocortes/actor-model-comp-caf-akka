/*
Al contrario de la versión local del código, los mensajes deben ser serializados
para enviarse de forma remota. Akka proporciona varios seriazadores o permite
que el desarrollador personalice sus propios serializadores. Utilizamos el serializador
Jackson. Para que un mensaje pueda ser serialzado, se necesita definir una super clase
o un trait del cual extenderán los mensajes. En este caso, definimos el trait Serializador
 */
trait Serializador
