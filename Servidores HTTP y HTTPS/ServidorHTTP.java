import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorHTTP {

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

                // Encabezados
                // while (true) {
                // String encabezado = entrada.readLine();
                // System.out.println(encabezado);
                // if (encabezado.equals(""))
                // break;
                // }

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
        ServerSocket servidor = new ServerSocket(50000);

        while (true) {
            Socket conexion = servidor.accept();
            Worker w = new Worker(conexion); // Creamos una instancia de un hilo
            w.start(); // iniciamos el proceso Run dentro de Worker
            w.join(); // Esperar a que termine el proceso del hilo
        }
    }

}
