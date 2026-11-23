package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, Long> {

    Optional<Cupon> findByCodigoIgnoreCaseAndActivoTrue(String codigo);

    List<Cupon> findAllByOrderByFechaCreacionDesc();

    @Query("SELECT c FROM Cupon c WHERE c.activo = true AND c.fechaExpiracion > CURRENT_TIMESTAMP ORDER BY c.id DESC")
    List<Cupon> findCuponesVigentes();
}
