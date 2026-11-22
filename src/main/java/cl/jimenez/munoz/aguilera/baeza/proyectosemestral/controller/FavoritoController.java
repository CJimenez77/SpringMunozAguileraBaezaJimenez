package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ResenaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.FavoritoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final ResenaRepository resenaRepository;

    public FavoritoController(FavoritoService favoritoService, ResenaRepository resenaRepository) {
        this.favoritoService = favoritoService;
        this.resenaRepository = resenaRepository;
    }

    @GetMapping
    public String misFavoritos(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Cancha> canchasFavoritas = favoritoService.obtenerCanchasFavoritas(usuario);
        List<Complejo> complejosFavoritos = favoritoService.obtenerComplejosFavoritos(usuario);

        Map<Long, Double> promedios = new HashMap<>();
        Map<Long, Long> conteos = new HashMap<>();
        for (Cancha c : canchasFavoritas) {
            Double prom = resenaRepository.findPromedioCalificacionPorCancha(c);
            Long total = resenaRepository.countByCancha(c);
            promedios.put(c.getId_cancha(), prom != null ? Math.round(prom * 10.0) / 10.0 : 0.0);
            conteos.put(c.getId_cancha(), total != null ? total : 0L);
        }

        model.addAttribute("canchasFavoritas", canchasFavoritas);
        model.addAttribute("complejosFavoritos", complejosFavoritos);
        model.addAttribute("promedios", promedios);
        model.addAttribute("conteos", conteos);
        model.addAttribute("usuarioLogueado", usuario);
        return "favoritos/lista";
    }

    @PostMapping("/cancha/{id}")
    public String toggleCancha(@PathVariable("id") Long id,
                               @RequestParam(value = "redirect", required = false) String redirect,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            boolean agregado = favoritoService.toggleCanchaFavorita(usuario, id);
            redirectAttributes.addFlashAttribute("exito",
                    agregado ? "Cancha agregada a tus favoritos." : "Cancha eliminada de tus favoritos.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:" + resolverRutaSegura(redirect, "/favoritos");
    }

    @PostMapping("/complejo/{id}")
    public String toggleComplejo(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirect", required = false) String redirect,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            boolean agregado = favoritoService.toggleComplejoFavorito(usuario, id);
            redirectAttributes.addFlashAttribute("exito",
                    agregado ? "Complejo agregado a tus favoritos." : "Complejo eliminado de tus favoritos.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:" + resolverRutaSegura(redirect, "/favoritos");
    }

    @PostMapping("/api/cancha/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiToggleCancha(@PathVariable("id") Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión para guardar favoritos."));
        }

        try {
            boolean agregado = favoritoService.toggleCanchaFavorita(usuario, id);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("favorito", agregado);
            respuesta.put("mensaje", agregado ? "Cancha agregada a tus favoritos." : "Cancha eliminada de tus favoritos.");
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/complejo/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiToggleComplejo(@PathVariable("id") Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión para guardar favoritos."));
        }

        try {
            boolean agregado = favoritoService.toggleComplejoFavorito(usuario, id);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("favorito", agregado);
            respuesta.put("mensaje", agregado ? "Complejo agregado a tus favoritos." : "Complejo eliminado de tus favoritos.");
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String resolverRutaSegura(String redirect, String fallback) {
        if (redirect == null || redirect.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = redirect.trim();
        if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
            return trimmed;
        }
        return fallback;
    }
}
