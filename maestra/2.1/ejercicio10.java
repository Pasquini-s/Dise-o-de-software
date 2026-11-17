import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

record EventoAuditoria(long secuencia, Instant marcaTiempo, String idSesion,
                       String operador, String accion, String detalle,
                       String hashPrevio, String hashActual) { }

enum AuditorSeguridad {
    INSTANCIA;

    private static final String HASH_INICIAL = "0".repeat(64);

    private final List<EventoAuditoria> eventos = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public EventoAuditoria registrar(String idSesion, String operador, String accion, String detalle) {
        lock.lock();
        try {
         
            long sec = eventos.size() + 1;
            Instant ahora = Instant.now();
            String previo = eventos.isEmpty() ? HASH_INICIAL
                    : eventos.get(eventos.size() - 1).hashActual();
            String hash = calcularHash(sec, ahora, idSesion, operador, accion, detalle, previo);

            EventoAuditoria ev = new EventoAuditoria(sec, ahora, idSesion, operador,
                    accion, detalle, previo, hash);
            eventos.add(ev);
            return ev;
        } finally {
            lock.unlock();
        }
    }

    public int total() {
        lock.lock();
        try { return eventos.size(); } finally { lock.unlock(); }
    }

    
    public List<EventoAuditoria> traza() {
        lock.lock();
        try { return List.copyOf(eventos); } finally { lock.unlock(); }
    }

    
    public boolean verificarIntegridad() {
        lock.lock();
        try {
            String previo = HASH_INICIAL;
            for (EventoAuditoria e : eventos) {
                String esperado = calcularHash(e.secuencia(), e.marcaTiempo(), e.idSesion(),
                        e.operador(), e.accion(), e.detalle(), previo);
                if (!e.hashPrevio().equals(previo) || !e.hashActual().equals(esperado)) return false;
                previo = e.hashActual();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private static String calcularHash(long sec, Instant t, String sesion, String operador,
                                       String accion, String detalle, String previo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String datos = sec + "|" + t + "|" + sesion + "|" + operador + "|"
                    + accion + "|" + detalle + "|" + previo;
            return HexFormat.of().formatHex(md.digest(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

public class Ejercicio10 {
    public static void main(String[] args) throws InterruptedException {
        AuditorSeguridad auditor = AuditorSeguridad.INSTANCIA;
        String sesion = UUID.randomUUID().toString().substring(0, 8);

        
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 20; i++) {
            final int n = i;
            pool.submit(() -> auditor.registrar(sesion, "operador" + n,
                    "TRANSFERENCIA", "monto=" + (n * 100)));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Total de registros: " + auditor.total());
        System.out.println("Integridad de la cadena: " + auditor.verificarIntegridad());

        System.out.println("Primeros 3 eventos de la traza:");
        auditor.traza().stream().limit(3).forEach(e ->
                System.out.println("  #" + e.secuencia() + " " + e.marcaTiempo()
                        + " " + e.operador() + " " + e.accion()
                        + " hash=" + e.hashActual().substring(0, 12) + "..."));
       
        try {
            auditor.traza().add(null);
        } catch (UnsupportedOperationException e) {
            System.out.println("Traza inmutable -> UnsupportedOperationException");
        }

    
        try {
            Constructor<AuditorSeguridad> c =
                    AuditorSeguridad.class.getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            c.newInstance("OTRA", 1);
        } catch (Exception e) {
            System.out.println("Reflexión bloqueada -> " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
    }
}
