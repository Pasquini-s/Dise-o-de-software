import java.time.LocalTime;


final class ConfiguracionHospital {

    private static volatile ConfiguracionHospital instancia;      
    private static int instanciasCreadas = 0;                     

    private String nombre = "Hospital Central";
    private LocalTime apertura = LocalTime.of(8, 0);
    private LocalTime cierre = LocalTime.of(18, 0);
    private int duracionTurnoMinutos = 30;

    private ConfiguracionHospital() {
        instanciasCreadas++;
        System.out.println("  >>> Construyendo ConfiguracionHospital (instancias creadas: "
                + instanciasCreadas + ")");
    }

    public static ConfiguracionHospital getInstancia() {
        ConfiguracionHospital local = instancia;
        if (local == null) {                                      
            synchronized (ConfiguracionHospital.class) {
                local = instancia;
                if (local == null) {                              
                    instancia = local = new ConfiguracionHospital();
                }
            }
        }
        return local;
    }

    public static boolean estaCreada() { return instancia != null; }

    public static int getInstanciasCreadas() { return instanciasCreadas; }

    public synchronized String getNombre() { return nombre; }

    public synchronized LocalTime getApertura() { return apertura; }

    public synchronized LocalTime getCierre() { return cierre; }

    public synchronized int getDuracionTurnoMinutos() { return duracionTurnoMinutos; }

    public synchronized void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del hospital no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    
    public synchronized void setHorario(LocalTime apertura, LocalTime cierre) {
        if (apertura == null || cierre == null) {
            throw new IllegalArgumentException("Apertura y cierre son obligatorios");
        }
        if (!cierre.isAfter(apertura)) {
            throw new IllegalArgumentException("El horario de cierre debe ser posterior al de apertura");
        }
        this.apertura = apertura;
        this.cierre = cierre;
    }

    public synchronized void setDuracionTurnoMinutos(int minutos) {
        if (minutos <= 0) {
            throw new IllegalArgumentException("La duración del turno debe ser positiva");
        }
        this.duracionTurnoMinutos = minutos;
    }
}

final class ModuloRecepcion {
    private final ConfiguracionHospital config = ConfiguracionHospital.getInstancia();

    public ConfiguracionHospital getConfig() { return config; }

    public void mostrar() {
        System.out.println("  [Recepción]    " + config.getNombre() + " | atención de "
                + config.getApertura() + " a " + config.getCierre()
                + " | turnos de " + config.getDuracionTurnoMinutos() + " min");
    }
}

final class ModuloConsultorios {
    private final ConfiguracionHospital config = ConfiguracionHospital.getInstancia();

    public ConfiguracionHospital getConfig() { return config; }

    public void mostrar() {
        System.out.println("  [Consultorios] " + config.getNombre() + " | atención de "
                + config.getApertura() + " a " + config.getCierre()
                + " | turnos de " + config.getDuracionTurnoMinutos() + " min");
    }
}

public class Hospital06 {
    public static void main(String[] args) {
        System.out.println("Antes de la primera consulta, ¿existe la instancia? "
                + ConfiguracionHospital.estaCreada());

        System.out.println("\nSe crea el módulo de Recepción (primera consulta):");
        ModuloRecepcion recepcion = new ModuloRecepcion();

        System.out.println("\nSe crea el módulo de Consultorios (NO debe construir otra instancia):");
        ModuloConsultorios consultorios = new ModuloConsultorios();

        System.out.println("\nCinco consultas más:");
        for (int i = 0; i < 5; i++) ConfiguracionHospital.getInstancia();
        System.out.println("  Instancias creadas en total: " + ConfiguracionHospital.getInstanciasCreadas()
                + " (esperado: 1)");
        System.out.println("  ¿Mismo objeto en ambos módulos? "
                + (recepcion.getConfig() == consultorios.getConfig()));

        System.out.println("\nEstado inicial:");
        recepcion.mostrar();
        consultorios.mostrar();

        System.out.println("\nConsultorios modifica la configuración; Recepción lo ve de inmediato:");
        consultorios.getConfig().setNombre("Hospital San Martín");
        consultorios.getConfig().setHorario(LocalTime.of(7, 30), LocalTime.of(20, 0));
        consultorios.getConfig().setDuracionTurnoMinutos(20);
        recepcion.mostrar();
        consultorios.mostrar();

        System.out.println("\nValidaciones:");
        intentar("duración = 0", () -> recepcion.getConfig().setDuracionTurnoMinutos(0));
        intentar("duración negativa", () -> recepcion.getConfig().setDuracionTurnoMinutos(-15));
        intentar("cierre igual a apertura",
                () -> recepcion.getConfig().setHorario(LocalTime.of(9, 0), LocalTime.of(9, 0)));
        intentar("cierre anterior a apertura",
                () -> recepcion.getConfig().setHorario(LocalTime.of(18, 0), LocalTime.of(8, 0)));

        System.out.println("\nLa configuración sigue intacta:");
        recepcion.mostrar();
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
