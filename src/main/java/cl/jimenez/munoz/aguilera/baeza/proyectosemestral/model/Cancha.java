package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;

@Entity
@Table(name = "canchas")
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_cancha;

    @Column(nullable = false)
    private String nombre_cancha;

    @Column(nullable = false)
    private String tipo_cancha;

    @Column(nullable = false)
    private Integer precio_hora;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne
    @JoinColumn(name = "id_complejo", nullable = false)
    private Complejo complejo;

    public Cancha() {}

    public Cancha(String nombre_cancha, String tipo_cancha, Integer precio_hora, Complejo complejo) {
        this.nombre_cancha = nombre_cancha;
        this.tipo_cancha = tipo_cancha;
        this.precio_hora = precio_hora;
        this.complejo = complejo;
        this.activo = true;
    }

    public Long getId_cancha() { return id_cancha; }
    public void setId_cancha(Long id_cancha) { this.id_cancha = id_cancha; }
    public String getNombre_cancha() { return nombre_cancha; }
    public void setNombre_cancha(String nombre_cancha) { this.nombre_cancha = nombre_cancha; }
    public String getTipo_cancha() { return tipo_cancha; }
    public void setTipo_cancha(String tipo_cancha) { this.tipo_cancha = tipo_cancha; }
    public Integer getPrecio_hora() { return precio_hora; }
    public void setPrecio_hora(Integer precio_hora) { this.precio_hora = precio_hora; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Complejo getComplejo() { return complejo; }
    public void setComplejo(Complejo complejo) { this.complejo = complejo; }
}