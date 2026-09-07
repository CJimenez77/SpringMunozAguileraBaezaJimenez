package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.WebpaySandboxService;
import jakarta.servlet.http.HttpSession;
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
    private final WebpaySandboxService webpaySandboxService;

    public PagoController(ReservaRepository reservaRepository,
                          PagoRepository pagoRepository,
                          WebpaySandboxService webpaySandboxService) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.webpaySandboxService = webpaySandboxService;
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

        int totalPagar = reserva.getPrecioTotal();
        if (grupo != null && !grupo.isEmpty()) {
            List<Reserva> grupoReservas = reservaRepository.findByGrupoRecurrente(grupo);
            totalPagar = grupoReservas.stream().mapToInt(Reserva::getPrecioTotal).sum();
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("totalPagar", totalPagar);
        model.addAttribute("grupo", grupo);
        model.addAttribute("codigoComercio", WebpaySandboxService.CODIGO_COMERCIO);
        model.addAttribute("usuarioLogueado", usuario);

        return "pagos/webpay";
    }

    @PostMapping("/webpay/procesar")
    public String procesarWebpay(@RequestParam("reservaId") Long reservaId,
                                 @RequestParam(value = "grupo", required = false) String grupo,
                                 @RequestParam("numeroTarjeta") String numeroTarjeta,
                                 @RequestParam("mesVencimiento") String mesVencimiento,
                                 @RequestParam("anioVencimiento") String anioVencimiento,
                                 @RequestParam("cvv") String cvv,
                                 @RequestParam(value = "tipoTarjeta", defaultValue = "Crédito / Débito") String tipoTarjeta,
                                 RedirectAttributes redirectAttributes) {

        Pago pago = webpaySandboxService.procesarPagoWebpay(reservaId, numeroTarjeta, mesVencimiento, anioVencimiento, cvv, tipoTarjeta);
        if (pago == null) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la transacción.");
            return "redirect:/reservas/mis-reservas";
        }

        if ("PAGADO".equals(pago.getEstado())) {
            if (grupo != null && !grupo.isEmpty()) {
                List<Reserva> grupoReservas = reservaRepository.findByGrupoRecurrente(grupo);
                for (Reserva r : grupoReservas) {
                    r.setEstado("CONFIRMADA");
                    reservaRepository.save(r);
                }
            }
            return "redirect:/pagos/comprobante/" + pago.getId();
        } else {
            redirectAttributes.addFlashAttribute("errorPago", "Transacción rechazada por el banco emisor. Por favor verifica los datos o usa otra tarjeta de prueba.");
            return "redirect:/pagos/webpay/" + reservaId + (grupo != null ? "?grupo=" + grupo : "");
        }
    }

    @PostMapping("/transferencia/procesar")
    public String procesarTransferencia(@RequestParam("reservaId") Long reservaId,
                                        @RequestParam(value = "grupo", required = false) String grupo,
                                        @RequestParam("bancoOrigen") String bancoOrigen,
                                        @RequestParam("comprobanteNumero") String comprobanteNumero,
                                        RedirectAttributes redirectAttributes) {

        Pago pago = webpaySandboxService.simularTransferencia(reservaId, bancoOrigen, comprobanteNumero);
        if (pago == null) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la transferencia.");
            return "redirect:/reservas/mis-reservas";
        }

        if (grupo != null && !grupo.isEmpty()) {
            List<Reserva> grupoReservas = reservaRepository.findByGrupoRecurrente(grupo);
            for (Reserva r : grupoReservas) {
                r.setEstado("CONFIRMADA");
                reservaRepository.save(r);
            }
        }

        return "redirect:/pagos/comprobante/" + pago.getId();
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
        model.addAttribute("codigoComercio", WebpaySandboxService.CODIGO_COMERCIO);
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
