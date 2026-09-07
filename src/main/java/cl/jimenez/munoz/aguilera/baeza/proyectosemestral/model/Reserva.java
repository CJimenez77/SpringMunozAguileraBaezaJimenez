package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cancha", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false)
    private Integer precioCancha;

    @Column(nullable = false)
    private Integer precioServicios = 0;

    @Column(nullable = false)
    private Integer precioTotal;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(nullable = false, unique = true, length = 60)
    private String codigoReserva;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(length = 60)
    private String grupoRecurrente;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "reserva_servicios",
            joinColumns = @JoinColumn(name = "id_reserva"),
            inverseJoinColumns = @JoinColumn(name = "id_servicio")
    )
    private List<ServicioAdicional> servicios = new ArrayList<>();

    public Reserva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public Integer getPrecioCancha() { return precioCancha; }
    public void setPrecioCancha(Integer precioCancha) { this.precioCancha = precioCancha; }

    public Integer getPrecioServicios() { return precioServicios; }
    public void setPrecioServicios(Integer precioServicios) { this.precioServicios = precioServicios; }

    public Integer getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(Integer precioTotal) { this.precioTotal = precioTotal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getGrupoRecurrente() { return grupoRecurrente; }
    public void setGrupoRecurrente(String grupoRecurrente) { this.grupoRecurrente = grupoRecurrente; }

    public List<ServicioAdicional> getServicios() { return servicios; }
    public void setServicios(List<ServicioAdicional> servicios) { this.servicios = servicios; }

    public boolean esCancelable() {
        if (!"CONFIRMADA".equals(estado) && !"PENDIENTE".equals(estado)) {
            return false;
        }
        LocalDateTime fechaHoraInicio = LocalDateTime.of(fecha, horaInicio);
        return Duration.between(LocalDateTime.now(), fechaHoraInicio).toHours() >= 24;
    }
}

