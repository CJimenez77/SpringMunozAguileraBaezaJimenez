package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bloqueos_horario")
public class BloqueoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cancha", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean diaCompleto;

    private LocalTime horaInicio;
    private LocalTime horaFin;

    @Column(nullable = false, length = 250)
    private String motivo;

    public boolean seSuperpone(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin)
                && (diaCompleto || (inicio.isBefore(horaFin) && fin.isAfter(horaInicio)));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isDiaCompleto() { return diaCompleto; }
    public void setDiaCompleto(boolean diaCompleto) { this.diaCompleto = diaCompleto; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
