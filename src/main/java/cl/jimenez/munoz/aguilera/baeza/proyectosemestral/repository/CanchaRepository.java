package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanchaRepository extends JpaRepository<Cancha, Long> {
    
    @Query("SELECT c FROM Cancha c WHERE c.complejo.id_complejo = :idComplejo")
    List<Cancha> findByComplejoId(@Param("idComplejo") Long idComplejo);
}
