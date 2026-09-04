package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;

@Entity
@Table(name = "complejos")
public class Complejo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_complejo;

    @Column(nullable = false)
    private String nombre_complejo;

    @Column(nullable = false)
    private String direccion_complejo;

    @Column(columnDefinition = "TEXT")
    private String ubicacionMapa;

    public Complejo(){}

    public Complejo(String nombre_complejo, String direccion_complejo, String ubicacionMapa) {
        this.nombre_complejo = nombre_complejo;
        this.direccion_complejo = direccion_complejo;
        this.ubicacionMapa = ubicacionMapa;
    }

    public Long getId_complejo() {
        return id_complejo;
    }

    public void setId_complejo(Long id_complejo) {
        this.id_complejo = id_complejo;
    }

    public String getNombre_complejo() {
        return nombre_complejo;
    }

    public void setNombre_complejo(String nombre_complejo) {
        this.nombre_complejo = nombre_complejo;
    }

    public String getDireccion_complejo() {
        return direccion_complejo;
    }

    public void setDireccion_complejo(String direccion_complejo) {
        this.direccion_complejo = direccion_complejo;
    }

    public String getUbicacionMapa() {
        return ubicacionMapa;
    }

    public void setUbicacionMapa(String ubicacionMapa) {
        this.ubicacionMapa = ubicacionMapa;
    }
}