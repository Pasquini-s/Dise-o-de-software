import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


interface ProcesadorPagos {
    boolean cobrar(double monto, String cliente);
}


final class PasarelaPago implements ProcesadorPagos {

    private static final class Holder {
        static final PasarelaPago INSTANCIA = new PasarelaPago();
    }

    private final List<String> transacciones = new CopyOnWriteArrayList<>();

    private PasarelaPago() { }

    public static PasarelaPago getInstancia() { return Holder.INSTANCIA; }

    @Override
    public boolean cobrar(double monto, String cliente) {
        transacciones.add(cliente + ":" + monto);
        System.out.println("  [Pasarela real] Cobrando $" + monto + " a " + cliente);
        return true;
    }

    public int totalTransacciones() { return transacciones.size(); }
}


final class ServicioCheckout {
    private final ProcesadorPagos procesador;

    public ServicioCheckout(ProcesadorPagos procesador) {
        this.procesador = Objects.requireNonNull(procesador);
    }

    public boolean finalizarCompra(String cliente, double total) {
        return procesador.cobrar(total, cliente);
    }
}

final class ServicioSuscripciones {
    private final ProcesadorPagos procesador;

    public ServicioSuscripciones(ProcesadorPagos procesador) {
        this.procesador = Objects.requireNonNull(procesador);
    }

    public boolean renovar(String cliente) {
        return procesador.cobrar(9.99, cliente);
    }
}


final class PasarelaFalsa implements ProcesadorPagos {
    final List<String> llamadas = new ArrayList<>();

    @Override
    public boolean cobrar(double monto, String cliente) {
        llamadas.add(cliente + ":" + monto);
        return false;                                    
    }
}

public class Ejercicio08 {
    public static void main(String[] args) {
       
        ProcesadorPagos pasarela = PasarelaPago.getInstancia();
        ServicioCheckout checkout = new ServicioCheckout(pasarela);
        ServicioSuscripciones suscripciones = new ServicioSuscripciones(pasarela);

        System.out.println("Producción:");
        checkout.finalizarCompra("Ana", 150.0);
        suscripciones.renovar("Luis");
        System.out.println("Transacciones en la única pasarela: "
                + PasarelaPago.getInstancia().totalTransacciones());

        
        System.out.println("\nTest con doble de prueba:");
        PasarelaFalsa falsa = new PasarelaFalsa();
        ServicioCheckout checkoutDeTest = new ServicioCheckout(falsa);
        boolean resultado = checkoutDeTest.finalizarCompra("Test", 10.0);
        System.out.println("Resultado: " + resultado + " | llamadas registradas: " + falsa.llamadas);
    }
}
