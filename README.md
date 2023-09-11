
  # Servidor HTTP 📝  
Un servidor HTTP es un servidor que recibe peticiones HTTP a traves de una conexion TCP

En general los sevidores HTTP son multithread, de tal manera que el servidor creara un thread cada vez que un cliente se conecta.
Por default un servidor HTTP recibe conexiones a traves del puerto 80.
Los metodos mas utilizados son PUT, POST, GET, DELETE.

  ## ServidorHTTP.java

#### Cambios al programa ServidorMultithread
Cuando el navegador hace peticiones, manda lo siguiente (_ es un espacio)
~~~bash  
Cliente: http://localhost/hola
Servidor: 
  String req= GET_/hola_HTTP/1.1(carry return ASCII 13)
~~~
  
Ademas de la peticion, el navegador envia otros encabezados terminando con una linea en blanco, para leerlos
~~~bash  
for(;;){
  String encabezado = entrada.readLine()
  sout(encabezado)
  if(encabezado.equals("")) break;
}
~~~
  
 Cuando recibamos una peticion, mandaremos un html como respuesta, ademas de ciertos encabezados como Content-Type, estatus de la consulta, Connection, etc. 
~~~bash  
if(req.startsWith("Get /hola ")){
  String contenido= "<html></html>"
  salida.println("HTTP/1.1 200 ok") estatus
  salida.println("Content-Type: text/html") estatus
  salida.println("connection: close") cierra la conexion
}else{
  salida.println("HTTP/1.1 404 not found") estatus
}
~~~

El navegador debe saber el tipo MIME del contenido para poedr interpretarlo correctamente

La forma general del encabezado Content-Type es la siguiente:
~~~bash
Content-Type: tipo/subtipo
~~~

Para desplegar la direccion IP del cliente que se conecto al servidor HTTP podemos incluir la siguiente instruccion al principio del metodo run():
~~~bash
sout(conexion.getRemoteSocketAddress(),toString())
~~~

#### Caché
Un servidor HTTP puede regresar al navegador una variedad de contenidos por ejemplo, una pagina web, un archivo, etc.
En general, estos recursos no se modifican con frecuencia en el servidor, por esta raon es conveniente que el navegador guarde una copia del recurso y solo reciba una actualizacion del recurso cuando este haya sido modificado.
Los navegadores almacenan copias de los recursos en e cache.

Para que el navegador guarde un recurso en el cache, el servidor envia al nevagador HTTP/1.1 200 ok, el encabezado "Last Modified"
~~~bash
  HTTP/1.1 200 ok
  Last-Modified: Fri, 25 Aug 2023 21:00:00 GMT  
~~~

Le dice a navegador que guarde el recurso en la cache utilizando asi mismo la fecha de a ultima modificacion.
Cada vez que el navegador solicita el recurso al servidor, el navgeador agregara el siguiente encabezado a la peticion:
~~~bash
 if-Modified-Since: Fri, 25 Aug 2023 21:00:00 GMT  
~~~

Esto le dice al servidor que el navgeador tiene una copia del recurso solicitado

#### Encabezado cache control

El encabezado Cache-Control es utilizado por el servidor para controlar el cache del navegador.
 
- Public: se almacena de forma publica.
- Private: El contenido debe almacenarse en forma privada.
- no-store: El contenido no debe almacenarse.
- no-cache: 
- mas-age=tiempo : indica el tiempo en segundos que se guaradara el contenido en la cache.
- inmutable: indica que el contenido no será modificado nunca por el servidor.