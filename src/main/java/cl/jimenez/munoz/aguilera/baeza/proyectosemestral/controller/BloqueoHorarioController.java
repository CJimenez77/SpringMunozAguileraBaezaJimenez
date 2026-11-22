package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller.form.BloqueoHorarioForm;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.BloqueoHorarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/canchas/{id}/bloqueos")
public class BloqueoHorarioController {

    private static final String ATRIBUTO_CSRF = BloqueoHorarioController.class.getName() + ".csrf";
    private final BloqueoHorarioService bloqueoService;

    public BloqueoHorarioController(BloqueoHorarioService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @GetMapping
    public String listar(@PathVariable("id") Long id, HttpSession session, Model model,
                         RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        try {
            Cancha cancha = bloqueoService.obtenerCanchaAdministrable(id, usuario);
            BloqueoHorarioForm formulario = new BloqueoHorarioForm();
            formulario.setFechaInicio(LocalDate.now());
            formulario.setFechaFin(LocalDate.now());
            model.addAttribute("nuevoBloqueo", formulario);
            return mostrar(cancha, model, session);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/canchas";
        }
    }

    @PostMapping
    public String crear(@PathVariable("id") Long id,
                        @Valid @ModelAttribute("nuevoBloqueo") BloqueoHorarioForm formulario,
                        BindingResult errores,
                        @RequestParam(value = "tokenCsrf", required = false) String tokenCsrf,
                        HttpSession session, Model model,
                        RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        validarTokenCsrf(session, tokenCsrf);
        Cancha cancha;
        try {
            cancha = bloqueoService.obtenerCanchaAdministrable(id, usuario);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/canchas";
        }
        if (!errores.hasErrors()) {
            try {
                bloqueoService.crear(id, usuario, formulario.getFechaInicio(), formulario.getFechaFin(),
                        formulario.isDiaCompleto(), formulario.getHoraInicio(), formulario.getHoraFin(), formulario.getMotivo());
                redirectAttributes.addFlashAttribute("exito", "Bloqueo registrado. El horario ya no admite reservas.");
                return "redirect:/canchas/" + id + "/bloqueos";
            } catch (IllegalArgumentException e) {
                errores.reject("bloqueo", e.getMessage());
            }
        }
        return mostrar(cancha, model, session);
    }

    @PostMapping("/{bloqueoId}/eliminar")
    public String eliminar(@PathVariable("id") Long id, @PathVariable("bloqueoId") Long bloqueoId,
                           @RequestParam(value = "tokenCsrf", required = false) String tokenCsrf,
                           HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        validarTokenCsrf(session, tokenCsrf);
        try {
            bloqueoService.eliminar(id, bloqueoId, usuario);
            redirectAttributes.addFlashAttribute("exito", "Bloqueo eliminado. Se restableció la disponibilidad del horario.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/canchas/" + id + "/bloqueos";
    }

    private String mostrar(Cancha cancha, Model model, HttpSession session) {
        model.addAttribute("cancha", cancha);
        model.addAttribute("bloqueos", bloqueoService.listar(cancha));
        model.addAttribute("fechaHoy", LocalDate.now());
        model.addAttribute("tokenCsrf", obtenerTokenCsrf(session));
        return "canchas/bloqueos";
    }

    private String obtenerTokenCsrf(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(ATRIBUTO_CSRF);
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute(ATRIBUTO_CSRF, token);
            }
            return token;
        }
    }

    private void validarTokenCsrf(HttpSession session, String recibido) {
        String esperado = (String) session.getAttribute(ATRIBUTO_CSRF);
        if (esperado == null || recibido == null || recibido.length() != esperado.length()
                || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La solicitud no es válida. Recarga la página de bloqueos e inténtalo nuevamente.");
        }
    }
}
