import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Hospital_Ucu {
    volatile static boolean simulacionActiva = true;
    volatile static int tiempoSimulado = 480;

    public static void main(String[] args) {
        new RelojMundial().start(); 
    }

    static class RelojMundial extends Thread {
        Semaphore semaforoMedicos = new Semaphore(2);
        Semaphore enfermeros = new Semaphore(2);
        Semaphore pacientesDisponibles = new Semaphore(0);
        Semaphore reloj = new Semaphore (1);

        SecretariaPacientes Secretaria = new SecretariaPacientes();

        List<Integer> Tiempos = leerTiemposDeAtencion("pacientes.txt");
        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("pacientes.txt");
        List<Paciente> pacientesAEliminar = new ArrayList<>();
        int Tiempo_Emergencia = Tiempos.get(0);
        int Tiempo_Urgencia = Tiempos.get(1);
        int Tiempo_Comunes = Tiempos.get(2);

        public void run() {
            Thread medico1 = new Hilo_Medico("Médico 1", Secretaria, semaforoMedicos, reloj , enfermeros, pacientesDisponibles , Tiempo_Comunes, Tiempo_Urgencia, Tiempo_Emergencia);
            Thread medico2 = new Hilo_Medico("Médico 2", Secretaria, semaforoMedicos, reloj , enfermeros, pacientesDisponibles ,Tiempo_Comunes, Tiempo_Urgencia, Tiempo_Emergencia);

            medico1.start();
            medico2.start();

            while (Hospital_Ucu.tiempoSimulado < 1320) {
                System.out.println("Tiempo simulado: " + Hospital_Ucu.tiempoSimulado);
                if (!listaPacientes.isEmpty()) {
                    for (Paciente p : listaPacientes) {
                        if (p.horaLlegada == tiempoSimulado) {
                            Secretaria.agregarPaciente(p);
                            pacientesDisponibles.release();
                            pacientesAEliminar.add(p);
                        }
                    }
                    listaPacientes.removeAll(pacientesAEliminar);
                    pacientesAEliminar.clear();
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Hospital_Ucu.tiempoSimulado += 1;
            }
            Hospital_Ucu.simulacionActiva = false;
            pacientesDisponibles.release(10); 
            System.out.println("Fin de la simulación");

            try {
                medico1.join();
                medico2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Paciente> sinAtender = Secretaria.obtenerPacientesNoAtendidos();
            for (Paciente p : sinAtender) {
                Hilo_Medico.pacientesNoAtendidos.add(p.nombre + " - " + p.area + " (no atendido)");
            }

            generarReporte();
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
        Set<String> nombresUnicos = new HashSet<>(); // PARA EVITAR DUPLICADOS
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 3) {
                    String nombre = partes[0].trim();
                    if (nombresUnicos.contains(nombre)) continue; // EVITAR DUPLICADO
                    nombresUnicos.add(nombre);
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    pacientes.add(new Paciente(nombre, area, horaLlegada,"", false));
                }
                else if (partes.length == 5){
                    String nombre = partes[0].trim();
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    String Especialidad = partes[3].trim();
                    Boolean Trae_Documento = Boolean.parseBoolean(partes[4].trim());

                    System.out.println(
                            "Paciente leído: " + nombre + ", Área: " + area + ", Hora de llegada: " + horaLlegada);
                    pacientes.add(new Paciente(nombre, area, horaLlegada, Especialidad , Trae_Documento));}
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
        return pacientes;
    }

    static class Paciente {
        String nombre;
        String area;
        int horaLlegada;
        String Especialidad;
        Boolean Trae_Documento;

        Paciente(String nombre, String area, int horaLlegada, String Especialidad, Boolean Trae_Documento) {
            this.nombre = nombre;
            this.area = area;
            this.horaLlegada = horaLlegada;
            this.Especialidad = Especialidad;
            this.Trae_Documento = Trae_Documento;
        }
    }

    static class SecretariaPacientes {
        Queue<Paciente> Lista_Emergencia = new LinkedList<>();
        Queue<Paciente> Lista_Urgentes = new LinkedList<>();
        Queue<Paciente> Lista_comunes = new LinkedList<>();

        public synchronized void agregarPaciente(Paciente p) {
            if (p.area.equals("Comun") && p.Especialidad.equals("Odontologo") && !p.Trae_Documento) {
                Hilo_Medico.pacientesNoAtendidos.add(p.nombre + " - Comun (Odontología sin documentación)");
                return;
            }
            switch (p.area) {
                case "Emergencia" -> Lista_Emergencia.add(p);
                case "Urgente" -> Lista_Urgentes.add(p);
                default -> Lista_comunes.add(p);
            }
        }


        public synchronized Paciente obtenerSiguientePaciente() {
            if (!Lista_Emergencia.isEmpty()) return Lista_Emergencia.poll();
            if (!Lista_Urgentes.isEmpty()) return Lista_Urgentes.poll();
            if (!Lista_comunes.isEmpty()) return Lista_comunes.poll();
            return null;
        }
        public synchronized boolean isVacia() {
            return Lista_Emergencia.isEmpty() && Lista_Urgentes.isEmpty() && Lista_comunes.isEmpty();
        }

        public synchronized List<Paciente> obtenerPacientesNoAtendidos() {
        List<Paciente> pendientes = new ArrayList<>();
        for (Paciente p : Lista_Emergencia) {
            pendientes.add(p);
        }
        for (Paciente p : Lista_Urgentes) {
            pendientes.add(p);
        }
        for (Paciente p : Lista_comunes) {
            pendientes.add(p);
        }
        return pendientes;
    }

    }

    static class Hilo_Medico extends Thread {
        String nombreMedico;
        SecretariaPacientes secretaria;
        Semaphore semaforoMedicos;
        Semaphore enfermeros;
        Semaphore reloj;

        private boolean Paciente_Con_Emergencia = false;
        private boolean paciente_en_Sala = false;
        private Paciente p = null;
        private Paciente pEmergencia = null;

        int Tiempo_Comunes;
        int Tiempo_Urgencia;
        int Tiempo_Emergencia;

        Paciente pacienteInterrumpido = null; 
        int tiempoRestanteInterrumpido = 0;    
        int tiempoInicioEmergencia = 0;
        int totalPacientes = 0;
        int tiempoActualDeAtencion = 0;
        int tiempoDeEmergencia = 0;

        static Set<String> pacientesAtendidos = Collections.synchronizedSet(new HashSet<>());
        static List<String> logAtenciones = Collections.synchronizedList(new ArrayList<>());
        static int comunes = 0, urgentes = 0, emergencias = 0;
        static int esperaComunes = 0, esperaUrgentes = 0, esperaEmergencias = 0;

        static List<String> pacientesNoAtendidos = Collections.synchronizedList(new ArrayList<>()); 

        public Hilo_Medico(String nombre, SecretariaPacientes secretaria, Semaphore semaforoMedicos, Semaphore reloj,
                           Semaphore enfermeros, Semaphore pacientesDisponibles,
                           int Tiempo_Comunes, int Tiempo_Urgencia, int Tiempo_Emergencia) {
            this.nombreMedico = nombre;
            this.secretaria = secretaria;
            this.semaforoMedicos = semaforoMedicos;
            this.enfermeros = enfermeros;
            this.reloj = reloj;
            this.Tiempo_Comunes = Tiempo_Comunes;
            this.Tiempo_Urgencia = Tiempo_Urgencia;
            this.Tiempo_Emergencia = Tiempo_Emergencia;
        }

        int tiempoInicioAtencion = 0; 

        @Override
        public void run() {
            while (Hospital_Ucu.simulacionActiva || !secretaria.isVacia()) {
                try {
                    if (Paciente_Con_Emergencia) {
                        tiempoDeEmergencia--;
                        System.out.println("[" + nombreMedico + "] Atendiendo emergencia: " + pEmergencia.nombre + " - Tiempo restante: " + tiempoDeEmergencia);
                        if (tiempoDeEmergencia == 0) {
                            enfermeros.release();
                            int espera = tiempoInicioEmergencia - pEmergencia.horaLlegada;
                            esperaEmergencias += espera;
                            emergencias++;
                            totalPacientes++;
                            Paciente_Con_Emergencia = false;
                            logAtenciones.add(pEmergencia.nombre + " - Emergencia | Llegó: " + pEmergencia.horaLlegada +
                                    " | Salió: " + Hospital_Ucu.tiempoSimulado + " | Espera: " + espera + " min");
                            if (pacienteInterrumpido != null) {
                                p = pacienteInterrumpido;
                                tiempoActualDeAtencion = tiempoRestanteInterrumpido;
                                paciente_en_Sala = true;
                                pacienteInterrumpido = null;
                                tiempoRestanteInterrumpido = 0;
                                System.out.println("[" + nombreMedico + "] Reanudando atención de: " + p.nombre + " - " + p.area + " - Tiempo restante: " + tiempoActualDeAtencion);
                            }
                        }
                    } else if (!secretaria.Lista_Emergencia.isEmpty()) {
                        pEmergencia = secretaria.Lista_Emergencia.poll();
                        enfermeros.acquire();
                        if (paciente_en_Sala) {
                            pacienteInterrumpido = p;
                            tiempoRestanteInterrumpido = tiempoActualDeAtencion;
                            paciente_en_Sala = false;
                            System.out.println("[" + nombreMedico + "] Interrumpiendo atención de: " + pacienteInterrumpido.nombre + " - " + pacienteInterrumpido.area + " - Tiempo restante guardado: " + tiempoRestanteInterrumpido);
                        }
                        tiempoDeEmergencia = Tiempo_Emergencia;
                        tiempoInicioEmergencia = Hospital_Ucu.tiempoSimulado;
                        Paciente_Con_Emergencia = true;
                    } else if (paciente_en_Sala) {
                        tiempoActualDeAtencion--;
                        System.out.println("[" + nombreMedico + "] Atendiendo: " + p.nombre + " - " + p.area + " - Tiempo restante: " + tiempoActualDeAtencion);
                        if (tiempoActualDeAtencion == 0) {
                            enfermeros.release();
                            int espera = tiempoInicioAtencion - p.horaLlegada;
                            switch (p.area) {
                                case "Urgente" -> {
                                    urgentes++;
                                    esperaUrgentes += espera;
                                }
                                case "Comun" -> {
                                    comunes++;
                                    esperaComunes += espera;
                                }
                            }
                            totalPacientes++;
                            paciente_en_Sala = false;
                            logAtenciones.add(p.nombre + " - " + p.area + " | Llegó: " + p.horaLlegada +
                                    " | Salió: " + Hospital_Ucu.tiempoSimulado + " | Espera: " + espera + " min");
                        }
                    } else if (!secretaria.Lista_Urgentes.isEmpty() || !secretaria.Lista_comunes.isEmpty()) {
                        p = secretaria.obtenerSiguientePaciente();
                        enfermeros.acquire();
                        paciente_en_Sala = true;
                        tiempoInicioAtencion = Hospital_Ucu.tiempoSimulado;
                        switch (p.area) {
                            case "Urgente" -> tiempoActualDeAtencion = Tiempo_Urgencia;
                            case "Comun" -> tiempoActualDeAtencion = Tiempo_Comunes;
                        }
                        System.out.println("[" + nombreMedico + "] Comienza atención: " + p.nombre + " - " + p.area + " - Tiempo: " + tiempoActualDeAtencion);
                    }
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (Paciente_Con_Emergencia && tiempoDeEmergencia > 0) {
                pacientesNoAtendidos.add(pEmergencia.nombre + " - Emergencia (en atención inconclusa)");
            }
            if (paciente_en_Sala) {
                pacientesNoAtendidos.add(p.nombre + " - " + p.area + " (en atención inconclusa)");
            }
        }
    }

    static String formatoHora(int tiempo) {
        int hora = tiempo / 60;
        int minutos = tiempo % 60;
        return String.format("%02d:%02d", hora, minutos);
    }

    public static void generarReporte() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("reporte.txt"))) {
            bw.write("===== Reporte de Atención Médica =====\n\n");
            Set<String> logsUnicos = new LinkedHashSet<>(Hilo_Medico.logAtenciones); // mantiene orden y evita duplicados
            for (String log : logsUnicos) {
                String[] partes = log.split("\\|");
                String[] datos = partes[0].trim().split(" - ");
                String nombre = datos[0].trim();
                String tipo = datos[1].trim();
                int llegada = Integer.parseInt(partes[1].replace("Llegó:", "").trim());
                int salida = Integer.parseInt(partes[2].replace("Salió:", "").trim());
                String espera = partes[3].replace("Espera:", "").trim();

                bw.write(nombre + " - " + tipo + " | Llegó: " + formatoHora(llegada) + " | Salió: " + formatoHora(salida) + " | Espera: " + espera + "\n");
            }
            bw.newLine();
            bw.write("Totales:\n");
            bw.write("  Comunes: " + Hilo_Medico.comunes + "\n");
            bw.write("  Urgentes: " + Hilo_Medico.urgentes + "\n");
            bw.write("  Emergencias: " + Hilo_Medico.emergencias + "\n\n");

            bw.write("Tiempo total de espera por tipo:\n");
            bw.write("  Comunes: " + Hilo_Medico.esperaComunes + " minutos\n");
            bw.write("  Urgentes: " + Hilo_Medico.esperaUrgentes + " minutos\n");
            bw.write("  Emergencias: " + Hilo_Medico.esperaEmergencias + " minutos\n");

            bw.write("\nTiempo promedio de espera por tipo:\n");
            bw.write("  Comunes: " + promedio(Hilo_Medico.esperaComunes, Hilo_Medico.comunes) + " minutos\n");
            bw.write("  Urgentes: " + promedio(Hilo_Medico.esperaUrgentes, Hilo_Medico.urgentes) + " minutos\n");
            bw.write("  Emergencias: " + promedio(Hilo_Medico.esperaEmergencias, Hilo_Medico.emergencias) + " minutos\n");

            bw.write("\nPacientes no atendidos:\n");
            if (Hilo_Medico.pacientesNoAtendidos.isEmpty()) {
                bw.write("  Ninguno\n");
            } else {
                for (String paciente : Hilo_Medico.pacientesNoAtendidos) {
                    bw.write("  " + paciente + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error escribiendo el archivo de reporte: " + e.getMessage());
        }
    }

    private static int promedio(int total, int cantidad) {
        return (cantidad == 0) ? 0 : total / cantidad;
    }
}
