package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @Column(nullable = false)
    private Integer monto;

    @Column(nullable = false, length = 40)
    private String metodoPago;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(length = 80)
    private String transaccionId;

    @Column(length = 50)
    private String codigoAutorizacion;

    @Column(length = 10)
    private String ultimosDigitos;

    @Column(length = 50)
    private String tipoTarjeta;

    @Column(nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    public Pago() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public Integer getMonto() { return monto; }
    public void setMonto(Integer monto) { this.monto = monto; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public void setCodigoAutorizacion(String codigoAutorizacion) { this.codigoAutorizacion = codigoAutorizacion; }

    public String getUltimosDigitos() { return ultimosDigitos; }
    public void setUltimosDigitos(String ultimosDigitos) { this.ultimosDigitos = ultimosDigitos; }

    public String getTipoTarjeta() { return tipoTarjeta; }
    public void setTipoTarjeta(String tipoTarjeta) { this.tipoTarjeta = tipoTarjeta; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}

