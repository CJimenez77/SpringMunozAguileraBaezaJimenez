package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import java.time.LocalTime;

public class BloqueHorario {

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean disponible;
    private String motivo;
    private Integer precioCalculado;
    private boolean esPeak;

    public BloqueHorario() {}

    public BloqueHorario(LocalTime horaInicio, LocalTime horaFin, boolean disponible, String motivo, Integer precioCalculado, boolean esPeak) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.disponible = disponible;
        this.motivo = motivo;
        this.precioCalculado = precioCalculado;
        this.esPeak = esPeak;
    }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Integer getPrecioCalculado() { return precioCalculado; }
    public void setPrecioCalculado(Integer precioCalculado) { this.precioCalculado = precioCalculado; }

    public boolean isEsPeak() { return esPeak; }
    public void setEsPeak(boolean esPeak) { this.esPeak = esPeak; }

    public String getHoraTexto() {
        return String.format("%02d:%02d", horaInicio.getHour(), horaInicio.getMinute());
    }
}
