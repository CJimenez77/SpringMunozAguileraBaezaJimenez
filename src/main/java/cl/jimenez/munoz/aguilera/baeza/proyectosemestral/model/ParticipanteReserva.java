package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participantes_reserva", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_invitacion", "id_usuario"})
})
public class ParticipanteReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_invitacion", nullable = false)
    private InvitacionReserva invitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer montoCuota;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(unique = true, length = 60)
    private String buyOrder;

    @Column(unique = true, length = 80)
    private String tokenWs;

    @Column(length = 80)
    private String transaccionId;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer montoReembolsado = 0;

    @Column
    private LocalDateTime fechaPago;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InvitacionReserva getInvitacion() { return invitacion; }
    public void setInvitacion(InvitacionReserva invitacion) { this.invitacion = invitacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Integer getMontoCuota() { return montoCuota; }
    public void setMontoCuota(Integer montoCuota) { this.montoCuota = montoCuota; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getBuyOrder() { return buyOrder; }
    public void setBuyOrder(String buyOrder) { this.buyOrder = buyOrder; }

    public String getTokenWs() { return tokenWs; }
    public void setTokenWs(String tokenWs) { this.tokenWs = tokenWs; }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public Integer getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(Integer montoReembolsado) { this.montoReembolsado = montoReembolsado; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
