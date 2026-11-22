package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "favoritos",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"id_usuario", "id_cancha"}),
                @UniqueConstraint(columnNames = {"id_usuario", "id_complejo"})
        }
)
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cancha")
    private Cancha cancha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_complejo")
    private Complejo complejo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Favorito() {}

    public Favorito(Usuario usuario, Cancha cancha) {
        this.usuario = usuario;
        this.cancha = cancha;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Favorito(Usuario usuario, Complejo complejo) {
        this.usuario = usuario;
        this.complejo = complejo;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }

    public Complejo getComplejo() { return complejo; }
    public void setComplejo(Complejo complejo) { this.complejo = complejo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
