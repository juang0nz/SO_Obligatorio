import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class SimulacionCentroMedico {
    public static int tiempoGlobal = 0; // en minutos desde 08:00
    public static final PriorityBlockingQueue<Paciente> colaPacientes = new PriorityBlockingQueue<>();
    public static final Object lock = new Object();

    public static void main(String[] args) {
        GeneradorPacientes generador = new GeneradorPacientes();
        generador.start();

        Thread reloj = new Thread(() -> {
            while (tiempoGlobal < 720) { // 12 horas simuladas
                synchronized (lock) {
                    lock.notifyAll();
                }
                try {
                    Thread.sleep(5); // acelerado para no demorar
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    tiempoGlobal++;
                }
            }
        });
        reloj.start();

        Thread secretaria = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    while (colaPacientes.isEmpty() || colaPacientes.peek().horaLlegada > tiempoGlobal) {
                        try {
                            lock.wait();
                            if (tiempoGlobal >= 720) return;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Paciente p = colaPacientes.poll();
                    int finAtencion = tiempoGlobal + p.duracion;
                    String horaInicio = getHoraFormateada(tiempoGlobal);
                    String horaFin = getHoraFormateada(finAtencion);
                    System.out.println(horaInicio + " Atendiendo a: " + p + " (termina a las " + horaFin + ")");
                    tiempoGlobal = finAtencion;
                }
            }
        });
        secretaria.start();
    }

    public static String getHoraFormateada(int minutosDesde8AM) {
        int hora = 8 + (minutosDesde8AM / 60);
        int minutos = minutosDesde8AM % 60;
        return String.format("%02d:%02d", hora, minutos);
    }
}

class GeneradorPacientes extends Thread {
    Random rand = new Random();
    String[] nombres = {"Juan", "Ana", "Carlos", "Luisa", "Sofía", "Mateo", "Laura", "Pedro", "Valentina", "José"};

    public void run() {
        for (int i = 0; i < 3; i++) {
            agregarPaciente(randomTipo(), nombres[rand.nextInt(nombres.length)], 0);
        }

        while (SimulacionCentroMedico.tiempoGlobal < 720) {
            synchronized (SimulacionCentroMedico.lock) {
                if (SimulacionCentroMedico.tiempoGlobal % 60 == 0 && SimulacionCentroMedico.tiempoGlobal != 0) {
                    int cantidad = rand.nextInt(3) + 1;
                    for (int i = 0; i < cantidad; i++) {
                        agregarPaciente(randomTipo(), nombres[rand.nextInt(nombres.length)], SimulacionCentroMedico.tiempoGlobal);
                    }
                }
            }
            try {
                Thread.sleep(5); // acelerado
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void agregarPaciente(String tipo, String nombre, int hora) {
        int prioridad;
        String prioridadTexto;
        if ("emergencia".equals(tipo)) {
            prioridad = 10;
            prioridadTexto = "EMERGENCIA";
        } else if ("control".equals(tipo)) {
            prioridad = 5;
            prioridadTexto = "CONTROL";
        } else {
            prioridad = 3;
            prioridadTexto = "ANÁLISIS";
        }

        int duracion = tipo.equals("emergencia") ? 10 : rand.nextInt(5) + 5;
        Paciente p = new Paciente(nombre, tipo, hora, prioridad, prioridadTexto, duracion);
        SimulacionCentroMedico.colaPacientes.add(p);
        System.out.println(SimulacionCentroMedico.getHoraFormateada(hora) + " Programado: " + p);
    }

    private String randomTipo() {
        String[] tipos = {"control", "analisis", "emergencia"};
        return tipos[rand.nextInt(tipos.length)];
    }
}

class Paciente implements Comparable<Paciente> {
    String nombre;
    String tipo;
    int horaLlegada;
    int prioridad;
    String prioridadTexto;
    int duracion;

    public Paciente(String nombre, String tipo, int horaLlegada, int prioridad, String prioridadTexto, int duracion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.horaLlegada = horaLlegada;
        this.prioridad = prioridad;
        this.prioridadTexto = prioridadTexto;
        this.duracion = duracion;
    }

    @Override
    public int compareTo(Paciente otro) {
        return Integer.compare(otro.prioridad, this.prioridad); // Prioridad más alta primero
    }

    @Override
    public String toString() {
        return nombre + " [" + tipo.toUpperCase() + " | " + prioridadTexto + " (" + prioridad + ") | " + duracion + " min]";
    }
}
