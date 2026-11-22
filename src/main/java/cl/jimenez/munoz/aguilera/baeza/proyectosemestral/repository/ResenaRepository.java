package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Resena;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.projection.EstadisticasProyecciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    @Query("SELECT AVG(r.puntuacion) FROM Resena r WHERE r.cancha = :cancha")
    Double findPromedioCalificacionPorCancha(@Param("cancha") Cancha cancha);

    Long countByCancha(Cancha cancha);

    @Query("SELECT r.cancha.id_cancha AS canchaId, SUM(r.puntuacion) AS puntos, COUNT(r) AS opiniones "
            + "FROM Resena r WHERE r.cancha IN :canchas AND r.puntuacion BETWEEN 1 AND 5 "
            + "AND r.fecha >= :desde AND r.fecha < :hastaExclusivo GROUP BY r.cancha.id_cancha")
    List<EstadisticasProyecciones.CalificacionCancha> findCalificacionesParaEstadisticas(
            @Param("canchas") List<Cancha> canchas, @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo);
}
