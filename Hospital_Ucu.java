import java.util.concurrent.Semaphore;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class Hospital_Ucu {

    public static void main(String[] args) {
        Semaphore medico = new Semaphore(1);
        Semaphore reloj = new Semaphore(1);
        int Medicos = 0; // Número de médicos
        SecretariaPacientes Secretaria = new SecretariaPacientes();
        int tiempoSimulado = 800;

        List<Integer> Tiempos = leerTiemposDeAtencion("pacientes.txt");
        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("pacientes.txt");
        List<Paciente> pacientesAEliminar = new ArrayList<>();
        int Tiempo_Emergencia = Tiempos.get(0);
        int Tiempo_Urgencia = Tiempos.get(1);
        int Tiempo_Comunes = Tiempos.get(2);

        while (tiempoSimulado < 2300) { // Simulación por 60 minutos
            simularTiempo(tiempoSimulado, Secretaria, medico, Medicos, listaPacientes, pacientesAEliminar, reloj);
            tiempoSimulado += 10;
            if (Medicos == 0) {
                // Iniciar el hilo del médico
                new Hilo_Medico(Secretaria, medico, reloj).start();
                Medicos = 1; // Si no hay médicos, se asigna uno por defecto
            }
        }
        System.out.println("Simulación finalizada. Revisa el archivo reporte.txt para ver los resultados.");
    }

    static void simularTiempo(int tiempoSimulado, SecretariaPacientes Secretaria, Semaphore medico, int Medicos,
            List<Paciente> listaPacientes, List<Paciente> pacientesAEliminar, Semaphore reloj) {
        try {
            reloj.acquire();
            System.out.println("Tiempo Simulado: " + tiempoSimulado);
            if (!listaPacientes.isEmpty()) {
                for (int i = 0; i < listaPacientes.size(); i++) {
                    Paciente p = listaPacientes.get(i);
                    if (p.horaLlegada == tiempoSimulado) {
                        new Hilo_Paciente(p.nombre, p.urgente, p.horaLlegada, Secretaria, medico).start();
                        pacientesAEliminar.add(p);
                    }
                }
                listaPacientes.removeAll(pacientesAEliminar);
                pacientesAEliminar.clear();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            medico.release();
        }
    }

    static List<Integer> leerTiemposDeAtencion(String nombreArchivo) {
        List<Integer> Atenciones = new ArrayList<>(Arrays.asList(0, 0, 0));
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 2) {
                    String Especialidad = partes[0].trim();
                    if (Especialidad.equals("Emergencia")) {
                        Atenciones.set(0, Integer.parseInt(partes[1].trim()));
                    } else if (Especialidad.equals("Urgencia")) {
                        Atenciones.set(1, Integer.parseInt(partes[1].trim()));
                    } else if (Especialidad.equals("Comun")) {
                        Atenciones.set(2, Integer.parseInt(partes[1].trim()));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
        return Atenciones;
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

        public synchronized Optional<Paciente> obtenerSiguientePaciente() {
            if (!Lista_Urgentes.isEmpty()) {
                return Optional.of(Lista_Urgentes.poll());
            } else if (!Lista_comunes.isEmpty()) {
                return Optional.of(Lista_comunes.poll());
            } else {
                return Optional.empty();
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
        Semaphore reloj;

        int totalPacientes = 0;
        int emergencias = 0;
        int urgentes = 0;
        int comunes = 0;
        int tiempoActual = 0;

        List<String> logAtenciones = new ArrayList<>();

        public Hilo_Medico(SecretariaPacientes Secretaria, Semaphore medico, Semaphore reloj) {
            this.Secretaria = Secretaria;
            this.medico = medico;
            this.reloj = reloj;
        }

        @Override
        public void run() {
            while (totalPacientes < 6) {
                try {
                    Optional<Paciente> OptionalPa = Secretaria.obtenerSiguientePaciente();
                    if (OptionalPa.isPresent()) {
                        Paciente p = OptionalPa.get();

                        medico.acquire();

                        System.out
                                .println("Medico atiende a: " + p.nombre + " (" + (p.urgente ? "Urgente" : "Comun")
                                        + ")");
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
                        reloj.release();
                    } else {
                        System.out.println("No hay pacientes en espera. Médico esperando...");
                        Thread.sleep(1000);
                        reloj.release();
                    }
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
