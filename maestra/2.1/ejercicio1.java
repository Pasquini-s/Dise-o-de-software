import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class Configuracion {

    private static final class Holder {
        static final Configuracion INSTANCIA = new Configuracion();
    }

    private final Map<String, String> parametros = new HashMap<>();
    private boolean cargado = false;

    private Configuracion() { }                       
    public static Configuracion getInstancia() {         
        return Holder.INSTANCIA;
    }


    public synchronized void cargar(Path archivo) throws IOException {
        if (cargado) {
            throw new IllegalStateException("La configuración ya fue cargada");
        }
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#") || !linea.contains("=")) continue;
            String[] kv = linea.split("=", 2);
            parametros.put(kv[0].trim(), kv[1].trim());
        }
        cargado = true;
    }

    public synchronized String obtener(String clave) {
        return parametros.get(clave);
    }

    public synchronized String obtener(String clave, String porDefecto) {
        return parametros.getOrDefault(clave, porDefecto);
    }
}

public class Ejercicio01 {
    public static void main(String[] args) throws IOException {
        Path archivo = Files.createTempFile("app", ".cfg");
        Files.writeString(archivo, "# configuración\nhost=localhost\npuerto=5432\n");

        Configuracion cfg = Configuracion.getInstancia();
        cfg.cargar(archivo);

        System.out.println("host: " + Configuracion.getInstancia().obtener("host"));
        System.out.println("puerto: " + Configuracion.getInstancia().obtener("puerto"));
        System.out.println("timeout (por defecto): " + cfg.obtener("timeout", "30"));
        System.out.println("misma instancia: " + (cfg == Configuracion.getInstancia()));

        try {
            cfg.cargar(archivo);
        } catch (IllegalStateException e) {
            System.out.println("Recarga bloqueada -> " + e.getMessage());
        }

        
        Files.deleteIfExists(archivo);
    }
}
