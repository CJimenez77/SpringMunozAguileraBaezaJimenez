package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cupon;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.CuponService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/cupones")
public class CuponController {

    private final CuponService cuponService;

    public CuponController(CuponService cuponService) {
        this.cuponService = cuponService;
    }

    @GetMapping
    public String panelCupones(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        if ("CLIENTE".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }

        model.addAttribute("cupones", cuponService.listarTodos());
        model.addAttribute("usuarioLogueado", usuario);
        return "cupones/gestion";
    }

    @PostMapping("/crear")
    public String crearCupon(@RequestParam("codigo") String codigo,
                             @RequestParam("porcentaje") int porcentaje,
                             @RequestParam("diasDuracion") int diasDuracion,
                             @RequestParam(value = "limiteUsos", required = false) Integer limiteUsos,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        if ("CLIENTE".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }

        try {
            Cupon cupon = cuponService.crearCupon(codigo, porcentaje, diasDuracion, limiteUsos, usuario);
            redirectAttributes.addFlashAttribute("exito", "Cupón " + cupon.getCodigo() + " generado exitosamente (" + cupon.getPorcentaje() + "% de descuento).");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cupones";
    }

    @PostMapping("/alternar/{id}")
    public String alternarCupon(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        if ("CLIENTE".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }

        cuponService.alternarEstado(id);
        redirectAttributes.addFlashAttribute("exito", "Estado del cupón actualizado.");
        return "redirect:/cupones";
    }

    @GetMapping("/api/validar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiValidarCupon(@RequestParam("codigo") String codigo) {
        Map<String, Object> respuesta = new HashMap<>();
        Optional<Cupon> cuponOpt = cuponService.validarCupon(codigo);

        if (cuponOpt.isPresent()) {
            Cupon c = cuponOpt.get();
            respuesta.put("valido", true);
            respuesta.put("codigo", c.getCodigo());
            respuesta.put("porcentaje", c.getPorcentaje());
            respuesta.put("mensaje", "¡Cupón " + c.getCodigo() + " aplicado! (" + c.getPorcentaje() + "% de descuento)");
            return ResponseEntity.ok(respuesta);
        } else {
            respuesta.put("valido", false);
            respuesta.put("mensaje", "El código ingresado no existe o no se encuentra vigente.");
            return ResponseEntity.ok(respuesta);
        }
    }
}
