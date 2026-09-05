package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanchaRepository extends JpaRepository<Cancha, Long> {
    List<Cancha> findByActivoTrue();

    @Query("SELECT c FROM Cancha c WHERE c.complejo.id_complejo = :idComplejo AND c.activo = true")
    List<Cancha> findByComplejoIdAndActivoTrue(@Param("idComplejo") Long idComplejo);

    @Query("SELECT c FROM Cancha c WHERE c.complejo.dueno = :dueno AND c.activo = true")
    List<Cancha> findByDuenoAndActivoTrue(@Param("dueno") Usuario dueno);

    List<Cancha> findByComplejoInAndActivoTrue(List<Complejo> complejos);

    long countByActivoTrue();
}