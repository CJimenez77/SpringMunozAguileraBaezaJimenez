package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.EquipoJugador;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoJugadorRepository extends JpaRepository<EquipoJugador, Long> {

    List<EquipoJugador> findByReservaAndEquipo(Reserva reserva, String equipo);
}
