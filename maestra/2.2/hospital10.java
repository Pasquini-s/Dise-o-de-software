import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


record ClaveTurno(String profesional, LocalDate fecha, LocalTime hora) { }


record ItemAgenda(LocalTime hora, String paciente) {
    @Override
    public String toString() {
        return hora + " -> " + (paciente == null ? "LIBRE" : paciente);
    }
}

enum ResultadoTurno { RESERVADO, OCUPADO, HORARIO_INEXISTENTE }


final class AdministradorTurnos {

    private static volatile AdministradorTurnos instancia;

    private final Set<ClaveTurno> disponibles = new HashSet<>();
    private final Map<ClaveTurno, String> reservados = new HashMap<>();   

    private AdministradorTurnos() { }

    public static AdministradorTurnos getInstancia() {
        AdministradorTurnos local = instancia;
        if (local == null) {
            synchronized (AdministradorTurnos.class) {
                local = instancia;
                if (local == null) {
                    instancia = local = new AdministradorTurnos();
                }
            }
        }
        return local;
    }

    public synchronized void cargarAgenda(String profesional, LocalDate fecha, List<LocalTime> horarios) {
        for (LocalTime hora : horarios) {
            ClaveTurno clave = new ClaveTurno(profesional, fecha, hora);
            if (!reservados.containsKey(clave)) disponibles.add(clave);
        }
    }

    public synchronized ResultadoTurno reservar(String paciente, String profesional,
                                                LocalDate fecha, LocalTime hora) {
        if (paciente == null || paciente.isBlank()) {
            throw new IllegalArgumentException("El paciente no puede estar vacío");
        }
        ClaveTurno clave = new ClaveTurno(profesional, fecha, hora);
        if (reservados.containsKey(clave)) return ResultadoTurno.OCUPADO;
        if (!disponibles.remove(clave)) return ResultadoTurno.HORARIO_INEXISTENTE;
        reservados.put(clave, paciente);                              
        return ResultadoTurno.RESERVADO;
    }

    
    public synchronized boolean cancelar(String profesional, LocalDate fecha, LocalTime hora) {
        ClaveTurno clave = new ClaveTurno(profesional, fecha, hora);
        String paciente = reservados.remove(clave);
        if (paciente == null) return false;
        disponibles.add(clave);
        return true;
    }

    
    public synchronized List<ItemAgenda> consultarAgenda(String profesional, LocalDate fecha) {
        Map<LocalTime, String> ordenada = new TreeMap<>();
        for (ClaveTurno c : disponibles) {
            if (c.profesional().equals(profesional) && c.fecha().equals(fecha)) ordenada.put(c.hora(), null);
        }
        reservados.forEach((c, paciente) -> {
            if (c.profesional().equals(profesional) && c.fecha().equals(fecha)) ordenada.put(c.hora(), paciente);
        });
        List<ItemAgenda> resultado = new ArrayList<>();
        ordenada.forEach((hora, paciente) -> resultado.add(new ItemAgenda(hora, paciente)));
        return List.copyOf(resultado);
    }
}


final class PuestoRecepcion {
    private final AdministradorTurnos admin = AdministradorTurnos.getInstancia();

    public AdministradorTurnos getAdmin() { return admin; }

    public ResultadoTurno reservar(String paciente, String prof, LocalDate fecha, LocalTime hora) {
        return admin.reservar(paciente, prof, fecha, hora);
    }

    public List<ItemAgenda> verAgenda(String prof, LocalDate fecha) { return admin.consultarAgenda(prof, fecha); }
}


final class PuestoAutoservicio {
    private final AdministradorTurnos admin = AdministradorTurnos.getInstancia();

    public AdministradorTurnos getAdmin() { return admin; }

    public ResultadoTurno reservar(String paciente, String prof, LocalDate fecha, LocalTime hora) {
        return admin.reservar(paciente, prof, fecha, hora);
    }

