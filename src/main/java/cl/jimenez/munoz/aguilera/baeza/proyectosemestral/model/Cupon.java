package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false)
    private Integer porcentaje;

    @Column(nullable = false)
    private Integer diasDuracion;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column
    private Integer limiteUsos = 100;

    @Column(nullable = false)
    private Integer usosActuales = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_creador")
    private Usuario creador;

    public Cupon() {}

    public Cupon(String codigo, Integer porcentaje, Integer diasDuracion, Integer limiteUsos, Usuario creador) {
        this.codigo = codigo != null ? codigo.trim().toUpperCase() : "";
        this.porcentaje = Math.min(20, Math.max(1, porcentaje));
        this.diasDuracion = diasDuracion;
        this.limiteUsos = limiteUsos != null ? limiteUsos : 100;
        this.creador = creador;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaExpiracion = this.fechaCreacion.plusDays(diasDuracion);
        this.usosActuales = 0;
        this.activo = true;
    }

    public boolean esValido() {
        if (!activo) {
            return false;
        }
        if (fechaExpiracion != null && LocalDateTime.now().isAfter(fechaExpiracion)) {
            return false;
        }
        if (limiteUsos != null && usosActuales >= limiteUsos) {
            return false;
        }
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo != null ? codigo.trim().toUpperCase() : ""; }

    public Integer getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Integer porcentaje) { this.porcentaje = Math.min(20, Math.max(1, porcentaje)); }

    public Integer getDiasDuracion() { return diasDuracion; }
    public void setDiasDuracion(Integer diasDuracion) { this.diasDuracion = diasDuracion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public Integer getLimiteUsos() { return limiteUsos; }
    public void setLimiteUsos(Integer limiteUsos) { this.limiteUsos = limiteUsos; }

    public Integer getUsosActuales() { return usosActuales; }
    public void setUsosActuales(Integer usosActuales) { this.usosActuales = usosActuales; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Usuario getCreador() { return creador; }
    public void setCreador(Usuario creador) { this.creador = creador; }
}
