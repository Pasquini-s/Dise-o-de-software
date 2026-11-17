import java.util.List;
import java.util.UUID;

final class GestorConexion {

    private static final class Holder {
        static final GestorConexion INSTANCIA = new GestorConexion();
    }

    private boolean conectado;
    private final String idConexion = UUID.randomUUID().toString().substring(0, 8);
    private int operaciones = 0;

    private GestorConexion() {
        abrir();                                         
    }

    public static GestorConexion getInstancia() {
        return Holder.INSTANCIA;
    }

    private void abrir() {
        System.out.println("  [BD] Abriendo conexión física " + idConexion);
        conectado = true;
    }

    public synchronized boolean estaConectado() { return conectado; }

    public synchronized String getIdConexion() { return idConexion; }

    public synchronized int getOperaciones() { return operaciones; }

    public synchronized List<String> ejecutarConsulta(String sql) {
        verificar();
        operaciones++;
        System.out.println("  [BD " + idConexion + "] SELECT -> " + sql);
        return List.of("fila1", "fila2");
    }

    public synchronized int ejecutarActualizacion(String sql) {
        verificar();
        operaciones++;
        System.out.println("  [BD " + idConexion + "] UPDATE -> " + sql);
        return 1;
    }

    public synchronized void cerrar() {
        if (conectado) {
            System.out.println("  [BD " + idConexion + "] Conexión cerrada");
            conectado = false;
        }
    }

    public synchronized void reconectar() {
        if (!conectado) abrir();
    }

    private void verificar() {
        if (!conectado) throw new IllegalStateException("La conexión está cerrada");
    }
}

public class Ejercicio02 {
    public static void main(String[] args) {
        GestorConexion bd1 = GestorConexion.getInstancia();   
        GestorConexion bd2 = GestorConexion.getInstancia();   
        bd1.ejecutarActualizacion("INSERT INTO usuarios VALUES (1, 'Ana')");
        bd2.ejecutarConsulta("SELECT * FROM usuarios");

        System.out.println("misma conexión: " + (bd1 == bd2));
        System.out.println("operaciones totales: " + bd2.getOperaciones());

        bd1.cerrar();
        System.out.println("estado visto desde bd2: conectado=" + bd2.estaConectado());

        try {
            bd2.ejecutarConsulta("SELECT 1");
        } catch (IllegalStateException e) {
            System.out.println("Consulta rechazada -> " + e.getMessage());
        }

        bd2.reconectar();
        System.out.println("tras reconectar: conectado=" + bd1.estaConectado());
    }
}
