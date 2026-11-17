import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

enum TipoMovimiento { ENTRADA, SALIDA }

record Movimiento(String codigoInsumo, TipoMovimiento tipo, int cantidad, String sector,
                  LocalDateTime fecha) {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public String toString() {
        return "[" + fecha.format(FORMATO) + "] " + codigoInsumo + " | " + tipo + " | "
                + cantidad + " | " + sector;
    }
}


final class RegistroMovimientos {

    private static final class Holder {
        static final RegistroMovimientos INSTANCIA = new RegistroMovimientos();
    }

    private final List<Movimiento> movimientos = new ArrayList<>();

    private RegistroMovimientos() { }

    public static RegistroMovimientos getInstancia() { return Holder.INSTANCIA; }

    public synchronized Movimiento registrar(String codigoInsumo, TipoMovimiento tipo,
                                             int cantidad, String sector) {
        if (codigoInsumo == null || codigoInsumo.isBlank()) {
            throw new IllegalArgumentException("El código de insumo no puede estar vacío");
        }
        if (sector == null || sector.isBlank()) {
            throw new IllegalArgumentException("El sector no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (tipo == TipoMovimiento.SALIDA) {
            int saldo = saldoDe(codigoInsumo);
            if (cantidad > saldo) {
                throw new IllegalStateException("La salida de " + cantidad + " de " + codigoInsumo
                        + " supera el saldo disponible (" + saldo + ")");
            }
        }
        Movimiento m = new Movimiento(codigoInsumo, tipo, cantidad, sector, LocalDateTime.now());
        movimientos.add(m);
        return m;
    }

   
    public synchronized int saldoDe(String codigoInsumo) {
        int saldo = 0;
        for (Movimiento m : movimientos) {
            if (m.codigoInsumo().equals(codigoInsumo)) {
                saldo += (m.tipo() == TipoMovimiento.ENTRADA) ? m.cantidad() : -m.cantidad();
            }
        }
        return saldo;
    }

    public synchronized Map<String, Integer> saldos() {
        Map<String, Integer> resultado = new TreeMap<>();
        for (Movimiento m : movimientos) {
            resultado.merge(m.codigoInsumo(),
                    m.tipo() == TipoMovimiento.ENTRADA ? m.cantidad() : -m.cantidad(), Integer::sum);
        }
        return resultado;
    }

    public synchronized List<Movimiento> historial() { return List.copyOf(movimientos); }

    public synchronized List<Movimiento> porInsumo(String codigoInsumo) {
        return movimientos.stream().filter(m -> m.codigoInsumo().equals(codigoInsumo)).toList();
    }

    public synchronized List<Movimiento> porSector(String sector) {
        return movimientos.stream().filter(m -> m.sector().equalsIgnoreCase(sector)).toList();
    }
}


final class SectorHospital {
    private final String nombre;
    private final RegistroMovimientos registro = RegistroMovimientos.getInstancia();

    public SectorHospital(String nombre) { this.nombre = nombre; }

    public RegistroMovimientos getRegistro() { return registro; }

    public void entrada(String insumo, int cantidad) {
        registro.registrar(insumo, TipoMovimiento.ENTRADA, cantidad, nombre);
        System.out.println("  [" + nombre + "] ENTRADA " + cantidad + " de " + insumo + " -> OK");
    }

    public void salida(String insumo, int cantidad) {
        try {
            registro.registrar(insumo, TipoMovimiento.SALIDA, cantidad, nombre);
            System.out.println("  [" + nombre + "] SALIDA " + cantidad + " de " + insumo + " -> OK");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("  [" + nombre + "] SALIDA " + cantidad + " de " + insumo
                    + " -> RECHAZADA: " + e.getMessage());
        }
    }
}

public class Hospital08 {
    public static void main(String[] args) {
        SectorHospital deposito = new SectorHospital("Depósito");
        SectorHospital consultorios = new SectorHospital("Consultorios");
        SectorHospital laboratorio = new SectorHospital("Laboratorio");

        System.out.println("¿Los tres sectores usan el mismo registro? "
                + (deposito.getRegistro() == consultorios.getRegistro()
                && consultorios.getRegistro() == laboratorio.getRegistro()));

        System.out.println("\nSaldos iniciales (se cargan como movimientos de ENTRADA):");
        deposito.entrada("GASA-001", 100);
        deposito.entrada("JERINGA-005", 200);
        deposito.entrada("REACTIVO-010", 50);

        System.out.println("\nMovimientos de los sectores:");
        consultorios.salida("GASA-001", 30);
        laboratorio.salida("REACTIVO-010", 20);
        consultorios.salida("JERINGA-005", 80);
        laboratorio.salida("GASA-001", 80);                        
        laboratorio.salida("GASA-001", 20);
        deposito.entrada("GASA-001", 40);

        System.out.println("\nValidaciones:");
        consultorios.salida("GASA-001", 0);                        
        consultorios.salida("INSUMO-FANTASMA", 5);                 

        RegistroMovimientos registro = RegistroMovimientos.getInstancia();

        System.out.println("\nHistorial completo:");
        registro.historial().forEach(m -> System.out.println("  " + m));

        System.out.println("\nFiltrado por insumo GASA-001:");
        registro.porInsumo("GASA-001").forEach(m -> System.out.println("  " + m));

        System.out.println("\nFiltrado por sector Laboratorio:");
        registro.porSector("Laboratorio").forEach(m -> System.out.println("  " + m));

        System.out.println("\nSaldo de cada insumo:");
        registro.saldos().forEach((insumo, saldo) -> System.out.println("  " + insumo + ": " + saldo));

        System.out.println("\nLos movimientos no se pueden editar ni eliminar desde afuera:");
        try {
            registro.historial().remove(0);
        } catch (UnsupportedOperationException e) {
            System.out.println("  Eliminar del historial -> bloqueado (UnsupportedOperationException)");
        }
        System.out.println("  Movimientos registrados: " + registro.historial().size());
    }
}
