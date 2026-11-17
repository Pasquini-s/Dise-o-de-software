import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;

class DispositivoOcupadoException extends RuntimeException {
    public DispositivoOcupadoException(String mensaje) { super(mensaje); }
}

final class ControladorHardware implements Serializable {

    private static boolean creado = false;

    private static final class Holder {
        static final ControladorHardware INSTANCIA = new ControladorHardware();
    }

    private final transient ReentrantLock puerto = new ReentrantLock();
    private volatile String estado = "INACTIVO";
    private volatile String ultimoComando = null;

    private ControladorHardware() {
        synchronized (ControladorHardware.class) {
            if (creado) {                                
                throw new IllegalStateException("Ya existe una instancia del controlador");
            }
            creado = true;
        }
    }

    public static ControladorHardware getInstancia() { return Holder.INSTANCIA; }

    @Override
    protected Object clone() throws CloneNotSupportedException {   
        throw new CloneNotSupportedException("Singleton: no se puede clonar");
    }

    private Object readResolve() {                       
        return Holder.INSTANCIA;
    }

    public boolean estaDisponible() { return !puerto.isLocked(); }

    public String enviarComando(String comando) {
        if (!puerto.tryLock()) {                         
            throw new DispositivoOcupadoException("El puerto serial está en uso");
        }
        try {
            estado = "EJECUTANDO";
            Thread.sleep(50);                            
            ultimoComando = comando;
            estado = "LISTO";
            return "OK:" + comando;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido", e);
        } finally {
            puerto.unlock();
        }
    }

    public String leerEstado() {
        return "estado=" + estado + ", ultimoComando=" + ultimoComando;
    }
}

public class Ejercicio05 {
    public static void main(String[] args) throws Exception {
        ControladorHardware hw = ControladorHardware.getInstancia();
        System.out.println("Disponible: " + hw.estaDisponible());

        Thread t = new Thread(() -> System.out.println(hw.enviarComando("MOVER_BRAZO")));
        t.start();
        Thread.sleep(10);
        try {
            ControladorHardware.getInstancia().enviarComando("GIRAR");
        } catch (DispositivoOcupadoException e) {
            System.out.println("Segundo comando rechazado -> " + e.getMessage());
        }
        t.join();
        System.out.println("Estado: " + hw.leerEstado());

    
        try {
            Constructor<ControladorHardware> c = ControladorHardware.class.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();
        } catch (InvocationTargetException e) {
            System.out.println("Reflexión bloqueada -> " + e.getCause().getMessage());
        }

        try {
            hw.clone();
        } catch (CloneNotSupportedException e) {
            System.out.println("Clonación bloqueada -> " + e.getMessage());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) { oos.writeObject(hw); }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            System.out.println("Deserializado es la misma instancia: " + (ois.readObject() == hw));
        }
    }
}
