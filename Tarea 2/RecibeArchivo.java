import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLServerSocketFactory;

public class RecibeArchivo {

   static final int LONGITUD = 1024;
   static int DATAGRAMAS_RECIBIDOS = 0;
   static int SEGMENTOS;
   static boolean[] RECIBIDOS;

   static void recibe_datagramas(DatagramSocket socket, byte[] claveAES) throws Exception {
      System.out.println("Entramos a la funcion recibe_datagrama");
      while (true) {
         byte[] buffer = new byte[LONGITUD];
         boolean correcto;
         boolean duplicado = false;
         DatagramPacket paquete = new DatagramPacket(buffer, LONGITUD);

         while (true) {
            try {
               socket.receive(paquete);
               break;
            } catch (SocketTimeoutException e) {
               if (DATAGRAMAS_RECIBIDOS == SEGMENTOS)
                  return;
            }
         }

         ByteBuffer tramaCifrada = ByteBuffer.wrap(paquete.getData());
         System.out.println(Arrays.toString(tramaCifrada.array()));
         ByteBuffer tramaRecibida = descifrar(tramaCifrada, claveAES);
         int id = tramaRecibida.getInt(); // id del paquete
         byte[] datos = new byte[LONGITUD - 12]; // arreglo donde almacenemos la informacion
         tramaRecibida.get(datos);// obtenemos los datos
         long crcRecibido = tramaRecibida.getLong();// obtenemos el crc recibido

         System.out.println("Se recibio la trama " + id + " Con informacion: " + Arrays.toString(datos));

         correcto = verificarCRC32(paquete, crcRecibido); // verificamos si el crc del paquete es igual al crc recibido

         if (correcto) {
            duplicado = RECIBIDOS[id];
            if (!duplicado) {
               RECIBIDOS[id] = true;
               DATAGRAMAS_RECIBIDOS++;
            }
         }

         // Obtiene la dirección IP y el puerto del remitente
         InetAddress direccionIP = paquete.getAddress();
         int puerto = paquete.getPort();

         System.out.println(id + "\t" + (correcto ? "OK" : "Error") + "\t" + (duplicado ? "Duplicado" : "\t") + "\t"
               + direccionIP.toString() + "\t" + puerto);

         byte codigo = (byte) (correcto ? 100 : 200);

         // utiliza un ByteBuffer para empacar el código de respuesta y el CRC
         ByteBuffer tramRespuesta = ByteBuffer.allocate(9); // 1 byte del código de respuesta y 8 bytes para el CRC de
                                                            // la trama
         tramRespuesta.put(codigo);
         tramRespuesta.putLong(calcularCRC32(tramRespuesta)); // Creamos el CRC de

         DatagramPacket paqueteRespuesta = new DatagramPacket(tramRespuesta.array(), 9, direccionIP, puerto);

         socket.send(paqueteRespuesta);
      }
   }

   public static long calcularCRC32(ByteBuffer buffer) {
      CRC32 crc32 = new CRC32();
      crc32.update(buffer.array(), 0, 1);
      return crc32.getValue();
   }

   public static boolean verificarCRC32(DatagramPacket data, long crcEnviado) {
      CRC32 crcData = new CRC32();
      crcData.update(data.getData(), 0, LONGITUD - 8);
      return crcData.getValue() == crcEnviado;
   }

   public static ByteBuffer cifrar(ByteBuffer buffer, byte[] clave) throws Exception {
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Instanciamos un cifrador AES-256
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES")); // Inicializamos el cifrador con la clave
      return ByteBuffer.wrap(cipher.doFinal(buffer.array())); // Ciframos el contenido del buffer
   }

   public static ByteBuffer descifrar(ByteBuffer buffer, byte[] clave) throws Exception {
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Instanciamos un descifrador AES-256
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES")); // Inicializamos el descifrador con la clave
      return ByteBuffer.wrap(cipher.doFinal(buffer.array())); // Desciframos el contenido del buffer
   }

   public static void read(DataInputStream f, byte[] b, int posicion, int longitud) throws IOException {

      while (longitud > 0) {
         int n = f.read(b, posicion, longitud);
         posicion += n;
         longitud -= n;
      }

   }

   static class Worker extends Thread {
      private Socket socket;

      Worker(Socket socket) {
         this.socket = socket;
      }

      public void run() {
         try {
            // Recibir información del cliente
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            String nombreArchivo = inputStream.readUTF();
            long tamanoArchivo = inputStream.readLong();

            byte[] claveAES = inputStream.readNBytes(32);

            SEGMENTOS = (int) Math.ceil((double) tamanoArchivo / LONGITUD); // Calcular el número de segmentos
            RECIBIDOS = new boolean[SEGMENTOS]; // Inicializar el arreglo RECIBIDOS

            System.out.println(nombreArchivo);
            System.out.println(tamanoArchivo);
            System.out.println(Arrays.toString(claveAES));
            System.out.println(SEGMENTOS);
            socket.close();

            try {

               DatagramSocket socketDatagrama = new DatagramSocket(50001);
               socketDatagrama.setSoTimeout(500);

               recibe_datagramas(socketDatagrama, claveAES);

               socketDatagrama.close();
            } catch (Exception e) {
               e.printStackTrace();
            }

         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static void main(String[] args) throws Exception {

      System.setProperty("javax.net.ssl.keyStore", "keystore_serv.jks");
      System.setProperty("javax.net.ssl.keyStorePassword", "1234567");

      SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
      ServerSocket serverSocket = socketFactory.createServerSocket(50000);

      while (true) {
         Socket conexion = serverSocket.accept();
         Worker con = new Worker(conexion);
         con.start();
         con.join();
      }

   }
}
