import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.net.*;

public class RecibeDatagramas {
  final static int LONGITUD = 4 + 1012 + 8; // id+datos+crc
  final static int N = 100; // número de datagramas a recibir
  static boolean[] recibidos = new boolean[N]; // indica qué paquetes se han recibido
  static int numero_datagramas_recibidos = 0;

  static void recibe_datagramas(DatagramSocket socket) throws Exception {
    // recibe N datagramas
    for (;;) {
      byte[] buffer = new byte[LONGITUD];
      boolean correcto;
      boolean duplicado = false;

      // Crea un paquete datagrama vacío
      DatagramPacket paquete = new DatagramPacket(buffer, LONGITUD);

      for (;;)
        try {
          // Espera la recepción de un paquete
          // si se cunple el timeout entonces produce una excepción
          socket.receive(paquete);
          break;
        } catch (SocketTimeoutException e) {
          // si recibió N datagramas, termina
          if (numero_datagramas_recibidos == N)
            return;
        }

      // desempaca el id, los datos y el CRC
      ByteBuffer bb = ByteBuffer.wrap(paquete.getData());
      int id = bb.getInt();
      byte[] datos = new byte[LONGITUD - 12];
      bb.get(datos);
      long crc_enviado = bb.getLong();

      // calcula el CRC del id y los datos
      CRC32 crc = new CRC32();
      crc.update(paquete.getData(), 0, LONGITUD - 8);

      // si el CRC calculado es igual al CRC enviado entonces el paquete es correcto
      correcto = crc.getValue() == crc_enviado;

      // si el paquete es correcto marca el paquete como recibido
      // el paquete es duplicado si es correcto y ya se había recibido previamente
      if (correcto) {
        duplicado = recibidos[id];

        if (!duplicado) {
          recibidos[id] = true;
          numero_datagramas_recibidos++;
        }
      }

      // Obtiene la dirección IP y el puerto del remitente
      InetAddress direccionIP = paquete.getAddress(); // dirección IP del originario
      int puerto = paquete.getPort(); // puerto de origen, notar que cada paquete un puerto distinto

      System.out.println(id + "\t" + (correcto ? "OK" : "Error") + "\t" + (duplicado ? "Duplicado" : "\t") + "\t"
          + direccionIP.toString() + "\t" + puerto);

      // envía el código 100 si el paquete fue correcto, de lo contrario envía el
      // código 200
      // notar que la respuesta se envía aún si el paquete fue duplicado
      byte codigo = (byte) (correcto ? 100 : 200);

      // utiliza un ByteBuffer para empacar el código de respuesta y el CRC
      ByteBuffer bb2 = ByteBuffer.allocate(9); // 1 byte del código de respuesta y 8 bytes para el CRC del código
      bb2.put(codigo);

      // calcula el CRC del código de respuesta
      CRC32 crc_2 = new CRC32();
      crc_2.update(bb2.array(), 0, 1);
      bb2.putLong(crc_2.getValue());

      // Crea un nuevo paquete con la respuesta, la dirección IP y el puerto del
      // remitente
      DatagramPacket paqueteRespuesta = new DatagramPacket(bb2.array(), 9, direccionIP, puerto);

      // envía la respuesta
      socket.send(paqueteRespuesta);
    }
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
      // Crea un socket de datagrama y lo enlaza a un puerto específico
      DatagramSocket socket = new DatagramSocket(8080);

      // timeout para la recepción de datagramas viene en la clase socket
      socket.setSoTimeout(500);

      // recibe N datagramas
      recibe_datagramas(socket);

      socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
