package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/complejos")
public class ComplejoController {

    private final ComplejoRepositorio complejoRepositorio;

    public ComplejoController(ComplejoRepositorio complejoRepositorio) {
        this.complejoRepositorio = complejoRepositorio;
    }

    @GetMapping
    public String listarComplejos(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) {
            return "redirect:/login";
        }
        List<Complejo> lista;
        if ("ADMIN_COMPLEJO".equals(logueado.getRol())) {
            lista = complejoRepositorio.findByDuenoAndActivoTrue(logueado);
        } else {
            lista = complejoRepositorio.findByActivoTrue();
        }
        model.addAttribute("complejos", lista);
        model.addAttribute("usuarioLogueado", logueado);
        return "complejos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        model.addAttribute("complejo", new Complejo());
        return "complejos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarComplejo(@ModelAttribute Complejo complejo, HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/dashboard";
        }

        if (complejo.getId_complejo() != null) {
            Complejo existente = complejoRepositorio.findById(complejo.getId_complejo()).orElse(null);
            if (existente == null) {
                return "redirect:/complejos";
            }
            if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                    (existente.getDueno() == null || !logueado.getId().equals(existente.getDueno().getId()))) {
                return "redirect:/complejos";
            }
            existente.setNombre_complejo(complejo.getNombre_complejo());
            existente.setDireccion_complejo(complejo.getDireccion_complejo());
            existente.setUbicacionMapa(complejo.getUbicacionMapa());
            complejoRepositorio.save(existente);
            return "redirect:/complejos";
        } else {
            complejo.setDueno(logueado);
            complejo.setActivo(true);
            complejoRepositorio.save(complejo);
        }
        return "redirect:/complejos";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/complejos";
        }
        if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
            return "redirect:/complejos";
        }
        model.addAttribute("complejo", complejo);
        return "complejos/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarComplejo(@PathVariable("id") Long id, HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null || "CLIENTE".equals(logueado.getRol())) {
            return "redirect:/complejos";
        }
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo != null) {
            if ("ADMIN_COMPLEJO".equals(logueado.getRol()) &&
                    (complejo.getDueno() == null || !logueado.getId().equals(complejo.getDueno().getId()))) {
                return "redirect:/complejos";
            }
            complejo.setActivo(false);
            complejoRepositorio.save(complejo);
        }
        return "redirect:/complejos";
    }
}