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
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket serverSocket = socketFactory.createServerSocket(50000);

        while (true) {
            Socket conexion = serverSocket.accept();
            new Worker(conexion).start();
        }

    }
}
