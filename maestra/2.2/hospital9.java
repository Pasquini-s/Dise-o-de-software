import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.TreeMap;

record TrabajoImpresion(long id, String nombre, String sector, int paginas) {
    @Override
    public String toString() {
        return "#" + id + " | " + nombre + " | " + sector + " | " + paginas + " págs.";
    }
}


final class GestorImpresion {

    private static volatile GestorImpresion instancia;

    private final Queue<TrabajoImpresion> cola = new ArrayDeque<>();
    private final List<TrabajoImpresion> procesados = new ArrayList<>();
    private long proximoId = 1;

    private GestorImpresion() { }

    public static GestorImpresion getInstancia() {
        GestorImpresion local = instancia;
        if (local == null) {
            synchronized (GestorImpresion.class) {
                local = instancia;
                if (local == null) {
                    instancia = local = new GestorImpresion();
                }
            }
        }
        return local;
    }

    
    public synchronized TrabajoImpresion encolar(String nombre, String sector, int paginas) {
        if (nombre == null || nombre.isBlank() || sector == null || sector.isBlank()) {
            throw new IllegalArgumentException("Nombre y sector son obligatorios");
        }
        if (paginas <= 0) {
            throw new IllegalArgumentException("La cantidad de páginas debe ser positiva");
        }
        TrabajoImpresion t = new TrabajoImpresion(proximoId++, nombre, sector, paginas);
        cola.add(t);
        return t;
    }

    
    public synchronized Optional<TrabajoImpresion> procesarSiguiente() {
        TrabajoImpresion t = cola.poll();
        if (t == null) return Optional.empty();
        System.out.println("  Imprimiendo " + t);                  
        procesados.add(t);
        return Optional.of(t);
    }

    public synchronized int pendientes() { return cola.size(); }

    public synchronized List<TrabajoImpresion> informe() { return List.copyOf(procesados); }
}

public class Hospital09 {
    public static void main(String[] args) throws Exception {
        final int HILOS = 10;
        final int DOCS_POR_HILO = 3;                               
        final String[] sectores = {"Recepción", "Facturación", "Archivo"};

        ExecutorService emisores = Executors.newFixedThreadPool(HILOS);
        CountDownLatch largada = new CountDownLatch(1);
        Set<GestorImpresion> instanciasVistas = ConcurrentHashMap.newKeySet();

        List<Future<Object>> futuros = new ArrayList<>();
        for (int h = 0; h < HILOS; h++) {
            final int hilo = h;
            Callable<Object> tarea = () -> {
                largada.await();
                GestorImpresion gestor = GestorImpresion.getInstancia();
                instanciasVistas.add(gestor);
                for (int d = 0; d < DOCS_POR_HILO; d++) {
                    String sector = sectores[(hilo + d) % sectores.length];
                    gestor.encolar("documento-h" + hilo + "-" + d + ".pdf", sector, 1 + (hilo + d) % 5);
                }
                return null;
            };
            futuros.add(emisores.submit(tarea));
        }
        largada.countDown();
        for (Future<Object> f : futuros) f.get();
        emisores.shutdown();
        emisores.awaitTermination(10, TimeUnit.SECONDS);

        GestorImpresion gestor = GestorImpresion.getInstancia();
        System.out.println("Hilos emisores: " + HILOS + " | documentos enviados: " + HILOS * DOCS_POR_HILO);
        System.out.println("Instancias distintas observadas: " + instanciasVistas.size() + " (esperado: 1)");
        System.out.println("Documentos pendientes en la cola: " + gestor.pendientes());

        
        System.out.println("\nProcesando con 4 hilos en paralelo:");
        ExecutorService impresoras = Executors.newFixedThreadPool(4);
        List<Future<Object>> trabajos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Callable<Object> t = () -> {
                while (GestorImpresion.getInstancia().procesarSiguiente().isPresent()) {  }
                return null;
            };
            trabajos.add(impresoras.submit(t));
        }
        for (Future<Object> f : trabajos) f.get();
        impresoras.shutdown();
        impresoras.awaitTermination(10, TimeUnit.SECONDS);

        
        List<TrabajoImpresion> informe = gestor.informe();
        System.out.println("\n--- Informe de trabajos procesados ---");
        informe.forEach(t -> System.out.println("  " + t));

        System.out.println("\nPáginas por sector:");
        informe.stream()
                .collect(Collectors.groupingBy(TrabajoImpresion::sector, TreeMap::new,
                        Collectors.summingInt(TrabajoImpresion::paginas)))
                .forEach((sector, pags) -> System.out.println("  " + sector + ": " + pags));

        long distintos = informe.stream().map(TrabajoImpresion::id).distinct().count();
        boolean enOrden = true;
        for (int i = 0; i < informe.size(); i++) {
            if (informe.get(i).id() != i + 1) enOrden = false;
        }
        System.out.println("\nProcesados: " + informe.size() + " | ids distintos: " + distintos
                + " | pendientes: " + gestor.pendientes());
        System.out.println("¿Cada documento procesado exactamente una vez? "
                + (informe.size() == 30 && distintos == 30));
        System.out.println("¿Procesados en orden de incorporación? " + enOrden);
    }
}
