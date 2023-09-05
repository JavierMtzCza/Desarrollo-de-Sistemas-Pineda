import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

   public static void main(String[] args) throws IOException {

      ServerSocket servidor = new ServerSocket(5000);

      System.out.println("Iniciando servidor en el puerto 5000");

      Socket conexion = servidor.accept();

      DataInputStream entrada = new DataInputStream(conexion.getInputStream());
      DataOutputStream salida = new DataOutputStream(conexion.getOutputStream());

      int n = entrada.readInt();
      System.out.println(n);

      double x = entrada.readDouble();
      System.out.println(x);

      salida.write("HOLA".getBytes());

      conexion.close();
      servidor.close();

   }
}
