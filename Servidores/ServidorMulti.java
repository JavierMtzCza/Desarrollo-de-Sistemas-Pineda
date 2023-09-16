import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorMulti {

    static class Worker extends Thread {
        Socket socket;

        Worker(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                int n = entrada.readInt();
                System.out.println(n);

                double x = entrada.readDouble();
                System.out.println(x);

                salida.write("HOLA".getBytes());

                socket.close();
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
