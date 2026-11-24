package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.InvitacionReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ParticipanteReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReservaService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private WebpayService webpayService;
    @Mock private InvitacionReservaRepository invitacionReservaRepository;
    @Mock private ParticipanteReservaRepository participanteReservaRepository;
    @Mock private ReservaService reservaService;

    @InjectMocks
    private PagoController pagoController;

    private Usuario titular;
    private Usuario ajeno;
    private Pago pago;

    @BeforeEach
    void setUp() {
        titular = new Usuario("Titular", "titular@canchasya.cl", "911111111", "pass", "CLIENTE", null);
        titular.setId(1L);

        ajeno = new Usuario("Ajeno", "ajeno@canchasya.cl", "933333333", "pass", "CLIENTE", null);
        ajeno.setId(99L);

        Reserva reserva = new Reserva();
        reserva.setId(10L);
        reserva.setUsuario(titular);

        pago = new Pago();
        pago.setId(50L);
        pago.setReserva(reserva);
        pago.setMonto(10_000);
        pago.setEstado("PAGADO");
    }

    @Test
    void comprobanteRechazaUsuarioAjeno() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", ajeno);
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));
        when(invitacionReservaRepository.findByReserva(pago.getReserva())).thenReturn(Optional.empty());

        String vista = pagoController.comprobante(50L, session, model, redirectAttributes);

        assertEquals("redirect:/reservas/mis-reservas", vista);
        assertEquals("No tienes permisos para ver este comprobante.",
                redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    void comprobantePermiteAccesoATitularDeLaReserva() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", titular);
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));
        when(invitacionReservaRepository.findByReserva(pago.getReserva())).thenReturn(Optional.empty());

        String vista = pagoController.comprobante(50L, session, model, redirectAttributes);

        assertEquals("pagos/comprobante", vista);
        assertSame(pago, model.getAttribute("pago"));
        assertSame(titular, model.getAttribute("usuarioLogueado"));
    }
}
