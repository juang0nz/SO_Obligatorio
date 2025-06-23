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
    volatile static boolean simulacionActiva = true;
    volatile static int tiempoSimulado = 800;

    public static void main(String[] args) {

        new RelojMundial().start(); // Inicia el reloj mundial

    }

    static class RelojMundial extends Thread {
        Semaphore medico = new Semaphore(0);
        Semaphore reloj = new Semaphore(1);
        Semaphore pacientes = new Semaphore(1);
        int Medicos = 0; // Número de médicos
        SecretariaPacientes Secretaria = new SecretariaPacientes();

        List<Integer> Tiempos = leerTiemposDeAtencion("pacientes.txt");
        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("pacientes.txt");
        List<Paciente> pacientesAEliminar = new ArrayList<>();
        int Tiempo_Emergencia = Tiempos.get(0);
        int Tiempo_Urgencia = Tiempos.get(1);
        int Tiempo_Comunes = Tiempos.get(2);

        public synchronized void run() {
            while (Hospital_Ucu.tiempoSimulado < 2300) { // Simulación por 60 minutos
                System.out.println("Tiempo simulado: " + Hospital_Ucu.tiempoSimulado + " minutos");
                try {
                    reloj.acquire();
                    if (!listaPacientes.isEmpty()) {
                        for (int i = 0; i < listaPacientes.size(); i++) {
                            Paciente p = listaPacientes.get(i);
                            if (p.horaLlegada == tiempoSimulado) {
                                new Hilo_Paciente(p.nombre, p.area, p.horaLlegada, Secretaria, medico,
                                        pacientes)
                                        .start();
                                Secretaria.agregarPaciente(p);
                                listaPacientes.remove(p);
                            }
                        }
                    }
                    medico.release(); // Libera el semáforo del médico para que pueda atender pacientes
                    Hospital_Ucu.tiempoSimulado += 10;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (Medicos == 0) {
                    // Iniciar el hilo del médico
                    new Hilo_Medico(Secretaria, medico, reloj, Tiempo_Comunes, Tiempo_Urgencia, Tiempo_Emergencia,
                            pacientes).start();
                    Medicos = 1; // Si no hay médicos, se asigna uno por defecto
                }
            }
            Hospital_Ucu.simulacionActiva = false;
            System.out.println("Simulación finalizada. Revisa el archivo reporte.txt para ver los resultados.");
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
                    System.out.println("Tiempo de atención leído: " + Especialidad + " - " + partes[1].trim());
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
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    System.out.println(
                            "Paciente leído: " + nombre + ", Área: " + area + ", Hora de llegada: " + horaLlegada);
                    pacientes.add(new Paciente(nombre, area, horaLlegada));
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
        String area; // Área de atención (Urgente, Común, Emergencia)
        int horaLlegada;

        Paciente(String nombre, String area, int horaLlegada) {
            this.nombre = nombre;
            this.area = area;
            this.horaLlegada = horaLlegada;
        }
    }

    // Maneja las listas de pacientes
    static class SecretariaPacientes {
        Queue<Paciente> Lista_Urgentes = new LinkedList<>(); // Lista de pacientes urgentes
        Queue<Paciente> Lista_comunes = new LinkedList<>(); // Lista de pacientes comunes
        Queue<Paciente> Lista_Emergencia = new LinkedList<>(); // Lista de pacientes de emergencia

        public synchronized void agregarPaciente(Paciente p) {

            if (p.area.equals("Emergencia")) {
                Lista_Emergencia.add(p);
                System.out.println("Paciente de emergencia ingresado: " + p.nombre);
            } else if (p.area.equals("Urgente")) {
                Lista_Urgentes.add(p);
                System.out.println("Paciente urgente ingresado: " + p.nombre);
            } else if (p.area.equals("Comun")) {
                Lista_comunes.add(p);
                System.out.println("Paciente comun ingresado: " + p.nombre);
            }

            // Libera el semáforo de la secretaria
        }

        public Paciente obtenerSiguientePaciente() {
            if (!Lista_Urgentes.isEmpty()) {
                return Lista_Urgentes.poll();
            } else if (!Lista_comunes.isEmpty()) {
                return Lista_comunes.poll();
            }
            return null; // No hay pacientes en espera
        }
    }

    // Hilo Paciente
    static class Hilo_Paciente extends Thread {
        String nombre;
        String area;
        SecretariaPacientes Secretaria;
        Semaphore medico;
        Semaphore pacientes; // Semáforo para controlar el acceso a la secretaria
        int horaLlegada;

        public Hilo_Paciente(String nombre, String area, int horaLlegada, SecretariaPacientes Secretaria,
                Semaphore medico, Semaphore pacientes) {
            this.nombre = nombre;
            this.area = area;
            this.horaLlegada = horaLlegada;
            this.Secretaria = Secretaria;
            this.medico = medico;
            this.pacientes = pacientes;
        }
    }

    // Hilo Médico
    static class Hilo_Medico extends Thread {
        SecretariaPacientes Secretaria;
        Semaphore medico;
        Semaphore reloj;
        Semaphore pacientes; // Semáforo para controlar el acceso a la secretaria
        boolean Paciente_Con_Emergencia = false;
        boolean paciente_en_Sala = false; // Indica si hay un paciente en la sala de espera
        Paciente p = null; // Paciente actual en la sala de espera;
        Paciente pEmergencia = null; // Paciente de emergencia actual

        int Tiempo_Comunes;
        int Tiempo_Urgencia;
        int Tiempo_Emergencia;

        int totalPacientes = 0;
        int emergencias = 0;
        int urgentes = 0;
        int comunes = 0;
        int tiempoActualDeAtencion = 0;
        int tiempoDeEmergencia = 0; // Tiempo de atención de emergencia
        int tiempoTotalDeAtencion = 0; // Tiempo total de atención acumulado

        List<String> logAtenciones = new ArrayList<>();

        public Hilo_Medico(SecretariaPacientes Secretaria, Semaphore medico, Semaphore reloj, int Tiempo_Comunes,
                int Tiempo_Urgencia, int Tiempo_Emergencia, Semaphore pacientes) {
            this.Secretaria = Secretaria;
            this.medico = medico;
            this.reloj = reloj;
            this.Tiempo_Comunes = Tiempo_Comunes;
            this.Tiempo_Urgencia = Tiempo_Urgencia;
            this.Tiempo_Emergencia = Tiempo_Emergencia;
            this.pacientes = pacientes;
        }

        @Override
        public synchronized void run() {
            while (Hospital_Ucu.simulacionActiva) { // Simulación por 60 minutos
                try {
                    medico.acquire();
                    if (Paciente_Con_Emergencia) {
                        tiempoDeEmergencia -= 10;
                        System.out.println(
                                "Atendiendo paciente de emergencia: " + pEmergencia.nombre + " - Tiempo restante: "
                                        + tiempoDeEmergencia);
                        if (tiempoDeEmergencia == 0) {
                            totalPacientes++;
                            emergencias++;
                            Paciente_Con_Emergencia = false;
                            logAtenciones.add(pEmergencia.nombre + " - Emergencia | Inicio: "
                                    + pEmergencia.horaLlegada + " | Fin: " + Hospital_Ucu.tiempoSimulado);
                        }
                        sleep(20);
                        reloj.release();
                        pacientes.release(); // Libera el semáforo de la secretaria
                    } else if (!Secretaria.Lista_Emergencia.isEmpty()) {
                        pEmergencia = Secretaria.Lista_Emergencia.poll();
                        Paciente_Con_Emergencia = true;
                        tiempoDeEmergencia = Tiempo_Emergencia;
                        sleep(20);
                        reloj.release();
                        pacientes.release(); // Libera el semáforo de la secretaria
                    } else if (paciente_en_Sala) {
                        tiempoActualDeAtencion = tiempoActualDeAtencion - 10;
                        System.out.println("Estoy siendo atendido paciente: " + p.nombre + " - " + p.area
                                + " - Tiempo restante: " + tiempoActualDeAtencion);
                        if (tiempoActualDeAtencion == 0) {
                            totalPacientes++;
                            if (p.area.equals("Urgente")) {
                                urgentes++;
                            } else if (p.area.equals("Comun")) {
                                comunes++;
                            }
                            paciente_en_Sala = false;
                            System.out.println("Paciente atendido: " + p.nombre + " - " + p.area
                                    + " - Tiempo total de atención: " + tiempoActualDeAtencion);
                            logAtenciones.add(p.nombre + " - " + p.area + " | Inicio: " + p.horaLlegada + " | Fin: "
                                    + Hospital_Ucu.tiempoSimulado);
                        }
                        sleep(20);
                        reloj.release();
                        pacientes.release(); // Libera el semáforo de la secretaria
                    } else if (!Secretaria.Lista_Urgentes.isEmpty() || !Secretaria.Lista_comunes.isEmpty()) {
                        p = Secretaria.obtenerSiguientePaciente();
                        if (p.area.equals("Urgente")) {
                            tiempoActualDeAtencion = Tiempo_Urgencia;
                        } else if (p.area.equals("Comun")) {
                            tiempoActualDeAtencion = Tiempo_Comunes;
                        }
                        System.out.println(
                                "Atendiendo paciente: " + p.nombre + " - " + p.area + " - Tiempo restante: "
                                        + tiempoActualDeAtencion);
                        paciente_en_Sala = true;
                        sleep(20);
                        reloj.release();
                        pacientes.release(); // Libera el semáforo de la secretaria
                    } else {
                        System.out.println("No hay pacientes en espera. Médico esperando...");
                        sleep(20);
                        reloj.release();
                        pacientes.release(); // Libera el semáforo de la secretaria
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try (
                    FileWriter fw = new FileWriter("reporte.txt")) {
                fw.write("    Reporte Final \n");
                fw.write("Total pacientes atendidos: " + totalPacientes + "\n");
                fw.write("  Emergencias : " + emergencias + "\n");
                fw.write("  Urgentes: " + urgentes + "\n");
                fw.write("  Comunes : " + comunes + "\n");
                fw.write("  Tiempo Total de Atención : " + tiempoTotalDeAtencion + "\n");
                for (String log : logAtenciones) {
                    fw.write(log + "\n");
                }
            } catch (IOException e) {
                System.err.println("Error escribiendo el reporte: " + e.getMessage());
            }
        }

    }
}