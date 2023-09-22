import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocketFactory;

public class ServidorSSL {

    static class Worker extends Thread {
        Socket socket;

        Worker(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                System.out.println(entrada.readInt());
                socket.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Exception {

        System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket serverSocket = socketFactory.createServerSocket(8443);

        while (true) {
            Socket conexion = serverSocket.accept();
            new Worker(conexion).start();
        }

    }
}
