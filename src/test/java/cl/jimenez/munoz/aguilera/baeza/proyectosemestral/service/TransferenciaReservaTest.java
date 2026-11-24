package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.InvitacionReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ParticipanteReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.TransferenciaReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaReservaTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private DisponibilidadService disponibilidadService;

    @Mock
    private TarifaService tarifaService;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransferenciaReservaRepository transferenciaReservaRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CanchaRepository canchaRepository;

    @Mock
    private FidelizacionService fidelizacionService;

    @Mock
    private CuponService cuponService;

    @Mock
    private InvitacionReservaRepository invitacionReservaRepository;

    @Mock
    private ParticipanteReservaRepository participanteReservaRepository;

    @Mock
    private WebpayService webpayService;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario origen;
    private Usuario destino;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        origen = new Usuario("Titular Original", "origen@canchasya.cl", "911111111", "pass", "CLIENTE", null);
        origen.setId(1L);

        destino = new Usuario("Nuevo Titular", "destino@canchasya.cl", "922222222", "pass", "CLIENTE", null);
        destino.setId(2L);

        Complejo complejo = new Complejo("Complejo Centro", "Av. Centro 123", null, origen);
        Cancha cancha = new Cancha("Cancha 1", "Fútbol", 20000, complejo);

        reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCodigoReserva("RES-ABC123");
        reserva.setUsuario(origen);
        reserva.setCancha(cancha);
        reserva.setEstado("CONFIRMADA");
        reserva.setFecha(LocalDate.now().plusDays(2));
        reserva.setHoraInicio(LocalTime.of(19, 0));
        reserva.setHoraFin(LocalTime.of(20, 0));
    }

    @Test
    void transfiereReservaPagadaYGuardaTrazabilidad() {
        Pago pago = new Pago();
        pago.setEstado("PAGADO");

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findByEmailIgnoreCase("destino@canchasya.cl")).thenReturn(Optional.of(destino));
        when(transferenciaReservaRepository.save(any(TransferenciaReserva.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransferenciaReserva resultado = reservaService.transferirReserva(100L, origen, "destino@canchasya.cl", "Lesión");

        assertEquals(destino.getId(), reserva.getUsuario().getId());
        assertEquals(origen.getId(), resultado.getUsuarioOrigen().getId());
        assertEquals(destino.getId(), resultado.getUsuarioDestino().getId());
        assertEquals("Lesión", resultado.getMotivo());
        verify(reservaRepository).save(reserva);
        verify(transferenciaReservaRepository).save(any(TransferenciaReserva.class));
        verify(emailService).enviarTransferenciaReserva(reserva, origen, destino, "Lesión");
    }

    @Test
    void rechazaTransferenciaSiNoEsTitular() {
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class,
                () -> reservaService.transferirReserva(100L, destino, "otro@canchasya.cl", null));
        verify(transferenciaReservaRepository, never()).save(any());
    }

    @Test
    void rechazaTransferenciaSiReservaNoEstaConfirmada() {
        reserva.setEstado("PENDIENTE");
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class,
                () -> reservaService.transferirReserva(100L, origen, "destino@canchasya.cl", null));
    }

    @Test
    void rechazaTransferenciaAMismoUsuario() {
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("origen@canchasya.cl")).thenReturn(Optional.of(origen));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.transferirReserva(100L, origen, "origen@canchasya.cl", null));
    }
}
