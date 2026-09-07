package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;

@Entity
@Table(name = "servicios_adicionales")
public class ServicioAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_complejo", nullable = false)
    private Complejo complejo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private boolean activo = true;

    public ServicioAdicional() {}

    public ServicioAdicional(Complejo complejo, String nombre, String descripcion, Integer precio) {
        this.complejo = complejo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.activo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complejo getComplejo() { return complejo; }
    public void setComplejo(Complejo complejo) { this.complejo = complejo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
