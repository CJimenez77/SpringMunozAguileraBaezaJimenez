package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.InvitacionReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ParticipanteReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ParticipanteReservaRepository extends JpaRepository<ParticipanteReserva, Long> {

    Optional<ParticipanteReserva> findByInvitacionAndUsuario(InvitacionReserva invitacion, Usuario usuario);

    Optional<ParticipanteReserva> findByBuyOrder(String buyOrder);

    @Query("SELECT p FROM ParticipanteReserva p JOIN FETCH p.invitacion i JOIN FETCH i.reserva "
            + "WHERE p.tokenWs = :tokenWs")
    Optional<ParticipanteReserva> findByTokenWs(@Param("tokenWs") String tokenWs);

    List<ParticipanteReserva> findByInvitacionOrderByIdAsc(InvitacionReserva invitacion);

    long countByInvitacion(InvitacionReserva invitacion);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ParticipanteReserva p SET p.estado = 'REEMBOLSO_EN_PROCESO', "
            + "p.montoReembolsado = :monto WHERE p.id = :id AND p.estado IN ('PAGADO', 'REEMBOLSO_EN_PROCESO')")
    int iniciarReembolso(@Param("id") Long id, @Param("monto") int monto);

    @Query("SELECT p FROM ParticipanteReserva p JOIN FETCH p.invitacion i JOIN FETCH i.reserva r "
            + "JOIN FETCH r.cancha c JOIN FETCH c.complejo WHERE p.usuario = :usuario")
    List<ParticipanteReserva> findByUsuarioConReserva(@Param("usuario") Usuario usuario);

    @Query("SELECT p FROM ParticipanteReserva p JOIN FETCH p.invitacion i JOIN FETCH i.reserva r "
            + "JOIN FETCH r.cancha WHERE p.usuario = :usuario ORDER BY p.id DESC")
    List<ParticipanteReserva> findHistorialPorUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT p FROM ParticipanteReserva p JOIN FETCH p.invitacion i JOIN FETCH i.reserva r "
            + "JOIN FETCH r.cancha ORDER BY p.id DESC")
    List<ParticipanteReserva> findHistorialCompleto();
}
