package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String rol;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(nullable = false)
    private boolean activo = true;

    public Usuario() {}

    public Usuario(String nombre, String email, String telefono, String password, String rol, String foto) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.password = password;
        this.rol = rol;
        this.foto = foto;
        this.activo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}