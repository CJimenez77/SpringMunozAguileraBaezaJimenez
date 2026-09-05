package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplejoRepositorio extends JpaRepository<Complejo, Long> {
    List<Complejo> findByActivoTrue();
    List<Complejo> findByDuenoAndActivoTrue(Usuario dueno);
    long countByActivoTrue();
}