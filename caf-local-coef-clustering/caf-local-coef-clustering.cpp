/*
Versión local para el cálculo del coeficiente de clustering de una red dirigida con el patrón arquitectónico
Manager-Workers y el modelo de actor.

Ejecutar como sigue:

./caf-actor-car-V00 w d

donde w es la cantidad de workers y d es la cantidad de nodos
*/
#include <cassert>
#include <cstdint>
#include <set>
#include <string>
#include <utility>
#include <iostream>
#include <vector>
#include <fstream>
#include <boost/range/irange.hpp>
#include <boost/range/algorithm_ext/push_back.hpp>
#include <boost/range/numeric.hpp>

#include "caf/init_global_meta_objects.hpp"
#include "caf/all.hpp"

// Se definen los mensajes personalizados como struct
struct filas_asignadas;
struct receive_worker_row_col;
struct calcula_coef_data;

// Agregamos los mensajes al bloque principal de CAF
// También agregamos los atoms, los cuales definen el comportamiento del actor al recibir un mensaje.
CAF_BEGIN_TYPE_ID_BLOCK(mensajes_personalizados, first_custom_type_id)

  CAF_ADD_ATOM(mensajes_personalizados, get_id_atom)
  CAF_ADD_ATOM(mensajes_personalizados, envia_indice_fila_atom)
  CAF_ADD_ATOM(mensajes_personalizados, comenzar_cal_coef_atom)
  CAF_ADD_ATOM(mensajes_personalizados,calcula_coef_atom)
  CAF_ADD_ATOM(mensajes_personalizados,recibe_coef_atom)
  CAF_ADD_TYPE_ID(mensajes_personalizados, (filas_asignadas))
  CAF_ADD_TYPE_ID(mensajes_personalizados, (receive_worker_row_col))
  CAF_ADD_TYPE_ID(mensajes_personalizados, (calcula_coef_data))

CAF_END_TYPE_ID_BLOCK(mensajes_personalizados)

using namespace std;
using namespace caf;

// Definimos los atributos de los mensajes
// Mensaje filas_asignadas
struct filas_asignadas {
  std::vector<unsigned short int> a;
};

// Mensaje receive_worker_row_col
struct receive_worker_row_col{
  std::vector<unsigned short int> fila;
  std::vector<unsigned short int> columna;
  unsigned short int indexFila;
  unsigned short int indexColumna;
};

// Mensaje calcula_coef_data
struct calcula_coef_data{
  unsigned short int index;
  std::vector<unsigned short int> fila;
};

// Usamos la interface Inspector para serializar los mensajes.
// Serializamos el mensaje filas_asignadas
template <class Inspector>
typename Inspector::result_type inspect(Inspector& f, filas_asignadas& x) {
  return f(meta::type_name("filas_asignadas"), x.a);
}
// Serializamos el mensaje receive_worker_row_col
template <class Inspector>
typename Inspector::result_type inspect(Inspector& f, receive_worker_row_col& x) {
  return f(meta::type_name("receive_worker_row_col"), x.fila,x.columna,x.indexFila, x.indexColumna);
}
// Serializamos el mensaje calcula_coef_data
template <class Inspector>
typename Inspector::result_type inspect(Inspector& f, calcula_coef_data& x) {
  return f(meta::type_name("calcula_coef_data"), x.index,x.fila);
}

namespace{
  /*
  CAF proporciona distintas implementaciones para los actores, las cuales difieren en
  tres características:
  1) Tipado dinámico o estático.
  2) Basado en clases o basado en funciones.
  3) Un manejador de eventos asíncrono o recepciones bloqueantes.

  Estas tres características pueden ser combinadas con libertad, con solo una excepción:
  los actores estáticamente tipados son siempre basados en eventos.

  Una ventaja de usar actores estáticamente tipados es que permite al compilador verificar la
  comunicación entre los actores. Por esta razón, para la implementación se decidió utilizar
  este tipo de actores.

  Al utilizar actores estáticamente tipados el framework exige implementar una interface para los mensajes
  para permitir al compilador checar los tipos de los mensajes de la comunicación de los actores. Para parámetro
  del template define un insumo y un producto (e.g. replies_to<X1,...,Xn>::with<Y1,...,Yn>). Cuando hay insumos que
  no generan una salida, se utiliza reacts_to<X1,...,Xn>.
  */

