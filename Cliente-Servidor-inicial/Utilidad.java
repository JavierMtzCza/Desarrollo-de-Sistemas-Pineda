import java.io.DataInputStream;
import java.io.IOException;

public class Utilidad {

   public static void read(DataInputStream f, byte[] b, int posicion, int longitud) throws IOException {
      
      while (longitud > 0) {
         int n = f.read(b, posicion, longitud);
         posicion += n;
         longitud -= n;
      }
      
   }
}
