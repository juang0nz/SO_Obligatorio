
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.io.*;

public class SimulacionCentroMedico {
    public static int tiempoGlobal = 0; // en minutos desde 08:00
    public static final PriorityBlockingQueue<Paciente> colaPacientes = new PriorityBlockingQueue<>();
    public static final Object lock = new Object();
    public static List<String> logAtencion = new ArrayList<>();

    public static void main(String[] args) {
        GeneradorPacientes generador = new GeneradorPacientes("pacientes.txt");
        generador.start();

        Thread reloj = new Thread(() -> {
            while (tiempoGlobal < 720) { // 12 horas simuladass
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

        Thread recepcionista = new Thread(() -> {
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
                    String log = horaInicio + " Atendiendo a: " + p + " (termina a las " + horaFin + ")";
                    System.out.println(log);
                    logAtencion.add(log);
                    tiempoGlobal = finAtencion;
                }
            }
        });
        recepcionista.start();

        try {
            reloj.join();
            recepcionista.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Guardar log de atención
        try (FileWriter fw = new FileWriter("reporte.txt")) {
            for (String linea : logAtencion) {
                fw.write(linea + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error escribiendo el archivo de reporte: " + e.getMessage());
        }

        System.out.println("Fin de la simulación. Revisa el archivo reporte.txt.");
    }

    public static String getHoraFormateada(int minutosDesde8AM) {
        int hora = 8 + (minutosDesde8AM / 60);
        int minutos = minutosDesde8AM % 60;
        return String.format("%02d:%02d", hora, minutos);
    }
}

class GeneradorPacientes extends Thread {
    String archivo;

    public GeneradorPacientes(String archivo) {
        this.archivo = archivo;
    }

    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 3) {
                    String nombre = partes[0].trim();
                    String tipo = partes[1].trim().toLowerCase();
                    int llegada = Integer.parseInt(partes[2].trim());
                    agregarPaciente(tipo, nombre, llegada);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de pacientes: " + e.getMessage());
        }
    }

    private void agregarPaciente(String tipo, String nombre, int hora) {
        int prioridad;
        String prioridadTexto;
        int duracion;
        if ("emergencia".equals(tipo)) {
            prioridad = 10;
            prioridadTexto = "EMERGENCIA";
            duracion = 10;
        } else if ("control".equals(tipo)) {
            prioridad = 5;
            prioridadTexto = "CONTROL";
            duracion = 5 + new Random().nextInt(5);
        } else {
            prioridad = 3;
            prioridadTexto = "ANÁLISIS";
            duracion = 5 + new Random().nextInt(5);
        }

        Paciente p = new Paciente(nombre, tipo, hora, prioridad, prioridadTexto, duracion);
        synchronized (SimulacionCentroMedico.lock) {
            SimulacionCentroMedico.colaPacientes.add(p);
            System.out.println(SimulacionCentroMedico.getHoraFormateada(hora) + " Programado: " + p);
        }
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
