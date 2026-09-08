package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.EmailService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class WebpayPagoController {

    private final WebpayService webpayService;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final EmailService emailService;

    public WebpayPagoController(WebpayService webpayService,
                                ReservaRepository reservaRepository,
                                PagoRepository pagoRepository,
                                EmailService emailService) {
        this.webpayService = webpayService;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.emailService = emailService;
    }

    @RequestMapping(value = {"/reservas/pagar", "/reservas/pagar/{idReserva}"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String pagarReserva(@RequestParam(value = "idReserva", required = false) Long idReservaParam,
                               @PathVariable(value = "idReserva", required = false) Long idReservaPath,
                               Model model) {
        Long idReserva = idReservaParam != null ? idReservaParam : idReservaPath;
        if (idReserva == null) {
            return "redirect:/reservas/mis-reservas";
        }
        Reserva reserva = reservaRepository.findById(idReserva).orElse(null);
        if (reserva == null) {
            return "redirect:/dashboard";
        }

        try {
            String buyOrder = "ORD-" + reserva.getId() + "-" + System.currentTimeMillis() % 10000;
            String sessionId = "SES-" + reserva.getId();
            double monto = reserva.getPrecioTotal();

            WebpayPlusTransactionCreateResponse response = webpayService.iniciarPago(buyOrder, sessionId, monto);

            model.addAttribute("url", response.getUrl());
            model.addAttribute("tokenWs", response.getToken());
            return "reservas/redireccion_webpay";
        } catch (Exception e) {
            model.addAttribute("mensaje", "No se pudo conectar con Webpay: " + e.getMessage());
            return "reservas/pago_fallido";
        }
    }

    @RequestMapping(value = "/reservas/webpay-retorno", method = {RequestMethod.GET, RequestMethod.POST})
    public String retornoWebpay(@RequestParam(value = "token_ws", required = false) String tokenWs,
                                @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
                                Model model) {
        if (tokenWs == null) {
            model.addAttribute("mensaje", "La transacción fue anulada por el usuario.");
            return "reservas/pago_fallido";
        }

        try {
            WebpayPlusTransactionCommitResponse response = webpayService.confirmarPago(tokenWs);

            if (response.getResponseCode() == 0) {
                String buyOrder = response.getBuyOrder();
                Long idReserva = null;
                try {
                    idReserva = Long.parseLong(buyOrder.split("-")[1]);
                } catch (Exception ex) {
                    if (response.getSessionId() != null && response.getSessionId().startsWith("SES-")) {
                        idReserva = Long.parseLong(response.getSessionId().substring(4));
                    }
                }

                Reserva reserva = reservaRepository.findById(idReserva).orElse(null);
                if (reserva != null) {
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
                    }
                }

                model.addAttribute("response", response);
                return "reservas/pago_exitoso";
            } else {
                model.addAttribute("mensaje", "Pago rechazado por el banco (Código: " + response.getResponseCode() + ").");
                return "reservas/pago_fallido";
            }
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error procesando el voucher de Webpay: " + e.getMessage());
            return "reservas/pago_fallido";
        }
    }
}
