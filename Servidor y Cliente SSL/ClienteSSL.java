import java.io.DataOutputStream;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;

public class ClienteSSL {
    public static void main(String[] args) throws Exception {

        System.setProperty("javax.net.ssl.keyStore", "keystore_cliente.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLSocketFactory cliente = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket conexion = cliente.createSocket("40.88.227.203", 50000);
        DataOutputStream salida = new DataOutputStream(conexion.getOutputStream());

        Thread.sleep(1000);
        conexion.close();

    }

    

}
