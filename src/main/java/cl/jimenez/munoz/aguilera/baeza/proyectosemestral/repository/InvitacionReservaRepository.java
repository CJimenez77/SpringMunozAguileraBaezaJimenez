package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.InvitacionReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvitacionReservaRepository extends JpaRepository<InvitacionReserva, Long> {

    Optional<InvitacionReserva> findByCodigo(String codigo);

    Optional<InvitacionReserva> findByReserva(Reserva reserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvitacionReserva i JOIN FETCH i.reserva WHERE i.codigo = :codigo")
    Optional<InvitacionReserva> findByCodigoForUpdate(@Param("codigo") String codigo);

}