  // Definimos la interface de mensajes del actor manager
  using manager_actor = typed_actor<replies_to<get_id_atom>::with<filas_asignadas>,
                        replies_to<envia_indice_fila_atom,unsigned short int,unsigned short int>::with<receive_worker_row_col>,
                        replies_to<calcula_coef_atom, unsigned short int>::with<calcula_coef_data>,
                        reacts_to<recibe_coef_atom,unsigned short int, float>>; //manager_actor

  // Definimos la interface de mensajes del actor worker
  using worker_actor = typed_actor<reacts_to<comenzar_cal_coef_atom>>; //worker_actor

  /*
  Se mencionó que definimos usar actores estaticamente tipados. Adicionalmente, estos actores están basados en funciones.
  CAF proporciona stateful actors para facilitar el mantener el estado de los actores basados en funciones. Se define el struct
  que contendrá el estado del actor, como sus atributos y métodos. Posteriormente, usamos  behavior_type  que es un conjunto
  estaticamente tipado de manejadores de mensajes para agregra el estado de los actores al comportamiento de los actores tipados.
  */

  // Definimos el struct del manager_state

  struct manager_state{
    // id funciona para determinar qué id le toca a cada solicitud del worker
    unsigned short int id=0;
    // Entero que indica la cantidad de actores
    unsigned short int no_actores;
    // Entero que indica la dimesión de la matriz
    unsigned short int dim_mat;
    // Contador de nodos recibidos para determinar cuándo el manager termina el programa
    unsigned short int nodos_recibidos;

    // Arreglo que contendrá la matriz de adyacencia
    vector<vector<unsigned short int>> mat_adj;

    void aumentaID() {
      id+=1;
    }

    void aumentaNodosRecibidos(/* arguments */) {
      nodos_recibidos+=1;
    }
    // Método que calcula qué filas le tocan a cada worker de acuerdo al id asignado
    vector<unsigned short int> calculaFilas(unsigned short int n,unsigned short int p,unsigned short int id){
      unsigned short int Nrow  = n / p;

      unsigned short int filaInicio = id * Nrow;
      unsigned short int filaFinal ;

      if (id < (p - 1)) {
        filaFinal = ((id + 1) * Nrow)  -1;
      } else {
        filaFinal = (n-1);
      }

      // Generamos un vector que contendrá los enteros entre filaInicio y filaFinal
      std::vector<unsigned short int> vector;
      boost::push_back(vector, boost::irange((unsigned short)filaInicio,(unsigned short) (filaFinal+1)));

      return vector;
    }

    // Método para leer el archivo de texto de la matriz de adyacencia
    vector<vector<unsigned short int>> read_matrix(unsigned short int n){
      fstream in("/home/milo/Documentos/CAR/2doSemestre/Seminario/scripts/tesina/scala/crearModelo/files/AdjMatrix_big.txt");
      int rows=n;
      int cols=n;
      vector<vector<unsigned short int>> matrix(rows, vector<unsigned short int>(cols));
      for (auto& row : matrix){
        for (auto& cell : row){
          in >> cell;
        }
      }
      return matrix;
    }

  };//manager_state

  // Definimos el struct del worker_state
    struct worker_state{
      // id del worker
      unsigned short int id;
      // dimensión de la matriz
      unsigned short int dim_mat;
      // Este contador nos dirá cuando ya podemos realizar el proceso de cálculo del coefiente
      unsigned short int contador_break;
      // Este contador nos acumulará cuántos Coeficientes de Clustering hemos enviado
      unsigned short int coef_enviados;
      // Este vectorde enteros contendrá las filas que calculará cada worker
      vector<unsigned short int> filas;
      // Este vector de enteros contendrá el arreglo en 1D de la matriz A2 parcial del worker
      vector<unsigned short int> A2_1D_Par;
      // Este vector de enteros contendrá el arreglo en 2D de la matriz A2 parcial del worker
      vector<vector<unsigned short int>> A2_2D_Par;

      void aumentaContador() {
        contador_break+=1;
      }

      void aumentaCoefEnviados() {
        coef_enviados+=1;
      }
      // Método para almacenar el producto punto de la multiplicación de vectores en un vector parcial
      void insertaValor(unsigned short int valor,unsigned short int fila,unsigned short int columna,unsigned short int n) {
        A2_1D_Par[(fila*n)+columna]=valor;
      }

