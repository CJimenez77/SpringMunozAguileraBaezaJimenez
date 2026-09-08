package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    @Query("SELECT AVG(r.puntuacion) FROM Resena r WHERE r.cancha = :cancha")
    Double findPromedioCalificacionPorCancha(@Param("cancha") Cancha cancha);

    Long countByCancha(Cancha cancha);
}
