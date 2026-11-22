package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Favorito;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    Optional<Favorito> findByUsuarioAndCancha(Usuario usuario, Cancha cancha);

    Optional<Favorito> findByUsuarioAndComplejo(Usuario usuario, Complejo complejo);

    @Query("SELECT f.cancha.id_cancha FROM Favorito f WHERE f.usuario = :usuario AND f.cancha IS NOT NULL AND f.cancha.activo = true")
    List<Long> findCanchaIdsFavoritasByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT f.complejo.id_complejo FROM Favorito f WHERE f.usuario = :usuario AND f.complejo IS NOT NULL AND f.complejo.activo = true")
    List<Long> findComplejoIdsFavoritosByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT f.cancha FROM Favorito f JOIN f.cancha c JOIN FETCH c.complejo WHERE f.usuario = :usuario AND c.activo = true ORDER BY f.fechaCreacion DESC")
    List<Cancha> findCanchasFavoritasByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT f.complejo FROM Favorito f JOIN f.complejo comp WHERE f.usuario = :usuario AND comp.activo = true ORDER BY f.fechaCreacion DESC")
    List<Complejo> findComplejosFavoritosByUsuario(@Param("usuario") Usuario usuario);
}
