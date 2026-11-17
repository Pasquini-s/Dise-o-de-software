import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


final class Logger {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final class Holder {
        static final Logger INSTANCIA = new Logger();
    }

    private Path archivo = Path.of(System.getProperty("java.io.tmpdir"), "app.log");

    private Logger() { }

    public static Logger getInstancia() { return Holder.INSTANCIA; }

    public synchronized void setArchivo(Path nuevo) { this.archivo = nuevo; }

    
    public static void info(String mensaje)    { Holder.INSTANCIA.escribir("INFO", mensaje); }
    public static void warning(String mensaje) { Holder.INSTANCIA.escribir("WARNING", mensaje); }
    public static void error(String mensaje)   { Holder.INSTANCIA.escribir("ERROR", mensaje); }

   
    private synchronized void escribir(String nivel, String mensaje) {
        String linea = "[" + LocalDateTime.now().format(FORMATO) + "] [" + nivel + "] " + mensaje;
        try {
            Files.writeString(archivo, linea + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir en el log: " + e.getMessage());
        }
        System.out.println(linea);
    }
}

public class Ejercicio03 {
    public static void main(String[] args) throws InterruptedException {
        Logger.getInstancia().setArchivo(
                Path.of(System.getProperty("java.io.tmpdir"), "demo.log"));

        Logger.info("Aplicación iniciada");
        Logger.warning("Memoria al 80%");
        Logger.error("Fallo de conexión con el servidor");

        
        Thread[] hilos = new Thread[3];
        for (int i = 0; i < hilos.length; i++) {
            final int n = i;
            hilos[i] = new Thread(() -> Logger.info("Mensaje desde el hilo " + n));
            hilos[i].start();
        }
        for (Thread h : hilos) h.join();
    }
}
