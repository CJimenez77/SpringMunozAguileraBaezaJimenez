package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioOrderByFechaDescHoraInicioDesc(Usuario usuario);

    List<Reserva> findByCanchaInOrderByFechaDescHoraInicioDesc(List<Cancha> canchas);

    List<Reserva> findByGrupoRecurrente(String grupoRecurrente);

    @Query("SELECT r FROM Reserva r WHERE r.cancha = :cancha AND r.fecha = :fecha AND r.estado != 'CANCELADA'")
    List<Reserva> findReservasActivasPorCanchaYFecha(@Param("cancha") Cancha cancha, @Param("fecha") LocalDate fecha);
}
