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

    @Query("SELECT r FROM Reserva r WHERE r.cancha = :cancha AND r.fecha BETWEEN :inicio AND :fin "
            + "AND r.estado != 'CANCELADA' ORDER BY r.fecha, r.horaInicio")
    List<Reserva> findReservasActivasPorCanchaYRango(@Param("cancha") Cancha cancha,
                                                  @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.cancha c JOIN FETCH c.complejo co JOIN FETCH r.usuario "
            + "WHERE r.fecha BETWEEN :desde AND :hasta AND (:todos = true OR co.dueno = :usuario) "
            + "ORDER BY r.fecha, r.horaInicio, r.id")
    List<Reserva> findParaReportes(@Param("usuario") Usuario usuario, @Param("todos") boolean todos,
                                 @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
