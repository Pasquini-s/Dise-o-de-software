import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

record Perfil(String nombre, String email) { }

final class CacheMemoria {

    private static final class Holder {
        static final CacheMemoria INSTANCIA = new CacheMemoria();
    }

    private volatile Map<String, Object> datos;          

    private CacheMemoria() { }

    public static CacheMemoria getInstancia() { return Holder.INSTANCIA; }

    private Map<String, Object> almacen() {
        Map<String, Object> local = datos;
        if (local == null) {
            synchronized (this) {
                local = datos;
                if (local == null) {
                    System.out.println("  [Caché] Inicializando almacén (primer uso)");
                    datos = local = new ConcurrentHashMap<>();
                }
            }
        }
        return local;
    }

    public void guardar(String clave, Object valor) {
        almacen().put(Objects.requireNonNull(clave), Objects.requireNonNull(valor));
    }

    public boolean existe(String clave) {
        return almacen().containsKey(clave);
    }

    public <T> Optional<T> obtener(String clave, Class<T> tipo) {
        return Optional.ofNullable(almacen().get(clave)).map(tipo::cast);
    }


    public void actualizar(String clave, Object valor) {
        if (almacen().replace(clave, valor) == null) {
            throw new NoSuchElementException("La clave '" + clave + "' no existe en la caché");
        }
    }

    public boolean eliminar(String clave) {
        return almacen().remove(clave) != null;
    }

    public int tamanio() { return almacen().size(); }
}

public class Ejercicio04 {
    public static void main(String[] args) {
        System.out.println("Obteniendo la instancia (todavía sin almacén)...");
        CacheMemoria cache = CacheMemoria.getInstancia();

        cache.guardar("u1", new Perfil("Ana", "ana@mail.com"));
        cache.guardar("u2", new Perfil("Luis", "luis@mail.com"));

        System.out.println("u1: " + CacheMemoria.getInstancia().obtener("u1", Perfil.class).orElseThrow());
        System.out.println("existe u2: " + cache.existe("u2"));
        System.out.println("existe u9: " + cache.existe("u9"));

        cache.actualizar("u1", new Perfil("Ana María", "ana@mail.com"));
        System.out.println("u1 actualizado: " + cache.obtener("u1", Perfil.class).orElseThrow());

        try {
            cache.actualizar("zzz", new Perfil("X", "x@mail.com"));
        } catch (NoSuchElementException e) {
            System.out.println("Actualizar inexistente -> " + e.getMessage());
        }

        System.out.println("eliminar u2: " + cache.eliminar("u2"));
        System.out.println("tamaño: " + cache.tamanio());
    }
}
