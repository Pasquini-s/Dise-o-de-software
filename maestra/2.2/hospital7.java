import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


record Atencion(int numero, String terminal, LocalDateTime hora) {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public String toString() {
        return "N° " + numero + " | " + terminal + " | " + hora.format(FORMATO);
    }
}


final class GeneradorNumeros {

    private static final class Holder {
        static final GeneradorNumeros INSTANCIA = new GeneradorNumeros();
    }

    private int contador = 0;                                     
    private final List<Atencion> historial = new ArrayList<>();

    private GeneradorNumeros() { }

    public static GeneradorNumeros getInstancia() { return Holder.INSTANCIA; }

    
    public synchronized int solicitarNumero(String terminal) {
        if (terminal == null || terminal.isBlank()) {
            throw new IllegalArgumentException("La terminal no puede estar vacía");
        }
        contador++;
        historial.add(new Atencion(contador, terminal, LocalDateTime.now()));
        return contador;
    }

    
    public synchronized int ultimoNumero() { return contador; }

    public synchronized List<Atencion> historial() { return List.copyOf(historial); }
}

final class TerminalAtencion {
    private final String nombre;
    private final GeneradorNumeros generador = GeneradorNumeros.getInstancia();

    public TerminalAtencion(String nombre) { this.nombre = nombre; }

    public GeneradorNumeros getGenerador() { return generador; }

    public int pedirNumero() { return generador.solicitarNumero(nombre); }
}

public class Hospital07 {
    public static void main(String[] args) {
        TerminalAtencion t1 = new TerminalAtencion("Terminal 1");
        TerminalAtencion t2 = new TerminalAtencion("Terminal 2");
        TerminalAtencion t3 = new TerminalAtencion("Terminal 3");
        TerminalAtencion[] terminales = {t1, t2, t3};

        System.out.println("¿Las tres terminales usan el mismo generador? "
                + (t1.getGenerador() == t2.getGenerador() && t2.getGenerador() == t3.getGenerador()));
        System.out.println("Último número antes de empezar: " + GeneradorNumeros.getInstancia().ultimoNumero());

        
        final int SOLICITUDES = 24;
        List<Integer> entregados = new ArrayList<>();
        for (int i = 0; i < SOLICITUDES; i++) {
            entregados.add(terminales[i % 3].pedirNumero());
        }

        System.out.println("\nHistorial de números entregados:");
        GeneradorNumeros.getInstancia().historial().forEach(a -> System.out.println("  " + a));

        
        int u1 = GeneradorNumeros.getInstancia().ultimoNumero();
        int u2 = GeneradorNumeros.getInstancia().ultimoNumero();
        System.out.println("\nÚltimo número (consulta 1): " + u1);
        System.out.println("Último número (consulta 2): " + u2 + " -> no se incrementó");

        
        Set<Integer> unicos = new HashSet<>(entregados);
        boolean consecutivos = true;
        for (int i = 0; i < entregados.size(); i++) {
            if (entregados.get(i) != i + 1) consecutivos = false;
        }
        System.out.println("\nSolicitudes: " + SOLICITUDES + " | números distintos: " + unicos.size());
        System.out.println("¿Sin repetidos? " + (unicos.size() == SOLICITUDES));
        System.out.println("¿Consecutivos desde 1? " + consecutivos);

        
        System.out.println("\nPróximo número emitido: " + t2.pedirNumero());
    }
}