      // Método para transformar un vector 1D a uno 2D
      vector<vector<unsigned short int>> convertir2D(vector<unsigned short int> d1_vector,unsigned short int rows,unsigned short int cols){

        vector<vector<unsigned short int>> d2_vector(rows, vector<unsigned short int>(cols));

        for (int i = 0; i < rows; i++) {
          for (int j = 0; j < cols; j++) {
            d2_vector[i][j]=d1_vector[(i*cols) + j];
          }
        }
        return d2_vector;
      }

    };// worker state

  manager_actor::behavior_type type_checked_manager (manager_actor::stateful_pointer<manager_state> self,unsigned short int no_actores,unsigned short int dim_mat ) {
    self->state.no_actores=no_actores;
    self->state.dim_mat=dim_mat;
    self->state.mat_adj=self->state.read_matrix(dim_mat);

   return {
     // Implementamos el comportamiento del manager ante el mensaje get_id_atom
     [=]( get_id_atom ) {

       /*
       Usamos el método calculaFilas para generar el vector con las filas que calculará el worker.
       El manager responde a este mensaje del worker con el mensaje filas_asignadas.
      */
       vector<unsigned short int> v=self->state.calculaFilas(self->state.dim_mat,self->state.no_actores,self ->state.id);
       self->state.aumentaID();

       return filas_asignadas{v};
     },
     /* Implementamos el comportamiento del manager ante el mensaje envia_indice_fila_atom.
        Este mensaje incorpora también el índice de la fila a solicitar así como el índice de la
        columna a solicitar
      */
     [=]( envia_indice_fila_atom,unsigned short int indexFila,unsigned short int indexColumna ) {

       // Obtenemos el vector fila del índice que necesita el worker
       std::vector<unsigned short int> fila= self->state.mat_adj[indexFila];
       // Obtenemos el vector columna del índice que necesita el worker
       std::vector<unsigned short int> columna= self->state.mat_adj[indexColumna];

       return receive_worker_row_col{fila, columna, indexFila,indexColumna};
     },
     /* Implementamos el comportamiento del manager ante el mensaje calcula_coef_atom.
        Este mensaje incorpora también el índice de la fila a solicitar.
      */
     [=]( calcula_coef_atom,unsigned short int indexFila) {

       // Obtenemos el vector fila del índice que necesita
       std::vector<unsigned short int> fila= self->state.mat_adj[indexFila];

       return calcula_coef_data{indexFila,fila};
     },
     /* Implementamos el comportamiento del manager ante el mensaje recibe_coef_atom, para el cual
        el manager recibe el valor calculado del coeficiente de clustering de un nodo por parte del
        worker
      */
     [=](recibe_coef_atom, unsigned short int indexNodo, float coef_clustering) {

       self->state.aumentaNodosRecibidos();
       aout(self)<< "Nodo: "<< indexNodo << ". Coeficientes de Clustering: " <<coef_clustering <<". Nodos recibidos: "<< self->state.nodos_recibidos<< "\n";

       // En caso que se hayan recibido los coeficientes de clustering de todos los nodos, el manager termina su ejecución
       if (self->state.nodos_recibidos==self->state.dim_mat) {
         aout(self)<< "He recibido todos los Coeficientes. Terminamos el programa" <<"\n";

         self->quit();
       }
     }
   };
}// type_checked_manager

