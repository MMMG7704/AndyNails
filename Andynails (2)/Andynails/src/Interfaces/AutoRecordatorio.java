package Interfaces;

public class AutoRecordatorio {

    public static void iniciar() {
        System.out.println(" Buscando citas para mañana (solo enviará una vez por cita)...");
        
        RecordatorioCita.enviarRecordatorios(id -> true);

        System.out.println(" Proceso de recordatorios completado.");
    }
}
