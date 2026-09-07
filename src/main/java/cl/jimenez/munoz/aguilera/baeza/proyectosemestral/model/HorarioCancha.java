package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_cancha")
public class HorarioCancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cancha", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private Integer diaSemana;

    @Column(nullable = false)
    private LocalTime horaApertura;

    @Column(nullable = false)
    private LocalTime horaCierre;

    @Column(nullable = false)
    private boolean abierto = true;

    public HorarioCancha() {}

    public HorarioCancha(Cancha cancha, Integer diaSemana, LocalTime horaApertura, LocalTime horaCierre, boolean abierto) {
        this.cancha = cancha;
        this.diaSemana = diaSemana;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.abierto = abierto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public boolean isAbierto() { return abierto; }
    public void setAbierto(boolean abierto) { this.abierto = abierto; }

    public String getNombreDia() {
        if (diaSemana == null) return "";
        switch (diaSemana) {
            case 1: return "Lunes";
            case 2: return "Martes";
            case 3: return "Miércoles";
            case 4: return "Jueves";
            case 5: return "Viernes";
            case 6: return "Sábado";
            case 7: return "Domingo";
            default: return "";
        }
    }
}
