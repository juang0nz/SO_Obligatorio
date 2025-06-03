import java.util.concurrent.Semaphore;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Hospital_Ucu {

    public static void main(String[] args) {
        Semaphore medico = new Semaphore(1);
        SecretariaPacientes Secretaria = new SecretariaPacientes();
        int tiempoSimulado = 0;

        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("pacientes.txt");
        List<Paciente> pacientesAEliminar = new ArrayList<>();

        // Iniciar el hilo del médico
        new Hilo_Medico(Secretaria, medico).start();

        while (!listaPacientes.isEmpty()) {
            for (int i = 0; i < listaPacientes.size(); i++) {
                Paciente p = listaPacientes.get(i);
                if (p.horaLlegada == tiempoSimulado) {
                    new Hilo_Paciente(p.nombre, p.urgente, p.horaLlegada, Secretaria, medico).start();
                    pacientesAEliminar.add(p);
                }
            }
            listaPacientes.removeAll(pacientesAEliminar);
            pacientesAEliminar.clear();
            try {
                Thread.sleep(1); // Simula el paso del tiempo (1 segundo)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tiempoSimulado++;
        }
    }

    static List<Paciente> leerPacientesDesdeArchivo(String nombreArchivo) {
        List<Paciente> pacientes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 3) {
                    String nombre = partes[0].trim();
                    boolean urgente = Boolean.parseBoolean(partes[1].trim());
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    pacientes.add(new Paciente(nombre, urgente, horaLlegada));
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
        return pacientes;
    }

    // Paciente con nombre y prioridad
    static class Paciente {
        String nombre;
        boolean urgente;
        int horaLlegada;

        Paciente(String nombre, boolean urgente, int horaLlegada) {
            this.nombre = nombre;
            this.urgente = urgente;
            this.horaLlegada = horaLlegada;
        }
    }

    // Maneja las listas de pacientes
    static class SecretariaPacientes {
        Queue<Paciente> Lista_Urgentes = new LinkedList<>(); // Lista de pacientes urgentes
        Queue<Paciente> Lista_comunes = new LinkedList<>(); // Lista de pacientes comunes

        public synchronized void agregarPaciente(Paciente p) {
            if (p.urgente) {
                Lista_Urgentes.add(p);
                System.out.println("Paciente urgente ingresado: " + p.nombre);
            } else {
                Lista_comunes.add(p);
                System.out.println("Paciente comun ingresado: " + p.nombre);
            }
            notify();
        }

        public synchronized Paciente obtenerSiguientePaciente() {
            while (Lista_Urgentes.isEmpty() && Lista_comunes.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!Lista_Urgentes.isEmpty()) {
                return Lista_Urgentes.poll();
            } else {
                return Lista_comunes.poll();
            }
        }
    }

    // Hilo Paciente
    static class Hilo_Paciente extends Thread {
        String nombre;
        boolean urgente;
        SecretariaPacientes Secretaria;
        Semaphore medico;
        int horaLlegada;

        public Hilo_Paciente(String nombre, boolean urgente, int horaLlegada, SecretariaPacientes Secretaria,
                Semaphore medico) {
            this.nombre = nombre;
            this.urgente = urgente;
            this.horaLlegada = horaLlegada;
            this.Secretaria = Secretaria;
            this.medico = medico;
        }

        @Override
        public void run() {
            Paciente p = new Paciente(nombre, urgente, horaLlegada);
            Secretaria.agregarPaciente(p);
        }
    }

    // Hilo Médico
    static class Hilo_Medico extends Thread {
        SecretariaPacientes Secretaria;
        Semaphore medico;

        int totalPacientes = 0;
        int urgentes = 0;
        int comunes = 0;
        int tiempoActual = 0;

        List<String> logAtenciones = new ArrayList<>();

        public Hilo_Medico(SecretariaPacientes Secretaria, Semaphore medico) {
            this.Secretaria = Secretaria;
            this.medico = medico;
        }

        @Override
        public void run() {
            while (totalPacientes < 6) {
                try {
                    Paciente p = Secretaria.obtenerSiguientePaciente();

                    medico.acquire();

                    System.out
                            .println("Medico atiende a: " + p.nombre + " (" + (p.urgente ? "Urgente" : "comun") + ")");
                    tiempoActual += 10;
                    System.out.println("Medico termino con: " + p.nombre);

                    totalPacientes++;
                    if (p.urgente) {
                        urgentes++;
                    } else {
                        comunes++;
                    }

                    logAtenciones.add(p.nombre + " - " + (p.urgente ? "Urgente" : "Común") + " | Inicio: "
                            + p.horaLlegada + " | Fin: " + (p.horaLlegada + 10));

                    medico.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try (FileWriter fw = new FileWriter("reporte.txt")) {
                fw.write("    Reporte Final \n");
                fw.write("Total pacientes atendidos: " + totalPacientes + "\n");
                fw.write("  Urgentes: " + urgentes + "\n");
                fw.write("  comunes : " + comunes + "\n");
                fw.write("  Tiempo Total de Atención : " + tiempoActual + "\n");
                for (String log : logAtenciones) {
                    fw.write(log + "\n");
                }
            } catch (IOException e) {
                System.err.println("Error escribiendo el reporte: " + e.getMessage());
            }
        }
    }
}
