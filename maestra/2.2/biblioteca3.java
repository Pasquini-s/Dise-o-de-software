import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


record Libro(String codigo, String titulo, String autor, int ejemplares) {
    Libro {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código no puede estar vacío");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("El autor no puede estar vacío");
        }
        if (ejemplares < 0) {
            throw new IllegalArgumentException("La cantidad de ejemplares no puede ser negativa");
        }
    }

    Libro conEjemplares(int nuevaCantidad) {
        return new Libro(codigo, titulo, autor, nuevaCantidad);
    }

    @Override
    public String toString() {
        return codigo + " | " + titulo + " | " + autor + " | ejemplares: " + ejemplares;
    }
}


final class CatalogoLibros {

    private static final class Holder {
        static final CatalogoLibros INSTANCIA = new CatalogoLibros();
    }

    private final Map<String, Libro> libros = new LinkedHashMap<>();

    private CatalogoLibros() { }

    public static CatalogoLibros getInstancia() { return Holder.INSTANCIA; }

    public synchronized void agregar(String codigo, String titulo, String autor, int ejemplares) {
        Libro nuevo = new Libro(codigo, titulo, autor, ejemplares);   
        if (libros.containsKey(nuevo.codigo())) {
            throw new IllegalArgumentException("Ya existe un libro con el código " + nuevo.codigo());
        }
        libros.put(nuevo.codigo(), nuevo);
    }

    public synchronized List<Libro> buscarPorTitulo(String texto) {
        String t = normalizar(texto);
        return libros.values().stream()
                .filter(l -> l.titulo().toLowerCase().contains(t)).toList();
    }

    public synchronized List<Libro> buscarPorAutor(String texto) {
        String t = normalizar(texto);
        return libros.values().stream()
                .filter(l -> l.autor().toLowerCase().contains(t)).toList();
    }

    public synchronized void actualizarEjemplares(String codigo, int nuevaCantidad) {
        Libro actual = libros.get(codigo);
        if (actual == null) {
            throw new NoSuchElementException("No existe un libro con el código " + codigo);
        }
        libros.put(codigo, actual.conEjemplares(nuevaCantidad));      
    }

    
    public synchronized List<Libro> listar() { return List.copyOf(libros.values()); }

    private static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El texto de búsqueda no puede estar vacío");
        }
        return texto.trim().toLowerCase();
    }
}


final class PantallaPersonal {
    private final CatalogoLibros catalogo = CatalogoLibros.getInstancia();

    public CatalogoLibros getCatalogo() { return catalogo; }

    public void agregarLibro(String codigo, String titulo, String autor, int ejemplares) {
        catalogo.agregar(codigo, titulo, autor, ejemplares);
        System.out.println("  [Personal] Libro agregado: " + codigo + " - " + titulo);
    }

    public void actualizarEjemplares(String codigo, int cantidad) {
        catalogo.actualizarEjemplares(codigo, cantidad);
        System.out.println("  [Personal] Ejemplares de " + codigo + " actualizados a " + cantidad);
    }
}


final class PantallaConsulta {
    private final CatalogoLibros catalogo = CatalogoLibros.getInstancia();

    public CatalogoLibros getCatalogo() { return catalogo; }

    public void mostrarCatalogo() {
        System.out.println("  [Consulta] Catálogo actual:");
        catalogo.listar().forEach(l -> System.out.println("     " + l));
    }

    public void buscar(String texto) {
        System.out.println("  [Consulta] Buscando '" + texto + "':");
        List<Libro> porTitulo = catalogo.buscarPorTitulo(texto);
        List<Libro> porAutor = catalogo.buscarPorAutor(texto);
        if (porTitulo.isEmpty() && porAutor.isEmpty()) System.out.println("     (sin resultados)");
        porTitulo.forEach(l -> System.out.println("     por título: " + l));
        porAutor.forEach(l -> System.out.println("     por autor : " + l));
    }
}

public class Biblioteca03 {
    public static void main(String[] args) {
        PantallaPersonal personal = new PantallaPersonal();
        PantallaConsulta consulta = new PantallaConsulta();

        System.out.println("¿Ambas pantallas usan el mismo catálogo? "
                + (personal.getCatalogo() == consulta.getCatalogo()));

        System.out.println("\nCarga inicial desde la pantalla del personal:");
        personal.agregarLibro("L-001", "El Aleph", "Jorge Luis Borges", 3);
        personal.agregarLibro("L-002", "Rayuela", "Julio Cortázar", 2);
        consulta.mostrarCatalogo();

        System.out.println("\nEl personal agrega un libro NUEVO (la consulta no recarga nada):");
        personal.agregarLibro("L-003", "Ficciones", "Jorge Luis Borges", 4);
        consulta.mostrarCatalogo();

        System.out.println("\nBúsquedas desde la pantalla de consulta:");
        consulta.buscar("borges");
        consulta.buscar("rayu");
        consulta.buscar("tolkien");

        System.out.println("\nActualización de ejemplares:");
        personal.actualizarEjemplares("L-002", 5);
        consulta.mostrarCatalogo();

        System.out.println("\nValidaciones:");
        intentar("código duplicado", () -> personal.agregarLibro("L-001", "Otro título", "Alguien", 1));
        intentar("título vacío", () -> personal.agregarLibro("L-010", "  ", "Alguien", 1));
        intentar("cantidad negativa", () -> personal.agregarLibro("L-011", "Nuevo", "Alguien", -2));
        intentar("actualizar a negativo", () -> personal.actualizarEjemplares("L-001", -1));
        intentar("actualizar código inexistente", () -> personal.actualizarEjemplares("L-999", 1));

        System.out.println("\nCatálogo final (sin cambios por los intentos inválidos):");
        consulta.mostrarCatalogo();
    }

    private static void intentar(String descripcion, Runnable accion) {
        try {
            accion.run();
            System.out.println("  " + descripcion + " -> aceptado (inesperado)");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.out.println("  " + descripcion + " -> rechazado: " + e.getMessage());
        }
    }
}
