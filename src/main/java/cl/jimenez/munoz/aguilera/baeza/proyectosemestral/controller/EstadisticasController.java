package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.EstadisticasService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping
    public String verEstadisticas(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        response.setHeader("Cache-Control", "no-store");
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        if (!usuario.isActivo() || !("ADMIN_SISTEMA".equals(usuario.getRol()) || "ADMIN_COMPLEJO".equals(usuario.getRol()))) {
            return "redirect:/dashboard";
        }
        desde = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        hasta = hasta == null ? LocalDate.now() : hasta;
        try {
            var datos = estadisticasService.generar(usuario, desde, hasta);
            model.addAttribute("estadisticas", datos);
            model.addAttribute("demandaPorHora", datos.demandaPorHora());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/estadisticas";
        }
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "estadisticas/panel";
    }
}
