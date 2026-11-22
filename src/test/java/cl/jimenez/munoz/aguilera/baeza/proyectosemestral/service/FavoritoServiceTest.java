package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Favorito;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.FavoritoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private CanchaRepository canchaRepository;

    @Mock
    private ComplejoRepositorio complejoRepositorio;

    @InjectMocks
    private FavoritoService favoritoService;

    private Usuario usuario;
    private Complejo complejo;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Cliente", "cliente@canchasya.cl", "911111111", "pass", "CLIENTE", null);
        usuario.setId(1L);
        complejo = new Complejo("Complejo Sur", "Av. Sur 200", null, usuario);
        complejo.setId_complejo(10L);
        cancha = new Cancha("Cancha Pádel", "Pádel", 14000, complejo);
        cancha.setId_cancha(20L);
    }

    @Test
    void toggleCanchaAgregaCuandoNoExiste() {
        when(canchaRepository.findById(20L)).thenReturn(Optional.of(cancha));
        when(favoritoRepository.findByUsuarioAndCancha(usuario, cancha)).thenReturn(Optional.empty());

        boolean agregado = favoritoService.toggleCanchaFavorita(usuario, 20L);

        assertTrue(agregado);
        verify(favoritoRepository).save(any(Favorito.class));
        verify(favoritoRepository, never()).delete(any());
    }

    @Test
    void toggleCanchaEliminaCuandoYaExiste() {
        Favorito existente = new Favorito(usuario, cancha);
        when(canchaRepository.findById(20L)).thenReturn(Optional.of(cancha));
        when(favoritoRepository.findByUsuarioAndCancha(usuario, cancha)).thenReturn(Optional.of(existente));

        boolean agregado = favoritoService.toggleCanchaFavorita(usuario, 20L);

        assertFalse(agregado);
        verify(favoritoRepository).delete(existente);
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void toggleComplejoAgregaYEliminaCorrectamente() {
        when(complejoRepositorio.findById(10L)).thenReturn(Optional.of(complejo));
        when(favoritoRepository.findByUsuarioAndComplejo(usuario, complejo)).thenReturn(Optional.empty());

        assertTrue(favoritoService.toggleComplejoFavorito(usuario, 10L));
        verify(favoritoRepository).save(any(Favorito.class));

        Favorito existente = new Favorito(usuario, complejo);
        when(favoritoRepository.findByUsuarioAndComplejo(usuario, complejo)).thenReturn(Optional.of(existente));

        assertFalse(favoritoService.toggleComplejoFavorito(usuario, 10L));
        verify(favoritoRepository).delete(existente);
    }

    @Test
    void lanzaExcepcionSiCanchaInactivaONoExiste() {
        cancha.setActivo(false);
        when(canchaRepository.findById(20L)).thenReturn(Optional.of(cancha));

        assertThrows(IllegalArgumentException.class, () -> favoritoService.toggleCanchaFavorita(usuario, 20L));
    }
}
