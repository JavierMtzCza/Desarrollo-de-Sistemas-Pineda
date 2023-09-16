import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket; 

public class ClienteIntentosConexion {
   public static void main(String[] args) throws InterruptedException, IOException {
          // Creamos una instancia de Socket que tiene como argumentos
      // Socket(DIRECCION_DEL_SERVIDOR, PUERTO)
      Socket conexion = null;
      while(true){
         try {
            conexion = new Socket("localhost", 50000);
            break;
         } catch (Exception e) {
            Thread.sleep(1000);
         }
      }

      // Creamos un Stream de salida y entrada
      DataOutputStream salida = new DataOutputStream(conexion.getOutputStream());

      DataInputStream entrada = new DataInputStream(conexion.getInputStream());

      salida.writeInt(123);
      salida.writeDouble(123456789.123456789);

      //Creamos uin buffer de bytes de longitud 4 para un string de 4 
      byte[] buffer = new byte[4];
      //Usamos la funcion de lectura con el DataInput entrada
      //el buffere creadp. la posicion 0 y longitud igual a la cadena que queremos enviar 
      Utilidad.read(entrada, buffer, 0, 4);
      //El buffer con el mensaje lo convertimos a una instancia de String con codificacion UTF-8
      System.out.println(new String(buffer, "UTF-8"));

      //cerramos la conexion
      Thread.sleep(1000);
      conexion.close();  
   }
}
