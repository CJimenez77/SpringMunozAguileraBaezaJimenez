package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Favorito;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.FavoritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final CanchaRepository canchaRepository;
    private final ComplejoRepositorio complejoRepositorio;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           CanchaRepository canchaRepository,
                           ComplejoRepositorio complejoRepositorio) {
        this.favoritoRepository = favoritoRepository;
        this.canchaRepository = canchaRepository;
        this.complejoRepositorio = complejoRepositorio;
    }

    @Transactional
    public boolean toggleCanchaFavorita(Usuario usuario, Long canchaId) {
        Cancha cancha = canchaRepository.findById(canchaId).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            throw new IllegalArgumentException("La cancha seleccionada no existe o no está disponible.");
        }

        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndCancha(usuario, cancha);
        if (existente.isPresent()) {
            favoritoRepository.delete(existente.get());
            return false;
        }

        favoritoRepository.save(new Favorito(usuario, cancha));
        return true;
    }

    @Transactional
    public boolean toggleComplejoFavorito(Usuario usuario, Long complejoId) {
        Complejo complejo = complejoRepositorio.findById(complejoId).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            throw new IllegalArgumentException("El complejo seleccionado no existe o no está disponible.");
        }

        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndComplejo(usuario, complejo);
        if (existente.isPresent()) {
            favoritoRepository.delete(existente.get());
            return false;
        }

        favoritoRepository.save(new Favorito(usuario, complejo));
        return true;
    }

    public Set<Long> obtenerIdsCanchasFavoritas(Usuario usuario) {
        if (usuario == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(favoritoRepository.findCanchaIdsFavoritasByUsuario(usuario));
    }

    public Set<Long> obtenerIdsComplejosFavoritos(Usuario usuario) {
        if (usuario == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(favoritoRepository.findComplejoIdsFavoritosByUsuario(usuario));
    }

    public List<Cancha> obtenerCanchasFavoritas(Usuario usuario) {
        if (usuario == null) {
            return Collections.emptyList();
        }
        return favoritoRepository.findCanchasFavoritasByUsuario(usuario);
    }

    public List<Complejo> obtenerComplejosFavoritos(Usuario usuario) {
        if (usuario == null) {
            return Collections.emptyList();
        }
        return favoritoRepository.findComplejosFavoritosByUsuario(usuario);
    }
}
