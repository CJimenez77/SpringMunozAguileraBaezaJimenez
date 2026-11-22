package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.TransferenciaReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaReservaRepository extends JpaRepository<TransferenciaReserva, Long> {

    @Query("SELECT t FROM TransferenciaReserva t JOIN FETCH t.usuarioOrigen JOIN FETCH t.usuarioDestino " +
            "WHERE t.reserva = :reserva ORDER BY t.fechaTransferencia DESC")
    List<TransferenciaReserva> findHistorialPorReserva(@Param("reserva") Reserva reserva);

    @Query("SELECT t FROM TransferenciaReserva t JOIN FETCH t.reserva r JOIN FETCH r.cancha c JOIN FETCH c.complejo " +
            "JOIN FETCH t.usuarioOrigen JOIN FETCH t.usuarioDestino " +
            "WHERE t.usuarioOrigen = :usuario OR t.usuarioDestino = :usuario ORDER BY t.fechaTransferencia DESC")
    List<TransferenciaReserva> findHistorialPorUsuario(@Param("usuario") Usuario usuario);
}
