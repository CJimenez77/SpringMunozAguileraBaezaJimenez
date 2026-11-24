package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.*;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoliticaReembolsoReservaTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private DisponibilidadService disponibilidadService;
    @Mock private TarifaService tarifaService;
    @Mock private PagoRepository pagoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TransferenciaReservaRepository transferenciaReservaRepository;
    @Mock private EmailService emailService;
    @Mock private CanchaRepository canchaRepository;
    @Mock private FidelizacionService fidelizacionService;
    @Mock private CuponService cuponService;
    @Mock private InvitacionReservaRepository invitacionReservaRepository;
    @Mock private ParticipanteReservaRepository participanteReservaRepository;
    @Mock private WebpayService webpayService;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario titular;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        titular = new Usuario("Titular", "titular@canchasya.cl", "911111111", "pass", "CLIENTE", null);
        titular.setId(1L);

        reserva = new Reserva();
        reserva.setId(10L);
        reserva.setUsuario(titular);
        reserva.setEstado("CONFIRMADA");
        reserva.setFecha(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.now().plusHours(12));
        reserva.setPrecioTotal(10_000);
    }

    @Test
    void aplicaReembolsoTotalSoloConMasDe24Horas() {
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 1, 10, 0);

        reserva.setFecha(ahora.plusHours(24).toLocalDate());
        reserva.setHoraInicio(ahora.plusHours(24).toLocalTime());
        assertEquals(50, reserva.porcentajeReembolso(ahora));

        reserva.setFecha(ahora.plusHours(24).plusMinutes(1).toLocalDate());
        reserva.setHoraInicio(ahora.plusHours(24).plusMinutes(1).toLocalTime());
        assertEquals(100, reserva.porcentajeReembolso(ahora));
    }

    @Test
    void cancelaReservaYReversaLaMitadDentroDe24Horas() throws Exception {
        reserva.setFecha(LocalDateTime.now().plusHours(12).toLocalDate());
        reserva.setHoraInicio(LocalDateTime.now().plusHours(12).toLocalTime());
        Pago pago = pagoPagado();

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 50)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.iniciarReembolso(1L, 5_000)).thenReturn(1);
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.empty());

        assertTrue(reservaService.cancelarReserva(10L, titular));

        verify(webpayService).reembolsar("token-webpay", 5_000);
        assertEquals("CANCELADA", reserva.getEstado());
        assertEquals(50, reserva.getPorcentajeReembolsoAplicado());
        assertEquals("REEMBOLSO_PARCIAL", pago.getEstado());
        assertEquals(5_000, pago.getMontoReembolsado());
        verify(pagoRepository).save(pago);
    }

    @Test
    void cancelaConMasDe24HorasYReversaElMontoCompleto() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusHours(48);
        reserva.setFecha(inicio.toLocalDate());
        reserva.setHoraInicio(inicio.toLocalTime());
        Pago pago = pagoPagado();

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 100)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.iniciarReembolso(1L, 10_000)).thenReturn(1);
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.empty());

        assertTrue(reservaService.cancelarReserva(10L, titular));

        verify(webpayService).reembolsar("token-webpay", 10_000);
        assertEquals(100, reserva.getPorcentajeReembolsoAplicado());
        assertEquals("REEMBOLSADO", pago.getEstado());
        assertEquals(10_000, pago.getMontoReembolsado());
    }

    @Test
    void revierteLaCuotaDeCadaParticipanteAlCancelarPagoCompartido() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusHours(12);
        reserva.setFecha(inicio.toLocalDate());
        reserva.setHoraInicio(inicio.toLocalTime());
        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo("invitation-code");
        invitacion.setMaxParticipantes(2);
        invitacion.setActiva(true);
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setInvitacion(invitacion);
        participante.setUsuario(titular);
        participante.setMontoCuota(5_000);
        participante.setEstado("PAGADO");
        participante.setTokenWs("participant-token");
        participante.setId(1L);

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 50)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.of(invitacion));
        when(participanteReservaRepository.findByInvitacionOrderByIdAsc(invitacion))
                .thenReturn(java.util.List.of(participante));
        when(participanteReservaRepository.iniciarReembolso(1L, 2_500)).thenReturn(1);

        assertTrue(reservaService.cancelarReserva(10L, titular));

        verify(webpayService).reembolsar("participant-token", 2_500);
        assertEquals("REEMBOLSO_PARCIAL", participante.getEstado());
        assertEquals(2_500, participante.getMontoReembolsado());
        assertFalse(invitacion.isActiva());
    }

    @Test
    void noCancelaSiWebpayNoConfirmaElReembolso() throws Exception {
        reserva.setFecha(LocalDateTime.now().plusHours(12).toLocalDate());
        reserva.setHoraInicio(LocalDateTime.now().plusHours(12).toLocalTime());
        Pago pago = pagoPagado();

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 50)).thenReturn(1);
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.iniciarReembolso(1L, 5_000)).thenReturn(1);
        when(webpayService.reembolsar("token-webpay", 5_000)).thenThrow(new RuntimeException("Webpay sin conexión"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> reservaService.cancelarReserva(10L, titular));

        assertTrue(error.getMessage().contains("La cancelación quedó pendiente"));
        assertEquals("CANCELACION_PENDIENTE", reserva.getEstado());
        assertEquals("REEMBOLSO_EN_PROCESO", pago.getEstado());
        verify(reservaRepository, never()).save(reserva);
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void divideMontoYAsignaRestoAlTitular() {
        reserva.setPrecioTotal(10_001);
        when(invitacionReservaRepository.save(any(InvitacionReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(participanteReservaRepository.save(any(ParticipanteReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipanteReserva titularCompartido =
                reservaService.crearInvitacionCompartida(reserva, titular, 3);

        assertEquals(3_335, titularCompartido.getMontoCuota());
        assertEquals(3, titularCompartido.getInvitacion().getMaxParticipantes());
        assertEquals(32, titularCompartido.getInvitacion().getCodigo().length());
    }

    @Test
    void confirmaLaReservaCuandoTodosHanPagado() {
        reserva.setEstado("PENDIENTE");
        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo("invitation-code");
        invitacion.setMaxParticipantes(2);
        ParticipanteReserva titularCompartido = new ParticipanteReserva();
        titularCompartido.setId(11L);
        titularCompartido.setInvitacion(invitacion);
        titularCompartido.setUsuario(titular);
        titularCompartido.setMontoCuota(5_000);
        titularCompartido.setEstado("PENDIENTE");
        titularCompartido.setBuyOrder("order-1");
        titularCompartido.setTokenWs("token-1");
        ParticipanteReserva invitado = new ParticipanteReserva();
        invitado.setId(12L);
        invitado.setInvitacion(invitacion);
        invitado.setUsuario(new Usuario("Invitado", "invitado@canchasya.cl", "922222222", "pass", "CLIENTE", null));
        invitado.setMontoCuota(5_000);
        invitado.setEstado("PAGADO");

        when(invitacionReservaRepository.findByCodigoForUpdate("invitation-code"))
                .thenReturn(Optional.of(invitacion));
        when(participanteReservaRepository.save(titularCompartido)).thenReturn(titularCompartido);
        when(participanteReservaRepository.findByInvitacionOrderByIdAsc(invitacion))
                .thenReturn(java.util.List.of(titularCompartido, invitado));

        boolean confirmada = reservaService.confirmarPagoParticipante(
                titularCompartido, "order-1", "token-1", 5_000);

        assertTrue(confirmada);
        assertEquals("CONFIRMADA", reserva.getEstado());
        assertEquals("PAGADO", titularCompartido.getEstado());
        verify(invitacionReservaRepository).save(invitacion);
        verify(emailService).enviarConfirmacionReserva(reserva);
    }

    @Test
    void prepararPagoParticipantePermiteReintentoConTokenWsPrevio() {
        reserva.setEstado("PENDIENTE");
        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setId(20L);
        participante.setInvitacion(invitacion);
        participante.setUsuario(titular);
        participante.setEstado("PENDIENTE");
        participante.setTokenWs("stale-webpay-token");
        participante.setBuyOrder("OLD-ORDER");

        when(participanteReservaRepository.save(participante)).thenReturn(participante);

        String nuevoBuyOrder = reservaService.prepararPagoParticipante(participante);

        assertNotNull(nuevoBuyOrder);
        assertTrue(nuevoBuyOrder.startsWith("P"));
        assertNotEquals("OLD-ORDER", nuevoBuyOrder);
        assertEquals(nuevoBuyOrder, participante.getBuyOrder());
        assertNull(participante.getTokenWs());
        verify(participanteReservaRepository).save(participante);
    }

    @Test
    void cancelarReservaConDetalleReintentaReembolsoEnProcesoDesdeCancelacionPendiente() throws Exception {
        reserva.setEstado("CANCELACION_PENDIENTE");
        reserva.setPorcentajeReembolsoAplicado(100);
        Pago pago = pagoPagado();
        pago.setEstado("REEMBOLSO_EN_PROCESO");

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 100)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.iniciarReembolso(1L, 10_000)).thenReturn(1);

        ReservaService.ResultadoCancelacion resultado = reservaService.cancelarReservaConDetalle(10L, titular);

        assertTrue(resultado.cancelada());
        assertTrue(resultado.huboReembolso());
        assertEquals("CANCELADA", reserva.getEstado());
        assertEquals("REEMBOLSADO", pago.getEstado());
        assertEquals(10_000, pago.getMontoReembolsado());
        verify(webpayService).reembolsar("token-webpay", 10_000);
        verify(pagoRepository).save(pago);
    }

    @Test
    void cancelarReservaConDetalleOmiteWebpaySiTransaccionIdEsNuloOBlanco() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusHours(48);
        reserva.setFecha(inicio.toLocalDate());
        reserva.setHoraInicio(inicio.toLocalTime());
        Pago pago = pagoPagado();
        pago.setTransaccionId("   ");

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 100)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.iniciarReembolso(1L, 10_000)).thenReturn(1);

        ReservaService.ResultadoCancelacion resultado = reservaService.cancelarReservaConDetalle(10L, titular);

        assertTrue(resultado.cancelada());
        assertEquals("CANCELADA", reserva.getEstado());
        assertEquals("REEMBOLSADO", pago.getEstado());
        verifyNoInteractions(webpayService);
    }

    @Test
    void cancelarReservaConDetalleRetornaHuboReembolsoFalseSinPagosReembolsados() {
        reserva.setEstado("PENDIENTE");
        LocalDateTime inicio = LocalDateTime.now().plusHours(48);
        reserva.setFecha(inicio.toLocalDate());
        reserva.setHoraInicio(inicio.toLocalTime());

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.iniciarCancelacion(10L, 100)).thenReturn(1);
        when(reservaRepository.cambiarEstadoCancelacion(10L, "CANCELACION_EN_PROCESO", "CANCELADA"))
                .thenReturn(1);
        when(invitacionReservaRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.empty());

        ReservaService.ResultadoCancelacion resultado = reservaService.cancelarReservaConDetalle(10L, titular);

        assertTrue(resultado.cancelada());
        assertFalse(resultado.huboReembolso());
        assertEquals("CANCELADA", reserva.getEstado());
        verifyNoInteractions(webpayService);
    }

    @Test
    void unirseAReservaRechazaInvitacionInactivaOExpirada() {
        reserva.setEstado("PENDIENTE");
        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo("code-1");
        invitacion.setMaxParticipantes(2);
        invitacion.setActiva(false);

        Usuario invitado = new Usuario("Invitado", "inv@canchasya.cl", "922222222", "pass", "CLIENTE", null);
        invitado.setId(2L);

        when(invitacionReservaRepository.findByCodigoForUpdate("code-1")).thenReturn(Optional.of(invitacion));

        assertThrows(IllegalStateException.class, () -> reservaService.unirseAReserva("code-1", invitado));

        invitacion.setActiva(true);
        LocalDateTime pasada = LocalDateTime.now(Reserva.ZONA_SANTIAGO).minusHours(1);
        reserva.setFecha(pasada.toLocalDate());
        reserva.setHoraInicio(pasada.toLocalTime());

        assertThrows(IllegalStateException.class, () -> reservaService.unirseAReserva("code-1", invitado));
    }

    @Test
    void unirseAReservaRechazaCuandoCapacidadEstaCompleta() {
        reserva.setEstado("PENDIENTE");
        LocalDateTime futura = LocalDateTime.now(Reserva.ZONA_SANTIAGO).plusHours(5);
        reserva.setFecha(futura.toLocalDate());
        reserva.setHoraInicio(futura.toLocalTime());

        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo("code-full");
        invitacion.setMaxParticipantes(2);
        invitacion.setActiva(true);

        Usuario invitado = new Usuario("Invitado", "inv@canchasya.cl", "922222222", "pass", "CLIENTE", null);
        invitado.setId(2L);

        when(invitacionReservaRepository.findByCodigoForUpdate("code-full")).thenReturn(Optional.of(invitacion));
        when(participanteReservaRepository.findByInvitacionAndUsuario(invitacion, invitado))
                .thenReturn(Optional.empty());
        when(participanteReservaRepository.countByInvitacion(invitacion)).thenReturn(2L);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> reservaService.unirseAReserva("code-full", invitado));
        assertTrue(error.getMessage().contains("máximo de participantes"));
        verify(participanteReservaRepository, never()).save(any(ParticipanteReserva.class));
    }

    @Test
    void unirseAReservaRetornaParticipantePendienteExistenteEnReingreso() {
        reserva.setEstado("PENDIENTE");
        LocalDateTime futura = LocalDateTime.now(Reserva.ZONA_SANTIAGO).plusHours(5);
        reserva.setFecha(futura.toLocalDate());
        reserva.setHoraInicio(futura.toLocalTime());

        InvitacionReserva invitacion = new InvitacionReserva();
        invitacion.setReserva(reserva);
        invitacion.setCodigo("code-dup");
        invitacion.setMaxParticipantes(2);
        invitacion.setActiva(true);

        Usuario invitado = new Usuario("Invitado", "inv@canchasya.cl", "922222222", "pass", "CLIENTE", null);
        invitado.setId(2L);

        ParticipanteReserva existente = new ParticipanteReserva();
        existente.setId(55L);
        existente.setInvitacion(invitacion);
        existente.setUsuario(invitado);
        existente.setEstado("PENDIENTE");

        when(invitacionReservaRepository.findByCodigoForUpdate("code-dup")).thenReturn(Optional.of(invitacion));
        when(participanteReservaRepository.findByInvitacionAndUsuario(invitacion, invitado))
                .thenReturn(Optional.of(existente));

        ParticipanteReserva resultado = reservaService.unirseAReserva("code-dup", invitado);

        assertSame(existente, resultado);
        verify(participanteReservaRepository, never()).save(any(ParticipanteReserva.class));
    }

    private Pago pagoPagado() {
        Pago pago = new Pago();
        pago.setId(1L);
        pago.setReserva(reserva);
        pago.setMonto(10_000);
        pago.setEstado("PAGADO");
        pago.setTransaccionId("token-webpay");
        return pago;
    }
}
