
final class ConfiguracionBiblioteca {

    
    private static final class Holder {
        static final ConfiguracionBiblioteca INSTANCIA = new ConfiguracionBiblioteca();
    }

    
    private String nombre = "Biblioteca Municipal";
    private int maxLibrosPorSocio = 3;
    private int plazoDevolucionDias = 7;

    private ConfiguracionBiblioteca() { }                

    public static ConfiguracionBiblioteca getInstancia() {
        return Holder.INSTANCIA;                         
    }

    public synchronized String getNombre() { return nombre; }

    public synchronized int getMaxLibrosPorSocio() { return maxLibrosPorSocio; }

    public synchronized int getPlazoDevolucionDias() { return plazoDevolucionDias; }

    public synchronized void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la biblioteca no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public synchronized void setMaxLibrosPorSocio(int maxLibrosPorSocio) {
        if (maxLibrosPorSocio <= 0) {
            throw new IllegalArgumentException("El máximo de libros debe ser mayor a cero");
        }
        this.maxLibrosPorSocio = maxLibrosPorSocio;
    }

    public synchronized void setPlazoDevolucionDias(int plazoDevolucionDias) {
        if (plazoDevolucionDias <= 0) {
            throw new IllegalArgumentException("El plazo de devolución debe ser mayor a cero");
        }
        this.plazoDevolucionDias = plazoDevolucionDias;
    }
}


final class ModuloPrestamos {
    private final ConfiguracionBiblioteca config = ConfiguracionBiblioteca.getInstancia();

    public ConfiguracionBiblioteca getConfig() { return config; }

    public boolean puedePrestar(int librosActualesDelSocio) {
        return librosActualesDelSocio < config.getMaxLibrosPorSocio();
    }

    public void mostrar() {
        System.out.println("  [Préstamos] " + config.getNombre()
                + " | máx. libros: " + config.getMaxLibrosPorSocio()
                + " | plazo: " + config.getPlazoDevolucionDias() + " días");
    }
}


final class ModuloConsultas {
    private final ConfiguracionBiblioteca config = ConfiguracionBiblioteca.getInstancia();

    public ConfiguracionBiblioteca getConfig() { return config; }

    public void mostrar() {
        System.out.println("  [Consultas] Bienvenido a " + config.getNombre()
                + ". Podés llevar hasta " + config.getMaxLibrosPorSocio()
                + " libros por " + config.getPlazoDevolucionDias() + " días.");
    }
}

public class Biblioteca01 {
    public static void main(String[] args) {
        ModuloPrestamos prestamos = new ModuloPrestamos();
        ModuloConsultas consultas = new ModuloConsultas();

        System.out.println("Estado inicial:");
        prestamos.mostrar();
        consultas.mostrar();

        
        System.out.println("\n¿Ambos módulos usan el mismo objeto? "
                + (prestamos.getConfig() == consultas.getConfig()));

        
        System.out.println("\nModificando la configuración desde el módulo de préstamos...");
        prestamos.getConfig().setNombre("Biblioteca Central");
        prestamos.getConfig().setMaxLibrosPorSocio(5);
        prestamos.getConfig().setPlazoDevolucionDias(14);
        prestamos.mostrar();
        consultas.mostrar();

        
        System.out.println("\nValidaciones:");
        intentar("nombre vacío", () -> consultas.getConfig().setNombre(""));
        intentar("nombre solo espacios", () -> consultas.getConfig().setNombre("   "));
        intentar("máximo de libros = 0", () -> consultas.getConfig().setMaxLibrosPorSocio(0));
        intentar("plazo = -5", () -> consultas.getConfig().setPlazoDevolucionDias(-5));

        System.out.println("\nTras los intentos inválidos, la configuración sigue igual:");
        prestamos.mostrar();

        
    }

    private static void intentar(String descripcion, Runnable accion) {
        try {
            accion.run();
            System.out.println("  " + descripcion + " -> aceptado (inesperado)");
        } catch (IllegalArgumentException e) {
            System.out.println("  " + descripcion + " -> rechazado: " + e.getMessage());
        }
    }
}
