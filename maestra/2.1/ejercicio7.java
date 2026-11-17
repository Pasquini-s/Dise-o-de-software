import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

record Documento(String nombre, String autor) { }

final class ColaImpresion {

    private static final class Holder {
        static final ColaImpresion INSTANCIA = new ColaImpresion();
    }

    private final BlockingQueue<Documento> cola = new LinkedBlockingQueue<>();

    private ColaImpresion() { }

    public static ColaImpresion getInstancia() { return Holder.INSTANCIA; }

    public void encolar(String nombre, String autor) {
        cola.add(new Documento(nombre, autor));
    }

   
    public Documento procesarSiguiente() {
        Documento doc = cola.poll();
        if (doc != null) {
            System.out.println("  Imprimiendo '" + doc.nombre() + "' de " + doc.autor());
        }
        return doc;
    }

    public int procesarTodos() {
        int n = 0;
        while (procesarSiguiente() != null) n++;
        return n;
    }

    public int pendientes() { return cola.size(); }
}

public class Ejercicio07 {
    public static void main(String[] args) throws InterruptedException {
        ColaImpresion cola = ColaImpresion.getInstancia();

        
        ExecutorService terminales = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            terminales.submit(() -> cola.encolar("informe" + n + ".pdf", "usuario" + n));
        }
        terminales.shutdown();
        terminales.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Pendientes: " + cola.pendientes() + " (ninguno se perdió ni se duplicó)");

        System.out.println("Procesando la cola:");
        int impresos = cola.procesarTodos();

        System.out.println("Impresos: " + impresos + " | Pendientes: " + cola.pendientes());
    }
}
