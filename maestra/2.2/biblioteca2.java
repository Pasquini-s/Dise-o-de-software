import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


record Actividad(LocalDateTime fechaHora, String sector, String descripcion) {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public String toString() {
        return "[" + fechaHora.format(FORMATO) + "] " + sector + ": " + descripcion;
    }
}


final class RegistroActividades {

    private static final class Holder {
        static final RegistroActividades INSTANCIA = new RegistroActividades();
    }

    
    private final List<Actividad> actividades = new ArrayList<>();

    private RegistroActividades() { }

    public static RegistroActividades getInstancia() { return Holder.INSTANCIA; }

    public synchronized void registrar(String sector, String descripcion) {
        if (sector == null || sector.isBlank()) {
            throw new IllegalArgumentException("El sector no puede estar vacío");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        actividades.add(new Actividad(LocalDateTime.now(), sector.trim(), descripcion.trim()));
    }

    
    public synchronized List<Actividad> historial() {
        return List.copyOf(actividades);
    }

    public synchronized List<Actividad> filtrarPorSector(String sector) {
        return actividades.stream()
                .filter(a -> a.sector().equalsIgnoreCase(sector))
                .toList();                                
    }

    public synchronized int total() { return actividades.size(); }
}


final class SectorBiblioteca {
    private final String nombre;
    private final RegistroActividades registro = RegistroActividades.getInstancia();

    public SectorBiblioteca(String nombre) { this.nombre = nombre; }

    public RegistroActividades getRegistro() { return registro; }

    public void registrar(String descripcion) { registro.registrar(nombre, descripcion); }
}

public class Biblioteca02 {
    public static void main(String[] args) {
        SectorBiblioteca prestamos = new SectorBiblioteca("Préstamos");
        SectorBiblioteca devoluciones = new SectorBiblioteca("Devoluciones");
        SectorBiblioteca atencion = new SectorBiblioteca("Atención a socios");

        
        prestamos.registrar("Préstamo del libro 'El Aleph' al socio 101");
        atencion.registrar("Consulta de horarios del socio 205");
        devoluciones.registrar("Devolución de 'Rayuela' del socio 150");
        prestamos.registrar("Préstamo de 'Ficciones' al socio 150");
        atencion.registrar("Alta de nuevo socio 310");
        devoluciones.registrar("Devolución de 'El Aleph' del socio 101");

        
        boolean mismo = prestamos.getRegistro() == devoluciones.getRegistro()
                && devoluciones.getRegistro() == atencion.getRegistro();
        System.out.println("¿Los tres sectores usan el mismo registro? " + mismo);

        System.out.println("\nHistorial completo (orden de incorporación):");
        RegistroActividades registro = RegistroActividades.getInstancia();
        registro.historial().forEach(a -> System.out.println("  " + a));

        System.out.println("\nSolo 'Devoluciones':");
        registro.filtrarPorSector("Devoluciones").forEach(a -> System.out.println("  " + a));

        System.out.println("\nTotal de eventos registrados: " + registro.total());

        
        try {
            registro.historial().add(new Actividad(LocalDateTime.now(), "Intruso", "Evento falso"));
        } catch (UnsupportedOperationException e) {
            System.out.println("\nModificar el historial desde afuera -> bloqueado (UnsupportedOperationException)");
        }
        try {
            registro.filtrarPorSector("Préstamos").clear();
        } catch (UnsupportedOperationException e) {
            System.out.println("Modificar un filtro desde afuera -> bloqueado (UnsupportedOperationException)");
        }
        System.out.println("Total tras los intentos: " + registro.total());
    }
}
