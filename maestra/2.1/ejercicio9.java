import java.util.ArrayDeque;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


final class ColectorMetricas {

    private static final class Holder {
        static final ColectorMetricas INSTANCIA = new ColectorMetricas();
    }

    public static final int UMBRAL_LATENCIAS = 1000;

    private final ConcurrentHashMap<String, LongAdder> contadores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VentanaLatencias> latencias = new ConcurrentHashMap<>();

    private ColectorMetricas() { }

    public static ColectorMetricas getInstancia() { return Holder.INSTANCIA; }

    
    private static final class VentanaLatencias {
        private final ArrayDeque<Long> muestras = new ArrayDeque<>(UMBRAL_LATENCIAS);

        synchronized void agregar(long ms) {             
            if (muestras.size() == UMBRAL_LATENCIAS) muestras.pollFirst();
            muestras.addLast(ms);
        }

        synchronized LongSummaryStatistics resumen() {
            return muestras.stream().mapToLong(Long::longValue).summaryStatistics();
        }
    }

    public void incrementar(String nombre) { incrementar(nombre, 1); }

    public void incrementar(String nombre, long cantidad) {
        contadores.computeIfAbsent(nombre, k -> new LongAdder()).add(cantidad);
    }

    public void registrarLatencia(String nombre, long ms) {
        latencias.computeIfAbsent(nombre, k -> new VentanaLatencias()).agregar(ms);
    }

    public record ResumenLatencia(long muestras, double promedioMs, long minMs, long maxMs) { }

    public record Resumen(Map<String, Long> contadores, Map<String, ResumenLatencia> latencias) { }

    
    public Resumen exportar() {
        Map<String, Long> c = new TreeMap<>();
        contadores.forEach((k, v) -> c.put(k, v.sum()));

        Map<String, ResumenLatencia> l = new TreeMap<>();
        latencias.forEach((k, v) -> {
            LongSummaryStatistics s = v.resumen();
            if (s.getCount() > 0) {
                l.put(k, new ResumenLatencia(s.getCount(),
                        Math.round(s.getAverage() * 100.0) / 100.0, s.getMin(), s.getMax()));
            }
        });
        return new Resumen(c, l);
    }

    public void reiniciar() {
        contadores.clear();
        latencias.clear();
    }
}

public class Ejercicio09 {
    public static void main(String[] args) throws InterruptedException {
        ColectorMetricas metricas = ColectorMetricas.getInstancia();

        
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int h = 0; h < 8; h++) {
            pool.submit(() -> {
                for (int i = 0; i < 1000; i++) {
                    metricas.incrementar("requests");
                    metricas.registrarLatencia("api", i % 50);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        ColectorMetricas.Resumen resumen = metricas.exportar();
        System.out.println("requests (esperado 8000): " + resumen.contadores().get("requests"));
        System.out.println("latencias de 'api' (ventana máxima " + ColectorMetricas.UMBRAL_LATENCIAS
                + "): " + resumen.latencias().get("api"));

        metricas.reiniciar();
        System.out.println("tras reiniciar: " + metricas.exportar());
    }
}
