import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

enum Tema { CLARO, OSCURO }

final class GestorTemas {

    private static final class Holder {
        static final GestorTemas INSTANCIA = new GestorTemas();
    }

    private Tema temaActual = Tema.CLARO;
    private final Map<Tema, Map<String, String>> paletas = new EnumMap<>(Tema.class);
    private final List<BiConsumer<Tema, Map<String, String>>> observadores =
            new CopyOnWriteArrayList<>();

    private GestorTemas() {
        paletas.put(Tema.CLARO,  new HashMap<>(Map.of("fondo", "#FFFFFF", "texto", "#000000")));
        paletas.put(Tema.OSCURO, new HashMap<>(Map.of("fondo", "#121212", "texto", "#EEEEEE")));
    }

    public static GestorTemas getInstancia() { return Holder.INSTANCIA; }

    public void suscribir(BiConsumer<Tema, Map<String, String>> observador) {
        observadores.add(observador);
    }

    public synchronized Tema getTema() { return temaActual; }

    
    public synchronized Map<String, String> getColores() {
        return Map.copyOf(paletas.get(temaActual));
    }

    public synchronized void cambiarTema(Tema nuevo) {
        if (nuevo != temaActual) {
            temaActual = nuevo;
            notificar();
        }
    }

    public synchronized void alternar() {
        cambiarTema(temaActual == Tema.CLARO ? Tema.OSCURO : Tema.CLARO);
    }

    
    public synchronized void establecerColor(Tema tema, String elemento, String valor) {
        paletas.get(tema).put(elemento, valor);
        if (tema == temaActual) notificar();
    }

    private void notificar() {
        Map<String, String> colores = Map.copyOf(paletas.get(temaActual));
        observadores.forEach(o -> o.accept(temaActual, colores));
    }
}

public class Ejercicio06 {
    public static void main(String[] args) {
        GestorTemas temas = GestorTemas.getInstancia();

        
        temas.suscribir((t, col) -> System.out.println("  Vista Inicio    -> " + t + " " + col));
        temas.suscribir((t, col) -> System.out.println("  Vista Ajustes   -> " + t));

        System.out.println("Tema inicial: " + temas.getTema());

        System.out.println("Alternando...");
        temas.alternar();

        System.out.println("Consultado desde otra referencia: " + GestorTemas.getInstancia().getTema());

        System.out.println("Cambiando un color del tema oscuro...");
        temas.establecerColor(Tema.OSCURO, "fondo", "#000000");

        
        try {
            temas.getColores().put("fondo", "#FF0000");
        } catch (UnsupportedOperationException e) {
            System.out.println("Modificación externa bloqueada -> UnsupportedOperationException");
        }
    }
}