  worker_actor::behavior_type type_checked_worker (worker_actor::stateful_pointer<worker_state> self,unsigned short int id,unsigned short int dim_mat, manager_actor ma){
    self->state.id=id;
    self->state.dim_mat=dim_mat;

    /*
    El worker comienza solicitud al manager con el mensaje get_id_atom, el cual regresa un vector de enteros con
    las filas que le corresponden a cada worker
    */
    self->request(ma,30s,get_id_atom_v).await(
      [=](filas_asignadas v){
        self->state.filas=v.a;

        // Hacemos un resize del vector A2_1D_Par con el vector recibido en el mensaje filas_asignadas
        self->state.A2_1D_Par.resize((self->state.filas.size())*self->state.dim_mat);

        /*
        Por cada elemento en el vector de filas, se solicitará al manager las columnas para calcular el producto punto
        de este por cada vector columna de la matriz de adyacencia
        */
        for (unsigned short int x : self->state.filas){
          for (unsigned short int j = 0; j < self->state.dim_mat; j++) {
            /*
            El worker solicita al manager el mensaje envia_indice_fila_atom, el cual regresa el vector fila y columna
            de los indices enviados por el worker. Con dichos vectores, el worker realiza el producto punto y almancena
            el resultado en el arreglo A2_1D_Par
            */
            self->request(ma,30s, envia_indice_fila_atom_v, x,j).await(
              [=](receive_worker_row_col mensaje){

                // Obtenemos el producto punto de dos vectores y lo guardamos en A2_1D_Par
                unsigned short int valor=inner_product(mensaje.fila.begin(),mensaje.fila.end(),mensaje.columna.begin(),0);
                self->state.insertaValor(valor, (x-(self->state.filas[0])), j, self->state.dim_mat);

                // Incrementamos el valor de contador_break en una unidad.
                self->state.aumentaContador();

                // Cuando el worker ha realizado todos los productos punto, puede comenzar a realizar el cálculo del coefiente
                if (self->state.contador_break==(int)(self->state.filas.size()*self->state.dim_mat)) {

                  // Convertimos A2_1D_Par en un arreglo 2D
                  self->state.A2_2D_Par=self->state.convertir2D(self->state.A2_1D_Par, (int)self->state.filas.size(),(int)self->state.dim_mat);

                  /*
                  Por cada una de las filas de este arreglo, el worker solicitará la fila correspondiente al manager
                  para calcular A3ii y la cantidad de conexiones del nodo. Con esto, puede calcular el coeficiente
                  de clustering del nodo. El resultado lo envía al manager mediante el mensaje recibe_coef_atom
                  */
                  for(unsigned short int i: self->state.filas){
                    /*
                    Solicitamos al manager el mensaje calcula_coef_atom, el cual regresa la fila de la matriz de adyacencia
                    del índice que le enviamos.
                    */
                    self->request(ma,30s,calcula_coef_atom_v,i).await(
                      [=](calcula_coef_data mensaje){

                        // Obtenemos el producto punto para calcular A3ii
                        unsigned short int valor=inner_product(mensaje.fila.begin(),mensaje.fila.end(),self->state.A2_2D_Par[mensaje.index-(self->state.filas[0])].begin(),0);
                        // Obtenemos la cantidad de conexiones del nodo
                        unsigned short int sum = boost::accumulate(mensaje.fila, 0);

                        // Calculamos el coeficiente de clustering
                        float coef_clustering= (float)valor/ (float)(sum*(sum-1));

                        // Aumentamos el contador de coeficientes calculados
                        self->state.aumentaCoefEnviados();

                        // Enviamos el resultado al manager mediante el mensaje recibe_coef_atom
                        self->send(ma,recibe_coef_atom_v,mensaje.index,coef_clustering);

                        // Cuando el worker ha enviado todos sus coeficientes, termina su ejecución
                        if (self->state.coef_enviados==(int)self->state.filas.size()) {
                          std::cout << "El worker "<< self->state.id << "  terminó el cálculo de sus coeficientes"  << '\n';
                          self->quit();
                        }
                      }

                    );
                  }
                }
              }
            );
          }
        }
      }
    );
    return {
      // Implementamos el comportamiento del manager ante el mensaje comenzar_cal_coef_atom
      [=]( comenzar_cal_coef_atom ) {
        std::cout << "Soy el worker "<< self->state.id<< " y comenzaré a calcular mis Coeficientes" << '\n';
        for (unsigned short int x : self->state.A2_1D_Par){aout(self) << x << " ";}
      }
    };
  }// type_checked_worker

}



int main(int argc, char** argv) {

  // Inicializamos la información global de los tipos de mensajes personalizados
  init_global_meta_objects<id_block::mensajes_personalizados>();
  core::init_global_meta_objects();

  // Con actor_system_config configuramos la aplicación
  actor_system_config cfg;

  // Con actor_system pasamos esta configuración al sistema de actor
  actor_system system{cfg};

  scoped_actor self{system};

  // Inicializamos no_actores y dim_mat con los valores recibidos del usuario
  int no_actores= atoi(argv[1]);
  int dim_mat= atoi(argv[2]);

  // Calculamos e imprimimos la cantidad de filas que calculará cada worker
  int Nrow=dim_mat/no_actores;
  std::cout << Nrow << '\n';

  // Generamos al manager
  auto manager=self->spawn(type_checked_manager,no_actores,dim_mat);

  /*
   Generamos tantos workers como el valor recibido en no_actores. Inmediatamete que son
  creados los workers, comienzan a solicitar sus filas al manager
  */
    for (size_t i = 0; i < (size_t)no_actores; i++) {
      self->spawn(type_checked_worker,i,dim_mat,manager);
    }

}
