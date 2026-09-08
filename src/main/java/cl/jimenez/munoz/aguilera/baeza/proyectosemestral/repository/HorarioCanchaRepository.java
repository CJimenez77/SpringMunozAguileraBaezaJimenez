package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.HorarioCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioCanchaRepository extends JpaRepository<HorarioCancha, Long> {

    Optional<HorarioCancha> findByCanchaAndDiaSemana(Cancha cancha, Integer diaSemana);

    List<HorarioCancha> findByCanchaOrderByDiaSemanaAsc(Cancha cancha);
}
