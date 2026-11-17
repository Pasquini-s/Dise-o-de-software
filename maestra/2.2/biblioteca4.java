import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


record Prestamo(String idEjemplar, String socio, LocalDate fecha) {
    @Override
    public String toString() {
        return "ejemplar " + idEjemplar + " -> socio " + socio + " (" + fecha + ")";
    }
}

final class AdministradorPrestamos {

    private static final class Holder {
        static final AdministradorPrestamos INSTANCIA = new AdministradorPrestamos();
    }

    private final Map<String, Prestamo> activos = new LinkedHashMap<>();   // clave: id de ejemplar

    private AdministradorPrestamos() { }

    public static AdministradorPrestamos getInstancia() { return Holder.INSTANCIA; }

    
    public boolean prestar(String idEjemplar, String socio, LocalDate fecha) {
        if (idEjemplar == null || idEjemplar.isBlank() || socio == null || socio.isBlank() || fecha == null) {
            throw new IllegalArgumentException("Ejemplar, socio y fecha son obligatorios");
        }
        if (activos.containsKey(idEjemplar)) {
            return false;                                        
        activos.put(idEjemplar, new Prestamo(idEjemplar, socio, fecha));
        return true;
    }

    
    public boolean devolver(String idEjemplar) {
        return activos.remove(idEjemplar) != null;
    }

    public boolean estaPrestado(String idEjemplar) { return activos.containsKey(idEjemplar); }

    public List<Prestamo> prestamosActivosDe(String socio) {
        return activos.values().stream().filter(p -> p.socio().equals(socio)).toList();
    }

    public int totalActivos() { return activos.size(); }
}


final class Mostrador {
    private final String nombre;
    private final AdministradorPrestamos administrador = AdministradorPrestamos.getInstancia();

    public Mostrador(String nombre) { this.nombre = nombre; }

    public AdministradorPrestamos getAdministrador() { return administrador; }

    public void prestar(String idEjemplar, String socio, LocalDate fecha) {
        boolean ok = administrador.prestar(idEjemplar, socio, fecha);
        System.out.println("  [" + nombre + "] Prestar " + idEjemplar + " a " + socio + " -> "
                + (ok ? "OK" : "RECHAZADO (el ejemplar ya está prestado)"));
    }

    public void devolver(String idEjemplar) {
        boolean ok = administrador.devolver(idEjemplar);
        System.out.println("  [" + nombre + "] Devolver " + idEjemplar + " -> "
                + (ok ? "OK" : "RECHAZADO (no tiene préstamo activo)"));
    }
}

public class Biblioteca04 {
    public static void main(String[] args) {
        Mostrador m1 = new Mostrador("Mostrador 1");
        Mostrador m2 = new Mostrador("Mostrador 2");
        LocalDate hoy = LocalDate.of(2026, 11, 17);

        System.out.println("¿Ambos mostradores usan el mismo administrador? "
                + (m1.getAdministrador() == m2.getAdministrador()));

        System.out.println("\nEl mostrador 1 presta la copia A de 'El Aleph':");
        m1.prestar("LIB-001-A", "Ana", hoy);

        System.out.println("\nEl mostrador 2 intenta prestar LA MISMA copia a otro socio:");
        m2.prestar("LIB-001-A", "Luis", hoy);

        System.out.println("\nEl mostrador 2 presta la copia B (mismo título, otro ejemplar físico):");
        m2.prestar("LIB-001-B", "Luis", hoy);

        System.out.println("\nPréstamos activos de Ana:");
        AdministradorPrestamos admin = AdministradorPrestamos.getInstancia();
        admin.prestamosActivosDe("Ana").forEach(p -> System.out.println("  " + p));
        System.out.println("Préstamos activos de Luis:");
        admin.prestamosActivosDe("Luis").forEach(p -> System.out.println("  " + p));

        System.out.println("\nDevolución rechazada de un ejemplar sin préstamo (no cambia el estado):");
        int antes = admin.totalActivos();
        m1.devolver("LIB-009-Z");
        System.out.println("  Préstamos activos antes: " + antes + " | después: " + admin.totalActivos());

        System.out.println("\nEl mostrador 1 registra la devolución de la copia A:");
        m1.devolver("LIB-001-A");

        System.out.println("\nAhora el mostrador 2 sí puede prestarla:");
        m2.prestar("LIB-001-A", "Luis", hoy);

        System.out.println("\nPréstamos activos de Luis:");
        admin.prestamosActivosDe("Luis").forEach(p -> System.out.println("  " + p));
    }
}
