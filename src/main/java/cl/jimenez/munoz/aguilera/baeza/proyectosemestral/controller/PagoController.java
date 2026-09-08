package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpayService;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final WebpayService webpayService;

    @Value("${transbank.webpay.commerce-code:597055555532}")
    private String codigoComercio;

    public PagoController(ReservaRepository reservaRepository,
                          PagoRepository pagoRepository,
                          WebpayService webpayService) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.webpayService = webpayService;
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

        try {
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
    public String comprobante(@PathVariable("pagoId") Long pagoId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Pago pago = pagoRepository.findById(pagoId).orElse(null);
        if (pago == null) {
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
        model.addAttribute("usuarioLogueado", usuario);
        return "pagos/historial";
    }
}
