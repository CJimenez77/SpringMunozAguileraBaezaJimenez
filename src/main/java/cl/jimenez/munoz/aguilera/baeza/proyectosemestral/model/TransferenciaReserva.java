package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferencias_reservas")
public class TransferenciaReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_origen", nullable = false)
    private Usuario usuarioOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_destino", nullable = false)
    private Usuario usuarioDestino;

    @Column(nullable = false)
    private LocalDateTime fechaTransferencia = LocalDateTime.now();

    @Column(length = 250)
    private String motivo;

    public TransferenciaReserva() {}

    public TransferenciaReserva(Reserva reserva, Usuario usuarioOrigen, Usuario usuarioDestino, String motivo) {
        this.reserva = reserva;
        this.usuarioOrigen = usuarioOrigen;
        this.usuarioDestino = usuarioDestino;
        this.motivo = motivo;
        this.fechaTransferencia = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public Usuario getUsuarioOrigen() { return usuarioOrigen; }
    public void setUsuarioOrigen(Usuario usuarioOrigen) { this.usuarioOrigen = usuarioOrigen; }

    public Usuario getUsuarioDestino() { return usuarioDestino; }
    public void setUsuarioDestino(Usuario usuarioDestino) { this.usuarioDestino = usuarioDestino; }

    public LocalDateTime getFechaTransferencia() { return fechaTransferencia; }
    public void setFechaTransferencia(LocalDateTime fechaTransferencia) { this.fechaTransferencia = fechaTransferencia; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
