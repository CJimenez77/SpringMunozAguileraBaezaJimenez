package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipo_jugadores")
public class EquipoJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, length = 10)
    private String equipo;

    @Column(nullable = false, length = 100)
    private String nombreJugador;

    @Column(length = 50)
    private String posicion;

    private Integer numeroCamiseta;

    public EquipoJugador() {}

    public EquipoJugador(Reserva reserva, String equipo, String nombreJugador, String posicion, Integer numeroCamiseta) {
        this.reserva = reserva;
        this.equipo = equipo;
        this.nombreJugador = nombreJugador;
        this.posicion = posicion;
        this.numeroCamiseta = numeroCamiseta;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public String getEquipo() { return equipo; }
    public void setEquipo(String equipo) { this.equipo = equipo; }

    public String getNombreJugador() { return nombreJugador; }
    public void setNombreJugador(String nombreJugador) { this.nombreJugador = nombreJugador; }

    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }

    public Integer getNumeroCamiseta() { return numeroCamiseta; }
    public void setNumeroCamiseta(Integer numeroCamiseta) { this.numeroCamiseta = numeroCamiseta; }
}
