package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.InvitacionReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ParticipanteReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.InvitacionReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ParticipanteReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReservaService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final WebpayService webpayService;
    private final InvitacionReservaRepository invitacionReservaRepository;
    private final ParticipanteReservaRepository participanteReservaRepository;
    private final ReservaService reservaService;

    @Value("${transbank.webpay.commerce-code:597055555532}")
    private String codigoComercio;

    public PagoController(ReservaRepository reservaRepository,
                          PagoRepository pagoRepository,
                          WebpayService webpayService,
                          InvitacionReservaRepository invitacionReservaRepository,
                          ParticipanteReservaRepository participanteReservaRepository,
                          ReservaService reservaService) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.webpayService = webpayService;
        this.invitacionReservaRepository = invitacionReservaRepository;
        this.participanteReservaRepository = participanteReservaRepository;
        this.reservaService = reservaService;
    }

    @GetMapping("/webpay/{reservaId}")
    public String pantallaWebpay(@PathVariable("reservaId") Long reservaId,
                                 @RequestParam(value = "grupo", required = false) String grupo,
                                 HttpSession session,
                                 Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return "redirect:/reservas/mis-reservas";
        }
        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            return "redirect:/reservas/mis-reservas";
        }
        if (!"PENDIENTE".equals(reserva.getEstado())) {
            return "redirect:/reservas/detalle/" + reservaId;
        }

        try {
            InvitacionReserva invitacion = invitacionReservaRepository.findByReserva(reserva).orElse(null);
            if (invitacion != null) {
                ParticipanteReserva participante = participanteReservaRepository
                        .findByInvitacionAndUsuario(invitacion, usuario).orElse(null);
                if (participante == null || !"PENDIENTE".equals(participante.getEstado())) {
                    return "redirect:/reservas/detalle/" + reservaId;
                }
                String buyOrder = reservaService.prepararPagoParticipante(participante);
                WebpayPlusTransactionCreateResponse response = webpayService.iniciarPago(
                        buyOrder, "PAR-" + participante.getId(), participante.getMontoCuota());
                reservaService.registrarTokenParticipante(participante.getId(), response.getToken());
                model.addAttribute("url", response.getUrl());
                model.addAttribute("tokenWs", response.getToken());
                return "reservas/redireccion_webpay";
            }

            String buyOrder = "ORD-" + reserva.getId() + "-" + System.currentTimeMillis() % 10000;
            String sessionId = "SES-" + reserva.getId();
            double totalPagar = reserva.getPrecioTotal();
            if (grupo != null && !grupo.isEmpty()) {
                List<Reserva> grupoReservas = reservaRepository.findByGrupoRecurrente(grupo);
                totalPagar = grupoReservas.stream().mapToInt(Reserva::getPrecioTotal).sum();
            }

            WebpayPlusTransactionCreateResponse response = webpayService.iniciarPago(buyOrder, sessionId, totalPagar);

            model.addAttribute("url", response.getUrl());
            model.addAttribute("tokenWs", response.getToken());
            return "reservas/redireccion_webpay";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al conectar con Webpay: " + e.getMessage());
            return "reservas/pago_fallido";
        }
    }

    @GetMapping("/comprobante/{pagoId}")
    public String comprobante(@PathVariable("pagoId") Long pagoId,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Pago pago = pagoRepository.findById(pagoId).orElse(null);
        if (pago == null) {
            return "redirect:/reservas/mis-reservas";
        }

        Reserva reserva = pago.getReserva();
        boolean esTitular = reserva != null && reserva.getUsuario() != null
                && reserva.getUsuario().getId().equals(usuario.getId());
        boolean esAdmin = "ADMIN_SISTEMA".equals(usuario.getRol())
                || ("ADMIN_COMPLEJO".equals(usuario.getRol())
                && reserva != null && reserva.getCancha() != null
                && reserva.getCancha().getComplejo() != null
                && reserva.getCancha().getComplejo().getDueno() != null
                && reserva.getCancha().getComplejo().getDueno().getId().equals(usuario.getId()));
        boolean esParticipante = reserva != null && invitacionReservaRepository.findByReserva(reserva)
                .flatMap(inv -> participanteReservaRepository.findByInvitacionAndUsuario(inv, usuario))
                .isPresent();

        if (!esTitular && !esAdmin && !esParticipante) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para ver este comprobante.");
            return "redirect:/reservas/mis-reservas";
        }

        model.addAttribute("pago", pago);
        model.addAttribute("codigoComercio", codigoComercio);
        model.addAttribute("usuarioLogueado", usuario);

        return "pagos/comprobante";
    }

    @GetMapping("/historial")
    public String historialPagos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Pago> pagos;
        if ("ADMIN_SISTEMA".equals(usuario.getRol())) {
            pagos = pagoRepository.findAll();
        } else {
            pagos = pagoRepository.findByUsuario(usuario);
        }

        model.addAttribute("pagos", pagos);
        model.addAttribute("pagosCompartidos", "ADMIN_SISTEMA".equals(usuario.getRol())
                ? participanteReservaRepository.findHistorialCompleto()
                : participanteReservaRepository.findHistorialPorUsuario(usuario));
        model.addAttribute("usuarioLogueado", usuario);
        return "pagos/historial";
    }
}
