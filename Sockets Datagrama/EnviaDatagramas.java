import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import utils.encriptado;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.net.*;

public class EnviaDatagramas {

  static final int LONGITUD = 4 + 1012 + 8; // id+datos+crc32
  static final int N = 100; // número de datagramas a enviar (cada thread envía un datagrama)
  static byte[] clave;

  static class Worker extends Thread {
    int id; // identificador del paquete, notar que inicia en cero
    InetAddress direccionIP; // dirección IP del destinatario
    int puerto; // puerto que escucha el destinatario

    Worker(int id, InetAddress direccionIP, int puerto) {
      this.id = id;
      this.direccionIP = direccionIP;
      this.puerto = puerto;
    }

    public void run() {
      try {
        // Crea un socket de datagrama
        DatagramSocket socket = new DatagramSocket();

        // timeout para recepción de la respuesta en milisegundos
        socket.setSoTimeout(500);

        // construye los datos a enviar
        byte[] datos = new byte[LONGITUD - 12];
        for (int i = 0; i < LONGITUD - 12; i++)
          datos[i] = (byte) (i & 0xff);

        for (;;) {
          // utiliza un ByteBuffer para empacar el id del paquete, los datos y el CRC
          ByteBuffer bb = ByteBuffer.allocate(LONGITUD);
          bb.putInt(id);
          bb.put(datos);

          // calcula el CRC del id y de los datos
          CRC32 crc = new CRC32();
          crc.update(bb.array(), 0, LONGITUD - 8);
          bb.putLong(crc.getValue());

          // encriptar datagramas
          ByteBuffer bbEncriptado = cifrar(bb, clave);

          // Crea un paquete datagrama con el mensaje, la dirección IP y el puerto
          DatagramPacket paquete = new DatagramPacket(bbEncriptado.array(), LONGITUD, direccionIP, puerto);

          // Envía el paquete
          socket.send(paquete);

          // Prepara un paquete para recibir la respuesta
          byte[] respuesta = new byte[9]; // 1 byte del código de respuesta y 8 bytes del CRC del código
          DatagramPacket paqueteRespuesta = new DatagramPacket(respuesta, respuesta.length);

          try {
            // produce una excepción si no recibe la respuesta antes del timeout
            // si se produce la excepción se supone que el destinatario
            // no recibió el datagrama, entonces lo re-envía
            socket.receive(paqueteRespuesta);
          } catch (SocketTimeoutException e) {
            System.out.println("id=" + id + "\t" + "No se recibió respuesta, re-envía datagrama");
            continue;
          }

          // desempaca el código de respuesta y el CRC
          ByteBuffer bb_2 = ByteBuffer.wrap(paqueteRespuesta.getData());
          byte codigo = bb_2.get();
          long crc_enviado = bb_2.getLong();

          // calcula el CRC del código de respuesta
          CRC32 crc_2 = new CRC32();
          crc_2.update(paqueteRespuesta.getData(), 0, 1);

          // si el CRC enviado no es igual al CRC calculado, entonces re-envía el
          // datagrama
          if (crc_2.getValue() != crc_enviado) {
            System.out.println("id=" + id + "\t" + "Error de integridad en la respuesta, re-envía datagrama");
            continue;
          }

          // si recibe el código 100 el paquete se recibió correctamente, termina
          if (codigo == 100)
            break;

          System.out.println("id=" + id + "\t" + "Error de integridad, re-envía datagrama");
        }

        socket.close();
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
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

  public static void main(String[] args) {
    try {
      // Direccion IP de la maquina a la que se va a conectar
      InetAddress direccionIP = InetAddress.getByName("localhost");
      int puerto = 8080;
      Worker[] w = new Worker[N]; // cramos un arreglo de hilos

      clave = generarClaveAES256();

      for (int i = 0; i < N; i++)
        w[i] = new Worker(i, direccionIP, puerto);

      for (int i = 0; i < N; i++)
        w[i].start();

      for (int i = 0; i < N; i++)
        w[i].join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
