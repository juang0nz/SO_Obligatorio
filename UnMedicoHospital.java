import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class UnMedicoHospital {
    volatile static boolean simulacionActiva = true;
    volatile static int tiempoSimulado = 480; // Tiempo simulado en minutos (8 horas)
    volatile static int TiempoDeAtecionParaEmergencia = 0;
    volatile static int TiempoDeAtecionParaUrgencia = 0;
    volatile static int TiempoDeAtecionParaComun = 0;
    static TiempoAtencion tiempoAtencion;
    volatile static int TiempoFinalDeSimulacion = 1380; // Tiempo final de simulación en minutos (23 horas)
    volatile static int Tiempo_Emergencia;
    volatile static int Tiempo_Urgencia;
    volatile static int Tiempo_Comunes;
    static int tiempoMaximoParaEmergencia = 10; // Tiempo máximo para atender una emergencia
    static int tiempoMaximoParaUrgencia = 120; // Tiempo máximo para atender una urgencia

    public static void main(String[] args) {
        Semaphore medico = new Semaphore(0);
        Semaphore reloj = new Semaphore(1);
        SecretariaPacientes Secretaria = new SecretariaPacientes();
        Semaphore enfermero = new Semaphore(1);
        List<Integer> Tiempos = leerTiemposDeAtencion("Pacientes_Un_Medico.txt");
        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("Pacientes_Un_Medico.txt");
        Tiempo_Emergencia = Tiempos.get(0);
        Tiempo_Urgencia = Tiempos.get(1);
        Tiempo_Comunes = Tiempos.get(2);
        tiempoAtencion = new UnMedicoHospital.TiempoAtencion(Tiempo_Emergencia, Tiempo_Urgencia, Tiempo_Comunes);
        new Hilo_Medico(Secretaria, medico, reloj, enfermero, Tiempo_Comunes, Tiempo_Urgencia, Tiempo_Emergencia).start();
        while (UnMedicoHospital.tiempoSimulado <= UnMedicoHospital.TiempoFinalDeSimulacion) {
                try {
                    reloj.acquire();
                    System.out.println("Tiempo Simulado: " + String.format("%02d:%02d", tiempoSimulado / 60, tiempoSimulado % 60));
                    if (!listaPacientes.isEmpty()) {
                        for (int i = 0; i < listaPacientes.size(); i++) {
                            Paciente p = listaPacientes.get(i);
                            if (p.horaLlegada == tiempoSimulado) {
                                Secretaria.agregarPaciente(p);
                                listaPacientes.remove(p);
                            }
                        }
                    }
                    medico.release(); // Libera el semáforo del médico para que pueda atender pacientes
                    UnMedicoHospital.tiempoSimulado += 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        UnMedicoHospital.simulacionActiva = false;
        System.out.println("Simulación finalizada. Revisa el archivo reporte.txt para ver los resultados.");
    }

    public static class TiempoAtencion {
        // Atributos privados
        private int Tiempo_Emergencia;
        private int Tiempo_Urgencia;
        private int Tiempo_Comunes;

        // Constructor para inicializar los tiempos
        public TiempoAtencion(int Tiempo_Emergencia, int Tiempo_Urgencia, int Tiempo_Comunes) {
            this.Tiempo_Emergencia = Tiempo_Emergencia;
            this.Tiempo_Urgencia = Tiempo_Urgencia;
            this.Tiempo_Comunes = Tiempo_Comunes;
        }

        // Métodos públicos para agregar o restar tiempos dependiendo del área
        public void agregarTiempoAtencion(String area) {
            switch (area) {
                case "Emergencia":
                    UnMedicoHospital.TiempoDeAtecionParaEmergencia += Tiempo_Emergencia ; // Solo incrementa el tiempo de emergencia
                    break;
                case "Urgente":
                    UnMedicoHospital.TiempoDeAtecionParaUrgencia += (UnMedicoHospital.TiempoDeAtecionParaEmergencia + Tiempo_Urgencia); // Incrementa el tiempo de urgencia
                    break;
                case "Comun":
                    UnMedicoHospital.TiempoDeAtecionParaComun += (UnMedicoHospital.TiempoDeAtecionParaUrgencia + UnMedicoHospital.TiempoDeAtecionParaEmergencia +  Tiempo_Comunes); // Incrementa el tiempo de común
                    break;
                default:
                    System.out.println("Área no reconocida");
            }
        }

        public void restarTiempoAtencion(String area) {
            switch (area) {
                case "Emergencia":
                    UnMedicoHospital.TiempoDeAtecionParaEmergencia -= 1; // Decrementa el tiempo de emergencia
                    UnMedicoHospital.TiempoDeAtecionParaUrgencia -= 1;
                    UnMedicoHospital.TiempoDeAtecionParaComun -= 1;
                    break;
                case "Urgente" :
                    UnMedicoHospital.TiempoDeAtecionParaUrgencia -= 1; // Decrementa el tiempo de urgencia
                    UnMedicoHospital.TiempoDeAtecionParaComun -= 1;
                    break;
                case "Comun":
                    UnMedicoHospital.TiempoDeAtecionParaComun -= 1; // Decrementa el tiempo de común
                    UnMedicoHospital.TiempoDeAtecionParaUrgencia -= 1;
                    break;
                default:
                    System.out.println("Área no reconocida");
            }
        }

        // Métodos getter para acceder a los tiempos
        public int getTiempoEmergencia() {
            return Tiempo_Emergencia;
        }

        public int getTiempoUrgencia() {
            return Tiempo_Urgencia;
        }

        public int getTiempoComun() {
            return Tiempo_Comunes;
        }
    }

    // Método para leer los tiempos de atención desde un archivo
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
                    } else if (Especialidad.equals("Urgente")) {
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

    static List<Paciente> leerPacientesDesdeArchivo(String nombreArchivo) {     // Método para leer pacientes desde un archivo
        List<Paciente> pacientes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 3) {

                    String nombre = partes[0].trim();
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    pacientes.add(new Paciente(nombre, area, horaLlegada, null , null));

                }else if (partes.length == 5){

                    String nombre = partes[0].trim();
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    String Especialidad = partes[3].trim();
                    Boolean Trae_Documento = Boolean.parseBoolean(partes[4].trim());
                    pacientes.add(new Paciente(nombre, area, horaLlegada, Especialidad , Trae_Documento));
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
        return pacientes;
    }

    static class Paciente { // Paciente con nombre y prioridad
        String nombre;
        String area; // Área de atención (Urgente, Común, Emergencia)
        int horaLlegada;
        String horaFormaString;
        String Especialidad; // Especialidad del paciente (Odontologo, Carnet de Salud, etc.)
        Boolean Trae_Documento; // Indica si trae documento o no

        Paciente(String nombre, String area, int horaLlegada, String Especialidad, Boolean Trae_Documento ) {
            this.nombre = nombre;
            this.area = area;
            this.horaLlegada = horaLlegada;
            this.Especialidad = Especialidad;
            this.Trae_Documento = Trae_Documento;
            this.horaFormaString = String.format("%02d:%02d", horaLlegada / 60, horaLlegada % 60);
        }
    }

    static class SecretariaPacientes {     // Maneja las listas de pacientes
        Queue<Paciente> Lista_Urgentes = new LinkedList<>(); // Lista de pacientes urgentes
        Queue<Paciente> Lista_comunes = new LinkedList<>(); // Lista de pacientes comunes
        Queue<Paciente> Lista_Emergencia = new LinkedList<>(); // Lista de pacientes de emergencia
        List<String> logNoAtenciones = new ArrayList<>(); // Lista para almacenar los registros de atención

        public synchronized void agregarPaciente(Paciente p) {
            if (p == null || p.area == null) {
                System.out.println("Paciente inválido.");
                return;
            }

            switch (p.area) {
                case "Emergencia":
                if (UnMedicoHospital.TiempoDeAtecionParaEmergencia <= (UnMedicoHospital.TiempoFinalDeSimulacion - UnMedicoHospital.tiempoSimulado)) {
                    if(UnMedicoHospital.TiempoDeAtecionParaEmergencia <= UnMedicoHospital.tiempoMaximoParaEmergencia) {
                        Lista_Emergencia.add(p);
                        tiempoAtencion.agregarTiempoAtencion(p.area);
                        break;

                    } else {
                        logNoAtenciones.add("  ·Paciente de emergencia no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString  + " - Tiempo de atención insuficiente");
                        break;
                    }
                } else {
                    logNoAtenciones.add("  ·Paciente de emergencia no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - Tiempo de atención insuficiente");
                    break;
                }
                case "Urgente":
                if (UnMedicoHospital.TiempoDeAtecionParaUrgencia <= (UnMedicoHospital.TiempoFinalDeSimulacion - UnMedicoHospital.tiempoSimulado)){
                    if (UnMedicoHospital.TiempoDeAtecionParaUrgencia <= UnMedicoHospital.tiempoMaximoParaUrgencia) {
                        Lista_Urgentes.add(p);
                        tiempoAtencion.agregarTiempoAtencion(p.area);
                        break;
                    } else {
                        logNoAtenciones.add("  ·Paciente urgente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - Tiempo de atención insuficiente");
                        break;
                    }
                }else {
                    logNoAtenciones.add("  ·Paciente urgente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - Tiempo de atención insuficiente");
                    break;
                }

                case "Comun":
                if (UnMedicoHospital.TiempoDeAtecionParaComun <= (UnMedicoHospital.TiempoFinalDeSimulacion - UnMedicoHospital.tiempoSimulado)) {
                    if ("Odontologo".equalsIgnoreCase(p.Especialidad) || "CarnetDeSalud".equalsIgnoreCase(p.Especialidad)) {
                        if (p.Trae_Documento) {
                            Lista_comunes.add(p);
                            tiempoAtencion.agregarTiempoAtencion(p.area);
                        } else {
                            logNoAtenciones.add("  ·Paciente " + p.Especialidad + " no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - No trae documento");
                        }
                    } else {
                        Lista_comunes.add(p);
                        System.out.println("Paciente común ingresado: " + p.nombre);
                    }
                    break;
                } else {
                    logNoAtenciones.add("  ·Paciente común no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - Tiempo de atención insuficiente");
                    break;
                }
                default:
                    logNoAtenciones.add("  ·Paciente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaFormaString + " - Área desconocida");
                }
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

    // Hilo Médico
    static class Hilo_Medico extends Thread {
        SecretariaPacientes Secretaria;
        Semaphore medico;
        Semaphore reloj;
        Semaphore enfermero;
        private boolean Paciente_Con_Emergencia = false;
        private boolean paciente_en_Sala = false; // Indica si hay un paciente en la sala de espera
        private Paciente p = null; // Paciente actual en la sala de espera;
        private Paciente pEmergencia = null; // Paciente de emergencia actual

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

        public Hilo_Medico(SecretariaPacientes Secretaria, Semaphore medico, Semaphore reloj, Semaphore enfermero, int Tiempo_Comunes,
                int Tiempo_Urgencia, int Tiempo_Emergencia) {
            this.Secretaria = Secretaria;
            this.medico = medico;
            this.reloj = reloj;
            this.enfermero = enfermero;
            this.Tiempo_Comunes = Tiempo_Comunes;
            this.Tiempo_Urgencia = Tiempo_Urgencia;
            this.Tiempo_Emergencia = Tiempo_Emergencia;
        }

        @Override
        public synchronized void run() {
            while (UnMedicoHospital.simulacionActiva) {
                try {
                    enfermero.acquire();
                    medico.acquire();
                    if (Paciente_Con_Emergencia) {
                        tiempoDeEmergencia --;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        if (tiempoDeEmergencia == 0) {
                            totalPacientes++;
                            emergencias++;
                            Paciente_Con_Emergencia = false;
                            logAtenciones.add("  ·Paciente atendido: " + pEmergencia.nombre + " - Emergencia | Ingreso Al Hospital: " + pEmergencia.horaFormaString + " | Salio Del Hospital: " + String.format("%02d:%02d", tiempoSimulado / 60, tiempoSimulado % 60));
                        }
                        enfermero.release();
                        reloj.release();
                    } else if (!Secretaria.Lista_Emergencia.isEmpty()) {
                        pEmergencia = Secretaria.Lista_Emergencia.poll();
                        Paciente_Con_Emergencia = true;
                        tiempoDeEmergencia = Tiempo_Emergencia - 1;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        tiempoTotalDeAtencion += Tiempo_Emergencia;
                        enfermero.release();
                        reloj.release();
                    } else if (paciente_en_Sala) {
                        tiempoActualDeAtencion --;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        if (tiempoActualDeAtencion <= 0) {
                            paciente_en_Sala = false;
                            totalPacientes++;
                            if (p.area.equals("Urgente")) {
                                urgentes++;
                                tiempoTotalDeAtencion += Tiempo_Urgencia;
                            } else if (p.area.equals("Comun")) {
                                comunes++;
                                tiempoTotalDeAtencion += Tiempo_Comunes;
                            }
                            logAtenciones.add("  ·Paciente atendido: " +p.nombre + " - " + p.area + " | Ingreso Al Hospital: " + p.horaFormaString + " | Salió Del Hospital: " + String.format("%02d:%02d", tiempoSimulado / 60, tiempoSimulado % 60));
                        }
                        enfermero.release();
                        reloj.release();
                    } else if (!Secretaria.Lista_Urgentes.isEmpty() || !Secretaria.Lista_comunes.isEmpty()) {
                        p = Secretaria.obtenerSiguientePaciente();
                        if (p.area.equals("Urgente")) {
                            tiempoActualDeAtencion = Tiempo_Urgencia-1;
                            tiempoAtencion.restarTiempoAtencion(p.area);
                        } else if (p.area.equals("Comun")) {
                            tiempoActualDeAtencion = Tiempo_Comunes-1;
                            tiempoAtencion.restarTiempoAtencion(p.area);
                        }
                        paciente_en_Sala = true;
                        enfermero.release();
                        reloj.release();
                    } else {
                        enfermero.release();
                        reloj.release();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try (
                FileWriter fw = new FileWriter("Reporte_De_Hospital_De_Un_Medico.txt")) {
                    fw.write("Reporte Final \n");
                    fw.write(" ·Total pacientes atendidos: " + totalPacientes + "\n");
                    fw.write(" ·Emergencias : " + emergencias + "\n");
                    fw.write(" ·Urgentes: " + urgentes + "\n");
                    fw.write(" ·Comunes : " + comunes + "\n");
                    fw.write(" ·Tiempo Total de Atención : " + tiempoTotalDeAtencion + "\n");
                    fw.write("\n");

                    fw.write("Pacientes atendidos:\n");
                    if (logAtenciones.isEmpty()) {
                        fw.write("  ·No hay pacientes atendidos.\n");
                    } else {
                        for (String log : logAtenciones) {
                            fw.write(log + "\n");
                        }
                    }
                    fw.write("\n");

                    fw.write("Paciente en la sala que no fue terminado de atender.\n");
                    if(paciente_en_Sala) {
                        fw.write("  ·Paciente en sala de espera: " + p.nombre + " - Área: " + p.area + " - Hora de llegada: " + p.horaFormaString + "\n");
                    }else {
                        fw.write("  ·No hay paciente que estuviera siendo atendido y no terminara.\n");
                    }
                    fw.write( "\n");

                    fw.write("Pacientes no atendidos en lista de Espera:\n");
                    if (Secretaria.Lista_Emergencia.isEmpty() && Secretaria.Lista_Urgentes.isEmpty() && Secretaria.Lista_comunes.isEmpty()) {
                        fw.write("  ·No hay pacientes en lista de espera.\n");
                    } else {
                        for (Paciente paciente : Secretaria.Lista_Emergencia) {
                            fw.write("  ·Paciente de emergencia no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaFormaString + "\n");
                        }
                        for (Paciente paciente : Secretaria.Lista_Urgentes) {
                            fw.write("  ·Paciente urgente no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaFormaString + "\n");
                        }
                        for (Paciente paciente : Secretaria.Lista_comunes) {
                            fw.write("  ·Paciente común no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaFormaString + "\n");
                        }
                    }
                    fw .write("\n");

                    fw.write("Pacientes que llegaron y no fueron registrados en lista:\n");
                    if (Secretaria.logNoAtenciones.isEmpty()) {
                        fw.write("  ·No hay personas que llegaran y no se les diera un lugar en lista de espera.\n");
                    }else {
                        for (String log : Secretaria.logNoAtenciones) {
                            fw.write(log + "\n");
                        }
                    }
                    fw .write("\n");
                    
                    fw.write("Fin del Reporte \n");
                    System.out.println("Reporte escrito en reporte.txt");
            } catch (IOException e) {
                System.err.println("Error escribiendo el reporte: " + e.getMessage());
            }
        }

    }
}