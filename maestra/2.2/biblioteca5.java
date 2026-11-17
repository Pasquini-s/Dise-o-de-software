import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

enum ResultadoReserva { CONFIRMADA, DUPLICADA, SIN_LUGARES }


final class GestorReservasSala {

    public static final int CAPACIDAD = 10;

    private static volatile GestorReservasSala instancia;

    private final Set<String> reservas = new LinkedHashSet<>();   

    private GestorReservasSala() { }

    public static GestorReservasSala getInstancia() {
        GestorReservasSala local = instancia;
        if (local == null) {
            synchronized (GestorReservasSala.class) {
                local = instancia;
                if (local == null) {
                    instancia = local = new GestorReservasSala();
                }
            }
        }
        return local;
    }

    public synchronized ResultadoReserva reservar(String socioId) {
        if (socioId == null || socioId.isBlank()) {
            throw new IllegalArgumentException("El identificador de socio no puede estar vacío");
        }
        if (reservas.contains(socioId)) return ResultadoReserva.DUPLICADA;
        if (reservas.size() >= CAPACIDAD) return ResultadoReserva.SIN_LUGARES;
        reservas.add(socioId);                                    
        return ResultadoReserva.CONFIRMADA;
    }

    
    public synchronized boolean cancelar(String socioId) {
        return reservas.remove(socioId);
    }

    public synchronized int lugaresDisponibles() { return CAPACIDAD - reservas.size(); }

    public synchronized List<String> reservasActivas() { return List.copyOf(reservas); }
}

public class Biblioteca05 {
    public static void main(String[] args) throws Exception {
        final int SOLICITUDES = 30;
        ExecutorService pool = Executors.newFixedThreadPool(SOLICITUDES);
        CountDownLatch largada = new CountDownLatch(1);           

        Set<GestorReservasSala> instanciasVistas = ConcurrentHashMap.newKeySet();
        Map<ResultadoReserva, AtomicInteger> resultados = new EnumMap<>(ResultadoReserva.class);
        for (ResultadoReserva r : ResultadoReserva.values()) resultados.put(r, new AtomicInteger());

        List<Future<Object>> futuros = new ArrayList<>();
        for (int i = 1; i <= SOLICITUDES; i++) {
            final String socio = String.format("SOCIO-%02d", i);
            Callable<Object> tarea = () -> {
                largada.await();
                GestorReservasSala gestor = GestorReservasSala.getInstancia();
                instanciasVistas.add(gestor);
                resultados.get(gestor.reservar(socio)).incrementAndGet();
                return null;
            };
            futuros.add(pool.submit(tarea));
        }
        largada.countDown();                                      
        for (Future<Object> f : futuros) f.get();                 
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        GestorReservasSala gestor = GestorReservasSala.getInstancia();
        System.out.println("Solicitudes concurrentes: " + SOLICITUDES);
        System.out.println("Instancias distintas observadas por los hilos: " + instanciasVistas.size()
                + " (esperado: 1)");
        System.out.println("Confirmadas : " + resultados.get(ResultadoReserva.CONFIRMADA) + " (esperado: 10)");
        System.out.println("Sin lugares : " + resultados.get(ResultadoReserva.SIN_LUGARES) + " (esperado: 20)");
        System.out.println("Reservas activas: " + gestor.reservasActivas().size()
                + " | Lugares disponibles: " + gestor.lugaresDisponibles());
        boolean invariantes = gestor.reservasActivas().size() <= GestorReservasSala.CAPACIDAD
                && gestor.lugaresDisponibles() >= 0;
        System.out.println("Invariantes (activas <= 10 y disponibles >= 0): " + invariantes);

        System.out.println("\nReserva duplicada:");
        String yaReservado = gestor.reservasActivas().get(0);
        System.out.println("  " + yaReservado + " reserva de nuevo -> " + gestor.reservar(yaReservado));

        System.out.println("\nCancelar una reserva inexistente:");
        int antes = gestor.lugaresDisponibles();
        boolean cancelada = gestor.cancelar("SOCIO-NO-EXISTE");
        System.out.println("  cancelada=" + cancelada + " | disponibles antes: " + antes
                + " | después: " + gestor.lugaresDisponibles());

        System.out.println("\nCancelar una reserva real y recuperar el lugar:");
        System.out.println("  cancelar " + yaReservado + " -> " + gestor.cancelar(yaReservado)
                + " | disponibles: " + gestor.lugaresDisponibles());
        System.out.println("  SOCIO-NUEVO reserva -> " + gestor.reservar("SOCIO-NUEVO")
                + " | disponibles: " + gestor.lugaresDisponibles());
    }
}
