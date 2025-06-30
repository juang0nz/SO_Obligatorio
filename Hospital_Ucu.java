import java.io.BufferedReader; 
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Hospital_Ucu {
    volatile static boolean simulacionActiva = true;
    volatile static int tiempoSimulado = 800;
    volatile static int TiempoDeAtecionParaEmergencia = 0;
    volatile static int TiempoDeAtecionParaUrgencia = 0;
    volatile static int TiempoDeAtecionParaComun = 0;
    static TiempoAtencion tiempoAtencion;
    volatile static int TiempoFinalDeSimulacion = 1100;

    public static void main(String[] args) {
        Semaphore medico = new Semaphore(0);
        Semaphore reloj = new Semaphore (1);
        SecretariaPacientes Secretaria = new SecretariaPacientes();

        List<Integer> Tiempos = leerTiemposDeAtencion("pacientes.txt");
        List<Paciente> listaPacientes = leerPacientesDesdeArchivo("pacientes.txt");
        int Tiempo_Emergencia = Tiempos.get(0);
        int Tiempo_Urgencia = Tiempos.get(1);
        int Tiempo_Comunes = Tiempos.get(2);
        tiempoAtencion = new Hospital_Ucu.TiempoAtencion(Tiempo_Emergencia, Tiempo_Urgencia, Tiempo_Comunes);
        new Hilo_Medico(Secretaria, medico, reloj, Tiempo_Comunes, Tiempo_Urgencia, Tiempo_Emergencia).start();
        while (Hospital_Ucu.tiempoSimulado < Hospital_Ucu.TiempoFinalDeSimulacion) { // Simulación por 60 minutos
                System.out.println("Tiempo simulado: " + Hospital_Ucu.tiempoSimulado + " minutos");
                try {
                    reloj.acquire();
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
                    Hospital_Ucu.tiempoSimulado += 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        Hospital_Ucu.simulacionActiva = false;
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
                    Hospital_Ucu.TiempoDeAtecionParaEmergencia += Tiempo_Emergencia ; // Solo incrementa el tiempo de emergencia
                    break;
                case "Urgente":
                    Hospital_Ucu.TiempoDeAtecionParaUrgencia += (Hospital_Ucu.TiempoDeAtecionParaEmergencia + Tiempo_Urgencia); // Incrementa el tiempo de urgencia
                    break;
                case "Comun":
                    Hospital_Ucu.TiempoDeAtecionParaComun += (Hospital_Ucu.TiempoDeAtecionParaUrgencia + Hospital_Ucu.TiempoDeAtecionParaEmergencia +  Tiempo_Comunes); // Incrementa el tiempo de común
                    break;
                default:
                    System.out.println("Área no reconocida");
            }
        }

        public void restarTiempoAtencion(String area) {
            switch (area) {
                case "Emergencia":
                    Hospital_Ucu.TiempoDeAtecionParaEmergencia -= 1; // Decrementa el tiempo de emergencia
                    Hospital_Ucu.TiempoDeAtecionParaUrgencia -= 1;
                    Hospital_Ucu.TiempoDeAtecionParaComun -= 1;
                    break;
                case "Urgente" :
                    Hospital_Ucu.TiempoDeAtecionParaUrgencia -= 1; // Decrementa el tiempo de urgencia
                    Hospital_Ucu.TiempoDeAtecionParaComun -= 1;
                    break;
                case "Comun":
                    Hospital_Ucu.TiempoDeAtecionParaComun -= 1; // Decrementa el tiempo de común
                    Hospital_Ucu.TiempoDeAtecionParaUrgencia -= 1;
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
                    System.out.println("Paciente leído: " + nombre + ", Área: " + area + ", Hora de llegada: " + horaLlegada);
                    pacientes.add(new Paciente(nombre, area, horaLlegada, null , null));

                }else if (partes.length == 5){

                    String nombre = partes[0].trim();
                    String area = partes[1].trim();
                    int horaLlegada = Integer.parseInt(partes[2].trim());
                    String Especialidad = partes[3].trim();
                    Boolean Trae_Documento = Boolean.parseBoolean(partes[4].trim());

                    System.out.println("Paciente leído: " + nombre + ", Área: " + area + ", Hora de llegada: " + horaLlegada);
                    pacientes.add(new Paciente(nombre, area, horaLlegada, Especialidad , Trae_Documento));
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
        String Especialidad; // Especialidad del paciente (Odontologo, Carnet de Salud, etc.)
        Boolean Trae_Documento; // Indicates if brings the necessary documentation

        Paciente(String nombre, String area, int horaLlegada, String Especialidad, Boolean Trae_Documento ) {
            this.nombre = nombre;
            this.area = area;
            this.horaLlegada = horaLlegada;
            this.Especialidad = Especialidad;
            this.Trae_Documento = Trae_Documento;
        }
    }

    // Maneja las listas de pacientes
    static class SecretariaPacientes {
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
                if (Hospital_Ucu.TiempoDeAtecionParaEmergencia < (Hospital_Ucu.TiempoFinalDeSimulacion - Hospital_Ucu.tiempoSimulado)) {
                    if(Hospital_Ucu.TiempoDeAtecionParaEmergencia < 10) {
                        Lista_Emergencia.add(p);
                        System.out.println("Paciente de emergencia ingresado: " + p.nombre);
                        tiempoAtencion.agregarTiempoAtencion(p.area);
                        break;

                    } else {
                        System.out.println("Lo sentimos, el tiempo de atención para Emergencia es insuficiente.");
                        logNoAtenciones.add("  ·Paciente de emergencia no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Tiempo de atención insuficiente");
                        break;
                    }
                } else {
                    System.out.println("Lo sentimos, el tiempo de atención para Emergencia es insuficiente.");
                    logNoAtenciones.add("  ·Paciente de emergencia no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Tiempo de atención insuficiente");
                    break;
                }
                case "Urgente":
                if (Hospital_Ucu.TiempoDeAtecionParaUrgencia < (Hospital_Ucu.TiempoFinalDeSimulacion - Hospital_Ucu.tiempoSimulado)){
                    if (Hospital_Ucu.TiempoDeAtecionParaUrgencia < 200) {
                        Lista_Urgentes.add(p);
                        System.out.println("Paciente urgente ingresado: " + p.nombre);
                        tiempoAtencion.agregarTiempoAtencion(p.area);
                        break;
                    } else {
                        System.out.println("Lo sentimos, el tiempo de atención para Urgencia es insuficiente.");
                        logNoAtenciones.add("  ·Paciente urgente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Tiempo de atención insuficiente");
                        break;
                    }
                }else {
                    System.out.println("Lo sentimos, el tiempo de atención para Urgencia es insuficiente.");
                    logNoAtenciones.add("  ·Paciente urgente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Tiempo de atención insuficiente");
                    break;
                }

                case "Comun":
                if (Hospital_Ucu.TiempoDeAtecionParaComun < (Hospital_Ucu.TiempoFinalDeSimulacion - Hospital_Ucu.tiempoSimulado)) {
                    if ("Odontologo".equalsIgnoreCase(p.Especialidad) || "CarnetDeSalud".equalsIgnoreCase(p.Especialidad)) {
                        if (p.Trae_Documento) {
                            Lista_comunes.add(p);
                            System.out.println("Paciente por " + p.Especialidad + " ingresado: " + p.nombre);
                            tiempoAtencion.agregarTiempoAtencion(p.area);
                        } else {
                            System.out.println("Lo sentimos, si no trae la documentación no puede ser atendido.");
                            logNoAtenciones.add("  ·Paciente " + p.Especialidad + " no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - No trae documento");
                        }
                    } else {
                        Lista_comunes.add(p);
                        System.out.println("Paciente común ingresado: " + p.nombre);
                    }
                    break;
                } else {
                    System.out.println("Lo sentimos, el tiempo de atención para Común es insuficiente.");
                    logNoAtenciones.add("  ·Paciente común no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Tiempo de atención insuficiente");
                    break;
                }
                default:
                    System.out.println("Área desconocida: " + p.area);
                    logNoAtenciones.add("  ·Paciente no atendido: " + p.nombre + " - Hora de llegada: " + p.horaLlegada + " - Área desconocida");
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

        public Hilo_Medico(SecretariaPacientes Secretaria, Semaphore medico, Semaphore reloj, int Tiempo_Comunes,
                int Tiempo_Urgencia, int Tiempo_Emergencia) {
            this.Secretaria = Secretaria;
            this.medico = medico;
            this.reloj = reloj;
            this.Tiempo_Comunes = Tiempo_Comunes;
            this.Tiempo_Urgencia = Tiempo_Urgencia;
            this.Tiempo_Emergencia = Tiempo_Emergencia;
        }

        @Override
        public synchronized void run() {
            System.out.println("Tiempoo de atención para Emergencia: " + Tiempo_Emergencia);
            System.out.println("Tiempoo de atención para Urgencia: " + Tiempo_Urgencia);
            System.out.println("Tiempoo de atención para Comunes: " + Tiempo_Comunes);
            while (Hospital_Ucu.simulacionActiva) {
                try {
                    medico.acquire();
                    if (Paciente_Con_Emergencia) {
                        tiempoDeEmergencia --;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        System.out.println("Atendiendo paciente de emergencia: " + pEmergencia.nombre + " - Tiempo restante: " + tiempoDeEmergencia);
                        if (tiempoDeEmergencia == 0) {
                            totalPacientes++;
                            emergencias++;
                            Paciente_Con_Emergencia = false;
                            logAtenciones.add("  ·Paciente atendido: " + pEmergencia.nombre + " - Emergencia | Ingreso Al Hospital: " + pEmergencia.horaLlegada + " | Salio Del Hospital: " + Hospital_Ucu.tiempoSimulado);
                        }
                        sleep(50);
                        reloj.release();
                    } else if (!Secretaria.Lista_Emergencia.isEmpty()) {
                        pEmergencia = Secretaria.Lista_Emergencia.poll();
                        Paciente_Con_Emergencia = true;
                        tiempoDeEmergencia = Tiempo_Emergencia - 1;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        tiempoTotalDeAtencion += Tiempo_Emergencia;
                        sleep(50);
                        reloj.release();
                    } else if (paciente_en_Sala) {
                        tiempoActualDeAtencion --;
                        tiempoAtencion.restarTiempoAtencion(p.area);
                        System.out.println("Estoy atendiendo paciente: " + p.nombre + " - " + p.area+ " - Tiempo restante: " + tiempoActualDeAtencion);
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
                            System.out.println("Paciente atendido: " + p.nombre + " - " + p.area + " - Tiempo total de atención: " + tiempoActualDeAtencion);
                            logAtenciones.add("  ·Paciente atendido: " +p.nombre + " - " + p.area + " | Ingreso Al Hospital: " + p.horaLlegada + " | Salió Del Hospital: " + Hospital_Ucu.tiempoSimulado);
                        }
                        sleep(50);
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
                        System.out.println("Atendiendo paciente: " + p.nombre + " - " + p.area + " - Tiempo restante: " + tiempoActualDeAtencion);
                        paciente_en_Sala = true;
                        sleep(50);
                        reloj.release();
                    } else {
                        System.out.println("No hay pacientes en espera. Médico esperando...");
                        sleep(50);
                        reloj.release();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try (
                    FileWriter fw = new FileWriter("reporte.txt")) {
                fw.write("Reporte Final \n");
                fw.write(" ·Total pacientes atendidos: " + totalPacientes + "\n");
                fw.write(" ·Emergencias : " + emergencias + "\n");
                fw.write(" ·Urgentes: " + urgentes + "\n");
                fw.write(" ·Comunes : " + comunes + "\n");
                fw.write(" ·Tiempo Total de Atención : " + tiempoTotalDeAtencion + "\n");
                fw.write("\n");


                fw.write("Pacientes atendidos:\n");
                for (String log : logAtenciones) {
                    fw.write(log + "\n");
                }
                fw.write("\n");


                fw.write("Paciente en la sala que no fue terminado de atender.\n");
                if(paciente_en_Sala) {
                    fw.write("  ·Paciente en sala de espera: " + p.nombre + " - Área: " + p.area + " - Hora de llegada: " + p.horaLlegada + "\n");
                }else {
                    fw.write("  ·No hay paciente que estuviera siendo atendido y no terminara.\n");
                }
                fw.write( "\n");


                fw.write("Pacientes no atendidos en lista de Espera:\n");
                if (Secretaria.Lista_Emergencia.isEmpty() && Secretaria.Lista_Urgentes.isEmpty() && Secretaria.Lista_comunes.isEmpty()) {
                    fw.write("  ·No hay pacientes en lista de espera.\n");
                } else {
                    for (Paciente paciente : Secretaria.Lista_Emergencia) {
                        fw.write("  ·Paciente de emergencia no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaLlegada + "\n");
                    }
                    for (Paciente paciente : Secretaria.Lista_Urgentes) {
                        fw.write("  ·Paciente urgente no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaLlegada + "\n");
                    }
                    for (Paciente paciente : Secretaria.Lista_comunes) {
                        fw.write("  ·Paciente común no atendido: " + paciente.nombre + " - Hora de llegada: " + paciente.horaLlegada + "\n");
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
