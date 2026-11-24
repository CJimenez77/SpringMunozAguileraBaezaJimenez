package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.ParticipanteReserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ParticipanteReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.EmailService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.ReservaService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class WebpayPagoController {

    private static final Logger log = LoggerFactory.getLogger(WebpayPagoController.class);

    private final WebpayService webpayService;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final EmailService emailService;
    private final ParticipanteReservaRepository participanteReservaRepository;
    private final ReservaService reservaService;

    public WebpayPagoController(WebpayService webpayService,
                                ReservaRepository reservaRepository,
                                PagoRepository pagoRepository,
                                EmailService emailService,
                                ParticipanteReservaRepository participanteReservaRepository,
                                ReservaService reservaService) {
        this.webpayService = webpayService;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.emailService = emailService;
        this.participanteReservaRepository = participanteReservaRepository;
        this.reservaService = reservaService;
    }

    @RequestMapping(value = {"/reservas/pagar", "/reservas/pagar/{idReserva}"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String pagarReserva(@RequestParam(value = "idReserva", required = false) Long idReservaParam,
                               @PathVariable(value = "idReserva", required = false) Long idReservaPath,
                               jakarta.servlet.http.HttpSession session) {
        Long idReserva = idReservaParam != null ? idReservaParam : idReservaPath;
        if (idReserva == null) {
            return "redirect:/reservas/mis-reservas";
        }
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/login";
        }
        return "redirect:/pagos/webpay/" + idReserva;
    }

    @RequestMapping(value = "/reservas/webpay-retorno", method = {RequestMethod.GET, RequestMethod.POST})
    public String retornoWebpay(@RequestParam(value = "token_ws", required = false) String tokenWs,
                                @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
                                Model model) {
        if (tokenWs == null) {
            if (tbkToken != null) {
                reservaService.limpiarIntentoPagoParticipante(tbkToken);
            }
            model.addAttribute("mensaje", "La transacción fue anulada por el usuario.");
            return "reservas/pago_fallido";
        }

        try {
            ParticipanteReserva participante = participanteReservaRepository.findByTokenWs(tokenWs).orElse(null);
            WebpayPlusTransactionCommitResponse response = webpayService.confirmarPago(tokenWs);

            if (response.getResponseCode() == 0) {
                if (participante != null) {
                    boolean reservaConfirmada = reservaService.confirmarPagoParticipante(
                            participante, response.getBuyOrder(), tokenWs, response.getAmount());
                    model.addAttribute("response", response);
                    model.addAttribute("reserva", participante.getInvitacion().getReserva());
                    model.addAttribute("participanteActual", participante);
                    if ("REEMBOLSADO".equals(participante.getEstado())
                            || "REEMBOLSO_PARCIAL".equals(participante.getEstado())) {
                        model.addAttribute("mensaje", "La reserva ya estaba cancelada; se procesó el reembolso correspondiente a la política.");
                        return "reservas/pago_fallido";
                    }
                    if (reservaConfirmada) {
                        model.addAttribute("participanteActual", participante);
                        return "reservas/pago_exitoso";
                    }
                    return "reservas/pago_compartido_pendiente";
                }

                Long idReserva = obtenerIdReserva(response.getBuyOrder(), response.getSessionId());

                Reserva reserva = idReserva == null ? null : reservaRepository.findById(idReserva).orElse(null);
                if (reserva != null) {
                    if ("CANCELADA".equals(reserva.getEstado())
                            || reserva.getEstado().startsWith("CANCELACION_")) {
                        Pago pagoExistente = pagoRepository.findByReserva(reserva).orElse(null);
                        if (pagoExistente != null && tokenWs.equals(pagoExistente.getTransaccionId())
                                && ("REEMBOLSADO".equals(pagoExistente.getEstado())
                                || "REEMBOLSO_PARCIAL".equals(pagoExistente.getEstado())
                                || "REEMBOLSO_EN_PROCESO".equals(pagoExistente.getEstado()))) {
                            model.addAttribute("mensaje", "El reembolso de esta transacción ya se procesó o está pendiente de conciliación.");
                            return "reservas/pago_fallido";
                        }
                        int montoReembolso = (int) Math.floor((long) response.getAmount()
                                * reserva.getPorcentajeReembolsoAplicado() / 100.0);
                        if (montoReembolso > 0) {
                            webpayService.reembolsar(tokenWs, montoReembolso);
                        }
                        Pago pago = pagoExistente == null ? new Pago() : pagoExistente;
                        pago.setReserva(reserva);
                        pago.setMonto((int) response.getAmount());
                        pago.setMontoReembolsado(montoReembolso);
                        pago.setMetodoPago("WEBPAY_PLUS");
                        pago.setEstado(reserva.getPorcentajeReembolsoAplicado() == 100
                                ? "REEMBOLSADO" : "REEMBOLSO_PARCIAL");
                        pago.setFechaPago(LocalDateTime.now());
                        pago.setCodigoAutorizacion(response.getAuthorizationCode());
                        pago.setTransaccionId(tokenWs);
                        pagoRepository.save(pago);
                        model.addAttribute("mensaje", "La reserva ya estaba cancelada; se reversó el monto correspondiente a la política de cancelación.");
                        return "reservas/pago_fallido";
                    }

                    reserva.setEstado("CONFIRMADA");
                    reservaRepository.save(reserva);

                    Pago pago = pagoRepository.findByReserva(reserva).orElse(new Pago());
                    pago.setReserva(reserva);
                    pago.setMonto((int) response.getAmount());
                    pago.setMetodoPago("WEBPAY_PLUS");
                    pago.setEstado("PAGADO");
                    pago.setFechaPago(LocalDateTime.now());
                    pago.setCodigoAutorizacion(response.getAuthorizationCode());
                    pago.setTransaccionId(tokenWs);
                    pago.setUltimosDigitos(response.getCardDetail() != null ? response.getCardDetail().getCardNumber() : "----");
                    pago.setTipoTarjeta(response.getPaymentTypeCode());
                    pagoRepository.save(pago);

                    try {
                        emailService.enviarConfirmacionReserva(reserva);
                    } catch (Exception mailEx) {
                        log.warn("Pago confirmado, pero no se pudo enviar el correo para reserva {}", reserva.getId(), mailEx);
                    }

                }

                model.addAttribute("response", response);
                return "reservas/pago_exitoso";
            } else {
                if (participanteReservaRepository.findByTokenWs(tokenWs).isPresent()) {
                    reservaService.limpiarIntentoPagoParticipante(tokenWs);
                }
                model.addAttribute("mensaje", "Pago rechazado por el banco (Código: " + response.getResponseCode() + ").");
                return "reservas/pago_fallido";
            }
        } catch (Exception e) {
            try {
                reservaService.limpiarIntentoPagoParticipante(tokenWs);
            } catch (Exception cleanupEx) {
                log.warn("No se pudo limpiar el intento de pago para token {}", tokenWs, cleanupEx);
            }
            log.error("Error procesando retorno de Webpay", e);
            model.addAttribute("mensaje", "Error procesando el voucher de Webpay: " + e.getMessage());
            return "reservas/pago_fallido";
        }
    }

    private Long obtenerIdReserva(String buyOrder, String sessionId) {
        if (buyOrder != null) {
            String[] partes = buyOrder.split("-");
            if (partes.length > 1) {
                try {
                    return Long.parseLong(partes[1]);
                } catch (NumberFormatException ignored) {
                    // Fall through to the session identifier supplied to Webpay.
                }
            }
        }
        if (sessionId != null && sessionId.startsWith("SES-")) {
            try {
                return Long.parseLong(sessionId.substring(4));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
