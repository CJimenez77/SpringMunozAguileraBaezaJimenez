package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RecordatorioService recordatorioService;

    private Usuario usuario;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Jugador", "jugador@canchasya.cl", "911111111", "pass", "CLIENTE", null);
        usuario.setId(1L);
        Complejo complejo = new Complejo("Complejo Norte", "Av. Uno 100", null, usuario);
        cancha = new Cancha("Cancha 1", "Fútbol", 15000, complejo);
    }

    private Reserva crearReservaEn(LocalDateTime fechaHoraInicio) {
        Reserva r = new Reserva();
        r.setId(10L);
        r.setCodigoReserva("RES-TEST01");
        r.setUsuario(usuario);
        r.setCancha(cancha);
        r.setEstado("CONFIRMADA");
        r.setFecha(fechaHoraInicio.toLocalDate());
        r.setHoraInicio(fechaHoraInicio.toLocalTime());
        r.setHoraFin(fechaHoraInicio.toLocalTime().plusHours(1));
        return r;
    }

    @Test
    void enviaRecordatorio24hYMarcaBandera() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 21, 12, 0);
        Reserva reserva = crearReservaEn(ahora.plusHours(20));

        when(reservaRepository.findConfirmadasParaRecordatorio(any(), any())).thenReturn(List.of(reserva));

        int enviados = recordatorioService.procesarRecordatorios(ahora);

        assertEquals(1, enviados);
        assertTrue(reserva.isRecordatorio24hEnviado());
        assertFalse(reserva.isRecordatorio2hEnviado());
        verify(emailService).enviarRecordatorioPartido(reserva, 24);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void enviaRecordatorio2hYMarcaBandera() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 21, 18, 0);
        Reserva reserva = crearReservaEn(ahora.plusMinutes(90));
        reserva.setRecordatorio24hEnviado(true);

        when(reservaRepository.findConfirmadasParaRecordatorio(any(), any())).thenReturn(List.of(reserva));

        int enviados = recordatorioService.procesarRecordatorios(ahora);

        assertEquals(1, enviados);
        assertTrue(reserva.isRecordatorio2hEnviado());
        verify(emailService).enviarRecordatorioPartido(reserva, 2);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void noReenviaSiBanderasYaEstanMarcadas() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 21, 18, 0);
        Reserva reserva = crearReservaEn(ahora.plusHours(1));
        reserva.setRecordatorio24hEnviado(true);
        reserva.setRecordatorio2hEnviado(true);

        when(reservaRepository.findConfirmadasParaRecordatorio(any(), any())).thenReturn(List.of(reserva));

        int enviados = recordatorioService.procesarRecordatorios(ahora);

        assertEquals(0, enviados);
        verify(emailService, never()).enviarRecordatorioPartido(any(), anyInt());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void ignoraReservasPasadas() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 21, 18, 0);
        Reserva reserva = crearReservaEn(ahora.minusMinutes(15));

        when(reservaRepository.findConfirmadasParaRecordatorio(any(), any())).thenReturn(List.of(reserva));

        int enviados = recordatorioService.procesarRecordatorios(ahora);

        assertEquals(0, enviados);
        verify(emailService, never()).enviarRecordatorioPartido(any(), anyInt());
    }
}
