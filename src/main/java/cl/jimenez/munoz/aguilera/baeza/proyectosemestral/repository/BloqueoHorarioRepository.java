package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.BloqueoHorario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BloqueoHorarioRepository extends JpaRepository<BloqueoHorario, Long> {

    List<BloqueoHorario> findByCanchaOrderByFechaInicioAscHoraInicioAsc(Cancha cancha);

    @Query("SELECT b FROM BloqueoHorario b WHERE b.cancha = :cancha "
            + "AND b.fechaInicio <= :fin AND b.fechaFin >= :inicio ORDER BY b.fechaInicio, b.id")
    List<BloqueoHorario> findPorRango(@Param("cancha") Cancha cancha,
                                    @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT b FROM BloqueoHorario b WHERE b.cancha IN :canchas "
            + "AND b.fechaInicio <= :hasta AND b.fechaFin >= :desde")
    List<BloqueoHorario> findParaReportes(@Param("canchas") List<Cancha> canchas,
                                        @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
