import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocketFactory;

public class ServidorHTTPS {

    static class Worker extends Thread {
        Socket socket;

        Worker(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream());

                String req = entrada.readLine();
                System.out.println(req);

                if (req.startsWith("GET /Hola ")) {
                    String contenido = "<html><button onClick='alert(\"Se presiono el boton\")'/></html>";
                    salida.println("HTTP/1.1 200 OK");
                    salida.println("Content-Type: text/html; charset=utf-8");
                    salida.println("Content-Length: " + contenido.length());
                    salida.println("Connection: close");
                    salida.println();
                    salida.println(contenido);
                    salida.flush();
                } else {
                    salida.println("HTTP/1.1 404 File Not Found");
                    salida.flush();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int puerto = 50000;
        System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "1234567");
        SSLServerSocketFactory socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket servidor = socket_factory.createServerSocket(puerto);

        while (true) {
            Socket conexion = servidor.accept();
            new Worker(conexion);
        }

    }
}
