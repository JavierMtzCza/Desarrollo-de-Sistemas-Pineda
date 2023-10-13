import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class EnviaArchivo {

   private static final String IP = "20.75.158.31"; // IP del servidor
   private static final int PUERTO_SSL = 50000; // Puerto del servidor SSL
   private static final int PUERTO_DATAGRAMA = 50001; // Puerto del servidor Datagrama
   private static final int LONGITUD = 1024; // Longitud de los paquetes
   private static final int MAXIMO_DE_HILOS = 100; // Máximo número de hilos

   static class Worker extends Thread {
      private int id;
      private int puerto;
      private InetAddress direccionIP;
      private byte[] segmento;
      private byte[] claveAES;

      Worker(int id, InetAddress direccionIP, int puerto, byte[] segmento, byte[] claveAES) {
         this.id = id;
         this.puerto = puerto;
         this.direccionIP = direccionIP;
         this.segmento = segmento;
         this.claveAES = claveAES;
      }

      public void run() {
         System.out.println("Se inicio el worker[" + id + "]");
         try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(500);

            while (true) {

               ByteBuffer trama = ByteBuffer.allocate(LONGITUD).putInt(id).put(segmento);
               trama.putLong(calcularCRC32(trama));
               ByteBuffer tramaEncriptada = cifrar(trama, claveAES);// Encriptar la trama usando AES-256

               DatagramPacket paquete = new DatagramPacket(tramaEncriptada.array(), LONGITUD, direccionIP, puerto);
               socket.send(paquete); // enviamos el paquete encriptado con la clave

               byte[] respuesta = new byte[9];
               DatagramPacket paqueteRespuesta = new DatagramPacket(respuesta, respuesta.length);

               try {
                  socket.receive(paqueteRespuesta);
               } catch (SocketTimeoutException e) {
                  System.out.println("id=" + id + "\t" + "No se recibió respuesta, re-envía datagrama");
                  continue;
               }

               ByteBuffer tramaRespuesta = ByteBuffer.wrap(paqueteRespuesta.getData());
               byte codigo = tramaRespuesta.get();
               long crcRespuesta = tramaRespuesta.getLong();

               if (verificarCRC32(paqueteRespuesta, crcRespuesta)) {
                  System.out
                        .println("id=" + id + "\t" + "Error de integridad en la respuesta, re-envía datagrama");
                  continue;
               }

               if (codigo == 100)
                  break;

               System.out.println("id=" + id + "\t" + "Error de integridad, re-envía datagrama");
            }

            socket.close();
         } catch (Exception e) {
            System.out.println("El worker[" + id + "] tuvo un error y es: ");
            e.printStackTrace();
         }
      }
   }

   public static void main(String[] args) throws Exception {

      if (args.length != 1) {
         System.err.println("Debes proporcionar la ubicación de un archivo como argumento.");
         return;
      }

      String archivoUbicacion = args[0];
      File archivo = new File(archivoUbicacion);

      if (!archivo.exists()) {
         System.err.println("El archivo especificado no existe.");
         return;
      }

      String nombreArchivo = archivo.getName();// Obtener el nombre del archivo y su tamaño
      long tamanoArchivo = archivo.length(); // ontenemos el tamano del archivo
      byte[] claveAES = generarClaveAES256();// Generar una clave AES-256

      // Propiedades del almacén de claves
      System.setProperty("javax.net.ssl.keyStore", "keystore_cli.jks");
      System.setProperty("javax.net.ssl.keyStorePassword", "123456.");
      System.setProperty("javax.net.ssl.trustStore", "truststore_cli.jks");
      System.setProperty("javax.net.ssl.trustStorePassword", "123456");

      System.out.println(Arrays.toString(claveAES));
      enviarInformacionAlServidor(nombreArchivo, tamanoArchivo, claveAES); // envimos la informacion por medio de
                                                                           // sockets seguros.

      AtomicInteger hilos = new AtomicInteger(0);
      Worker[] workers = new Worker[MAXIMO_DE_HILOS];
      int id = 0;

      try (FileInputStream archivoInput = new FileInputStream(archivo)) {
         byte[] buffer = new byte[LONGITUD - 12];
         int bytesRead;
         InetAddress direccionIP = InetAddress.getByName(IP);

         System.out.println("Enviando info");

         while ((bytesRead = archivoInput.read(buffer)) != -1) {

            if (bytesRead > 0) {
               workers[id] = new Worker(id, direccionIP, PUERTO_DATAGRAMA, buffer, claveAES);
               workers[id].start();
            }

            id++;
         }

      } catch (IOException e) {
         e.printStackTrace();
      }

      for (int i = 0; i < id; i++) {
         workers[i].join();
      }

   }

   public static byte[] generarClaveAES256() {
      SecureRandom random = new SecureRandom(); // Generamos un generador de números aleatorios seguro
      byte[] clave = new byte[32]; // Generamos un arreglo de bytes de 256 bits (32 bytes)
      random.nextBytes(clave);// Llenamos el arreglo de bytes con números aleatorios
      return clave;
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

   public static long calcularCRC32(ByteBuffer buffer) {
      CRC32 crc32 = new CRC32();
      crc32.update(buffer.array(), 0, LONGITUD - 8);
      return crc32.getValue();
   }

   public static boolean verificarCRC32(DatagramPacket data, long crcEnviado) {
      CRC32 crc32 = new CRC32();
      crc32.update(data.getData(), 0, 1);
      return crc32.getValue() != crcEnviado;
   }

   public static void enviarInformacionAlServidor(String nombreArchivo, long tamanoArchivo, byte[] claveAES)
         throws Exception {

      SSLSocketFactory cliente = (SSLSocketFactory) SSLSocketFactory.getDefault();
      Socket conexion = cliente.createSocket(IP, PUERTO_SSL);
      DataOutputStream salida = new DataOutputStream(conexion.getOutputStream());

      salida.writeUTF(nombreArchivo);// Enviar el nombre del archivo al servidor
      salida.writeLong(tamanoArchivo);// Enviar el tamaño del archivo al servidor
      salida.write(claveAES);// Enviar la clave AES-256 al servidor

      conexion.close();// Cerrar la conexión al finalizar
   }

}