    public List<ItemAgenda> verAgenda(String prof, LocalDate fecha) { return admin.consultarAgenda(prof, fecha); }
}

public class Hospital10 {

    static void mostrar(String titulo, List<ItemAgenda> agenda) {
        System.out.println(titulo);
        agenda.forEach(i -> System.out.println("    " + i));
    }

    public static void main(String[] args) throws Exception {
        final String PROF = "Dra. Gómez";
        final LocalDate FECHA = LocalDate.of(2026, 12, 1);
        final LocalTime NUEVE = LocalTime.of(9, 0);

        PuestoRecepcion recepcion = new PuestoRecepcion();
        PuestoAutoservicio autoservicio = new PuestoAutoservicio();
        System.out.println("¿Ambos componentes usan el mismo administrador? "
                + (recepcion.getAdmin() == autoservicio.getAdmin()));

        recepcion.getAdmin().cargarAgenda(PROF, FECHA,
                List.of(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)));
        mostrar("\nAgenda inicial (vista desde autoservicio):", autoservicio.verAgenda(PROF, FECHA));

        
        final int HILOS = 20;
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch largada = new CountDownLatch(1);
        Map<ResultadoTurno, AtomicInteger> conteo = new EnumMap<>(ResultadoTurno.class);
        for (ResultadoTurno r : ResultadoTurno.values()) conteo.put(r, new AtomicInteger());

        List<Future<Object>> futuros = new ArrayList<>();
        for (int i = 1; i <= HILOS; i++) {
            final String paciente = String.format("Paciente %02d", i);
            final boolean porRecepcion = (i % 2 == 0);
            Callable<Object> tarea = () -> {
                largada.await();
                ResultadoTurno r = porRecepcion
                        ? recepcion.reservar(paciente, PROF, FECHA, NUEVE)
                        : autoservicio.reservar(paciente, PROF, FECHA, NUEVE);
                conteo.get(r).incrementAndGet();
                return null;
            };
            futuros.add(pool.submit(tarea));
        }
        largada.countDown();
        for (Future<Object> f : futuros) f.get();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n" + HILOS + " hilos intentaron reservar el turno de las 09:00:");
        System.out.println("  RESERVADO: " + conteo.get(ResultadoTurno.RESERVADO) + " (esperado: 1)");
        System.out.println("  OCUPADO  : " + conteo.get(ResultadoTurno.OCUPADO) + " (esperado: 19)");
        mostrar("\nAgenda actualizada (vista desde recepción):", recepcion.verAgenda(PROF, FECHA));

        
        List<ItemAgenda> antes = recepcion.verAgenda(PROF, FECHA);
        boolean c1 = recepcion.getAdmin().cancelar(PROF, FECHA, LocalTime.of(11, 0));   
        boolean c2 = recepcion.getAdmin().cancelar(PROF, FECHA, LocalTime.of(10, 0));   
        boolean c3 = recepcion.getAdmin().cancelar("Dr. Otro", FECHA, NUEVE);            
        List<ItemAgenda> despues = recepcion.verAgenda(PROF, FECHA);
        System.out.println("\nCancelar turnos inexistentes -> " + c1 + ", " + c2 + ", " + c3);
        System.out.println("¿La agenda quedó sin cambios? " + antes.equals(despues));

        
        System.out.println("\nCancelar el turno de las 09:00 -> " + autoservicio.getAdmin().cancelar(PROF, FECHA, NUEVE));
        mostrar("Agenda tras cancelar:", autoservicio.verAgenda(PROF, FECHA));
        System.out.println("  Paciente 99 reserva las 09:00 -> " + recepcion.reservar("Paciente 99", PROF, FECHA, NUEVE));
        mostrar("Agenda final:", autoservicio.verAgenda(PROF, FECHA));
        System.out.println("  Reservar un horario que no existe (12:00) -> "
                + recepcion.reservar("Paciente 98", PROF, FECHA, LocalTime.of(12, 0)));
    }
}
