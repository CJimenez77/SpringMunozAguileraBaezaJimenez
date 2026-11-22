package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.projection;

public final class EstadisticasProyecciones {
    private EstadisticasProyecciones() {}

    public interface CanchaReservada {
        Long getCanchaId();
        String getNombre();
        String getComplejo();
        Long getReservas();
    }

    public interface HoraDemandada {
        Integer getHora();
        Long getReservas();
    }

    public interface ClienteFrecuente {
        Long getClienteId();
        String getNombre();
        String getEmail();
        Long getReservas();
    }

    public interface CalificacionCancha {
        Long getCanchaId();
        Long getPuntos();
        Long getOpiniones();
    }
}
