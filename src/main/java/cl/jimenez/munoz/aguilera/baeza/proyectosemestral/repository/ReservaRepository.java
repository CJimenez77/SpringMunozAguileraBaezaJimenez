package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.projection.EstadisticasProyecciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Reserva r SET r.estado = 'CANCELACION_EN_PROCESO', "
            + "r.porcentajeReembolsoAplicado = :porcentaje WHERE r.id = :id "
            + "AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELACION_PENDIENTE')")
    int iniciarCancelacion(@Param("id") Long id, @Param("porcentaje") int porcentaje);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Reserva r SET r.estado = :estadoNuevo WHERE r.id = :id AND r.estado = :estadoActual")
    int cambiarEstadoCancelacion(@Param("id") Long id, @Param("estadoActual") String estadoActual,
                                 @Param("estadoNuevo") String estadoNuevo);

    @Query("SELECT r FROM Reserva r WHERE r.usuario = :usuario AND r.estado IN ('CONFIRMADA', 'COMPLETADA') ORDER BY r.id ASC")
    List<Reserva> findValidasPorUsuario(@Param("usuario") Usuario usuario);

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

    @Query("SELECT r FROM Reserva r JOIN FETCH r.usuario JOIN FETCH r.cancha c JOIN FETCH c.complejo " +
            "WHERE r.estado = 'CONFIRMADA' AND r.fecha BETWEEN :desde AND :hasta")
    List<Reserva> findConfirmadasParaRecordatorio(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT c.id_cancha AS canchaId, c.nombre_cancha AS nombre, c.complejo.nombre_complejo AS complejo, "
            + "COUNT(r) AS reservas FROM Reserva r JOIN r.cancha c "
            + "WHERE c IN :canchas AND r.fecha BETWEEN :desde AND :hasta "
            + "AND r.estado IN ('CONFIRMADA', 'COMPLETADA') "
            + "GROUP BY c.id_cancha, c.nombre_cancha, c.complejo.nombre_complejo "
            + "ORDER BY COUNT(r) DESC, c.nombre_cancha, c.id_cancha")
    List<EstadisticasProyecciones.CanchaReservada> findRankingCanchas(
            @Param("canchas") List<Cancha> canchas, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT HOUR(r.horaInicio) AS hora, COUNT(r) AS reservas FROM Reserva r "
            + "WHERE r.cancha IN :canchas AND r.fecha BETWEEN :desde AND :hasta "
            + "AND r.estado IN ('CONFIRMADA', 'COMPLETADA') "
            + "GROUP BY HOUR(r.horaInicio) ORDER BY COUNT(r) DESC, HOUR(r.horaInicio)")
    List<EstadisticasProyecciones.HoraDemandada> findRankingHoras(
            @Param("canchas") List<Cancha> canchas, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT u.id AS clienteId, u.nombre AS nombre, u.email AS email, COUNT(r) AS reservas "
            + "FROM Reserva r JOIN r.usuario u "
            + "WHERE r.cancha IN :canchas AND r.fecha BETWEEN :desde AND :hasta "
            + "AND r.estado IN ('CONFIRMADA', 'COMPLETADA') "
            + "GROUP BY u.id, u.nombre, u.email ORDER BY COUNT(r) DESC, u.nombre, u.id")
    List<EstadisticasProyecciones.ClienteFrecuente> findRankingClientes(
            @Param("canchas") List<Cancha> canchas, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
