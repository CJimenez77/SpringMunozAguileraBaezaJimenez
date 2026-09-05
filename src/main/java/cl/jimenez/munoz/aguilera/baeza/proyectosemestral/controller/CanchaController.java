package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/canchas")
public class CanchaController {

    private final CanchaRepository canchaRepository;
    private final ComplejoRepositorio complejoRepositorio;

    public CanchaController(CanchaRepository canchaRepository, ComplejoRepositorio complejoRepositorio) {
        this.canchaRepository = canchaRepository;
        this.complejoRepositorio = complejoRepositorio;
    }

    @GetMapping
    public String listarCanchas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        List<Cancha> canchas;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            canchas = canchaRepository.findByDuenoAndActivoTrue(usuario);
        } else {
            canchas = canchaRepository.findByActivoTrue();
        }

        model.addAttribute("canchas", canchas);
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/lista";
    }

    @GetMapping("/complejo/{idComplejo}")
    public String listarCanchasPorComplejo(@PathVariable("idComplejo") Long idComplejo, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        Complejo complejo = complejoRepositorio.findById(idComplejo).orElse(null);
        if (complejo == null || !complejo.isActivo()) {
            return "redirect:/canchas";
        }

        if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                (complejo.getDueno() == null || !complejo.getDueno().getId().equals(usuario.getId()))) {
            return "redirect:/canchas";
        }

        model.addAttribute("canchas", canchaRepository.findByComplejoIdAndActivoTrue(idComplejo));
        model.addAttribute("complejoFiltro", complejo);
        model.addAttribute("usuarioLogueado", usuario);
        return "canchas/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        List<Complejo> complejosDisponibles;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            complejosDisponibles = complejoRepositorio.findByDuenoAndActivoTrue(usuario);
        } else {
            complejosDisponibles = complejoRepositorio.findByActivoTrue();
        }

        model.addAttribute("cancha", new Cancha());
        model.addAttribute("complejos", complejosDisponibles);
        return "canchas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarCancha(@ModelAttribute Cancha cancha, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        if (cancha.getComplejo() != null) {
            Complejo comp = complejoRepositorio.findById(cancha.getComplejo().getId_complejo()).orElse(null);
            if (comp == null || !comp.isActivo()) return "redirect:/canchas";
            if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                    (comp.getDueno() == null || !comp.getDueno().getId().equals(usuario.getId()))) {
                return "redirect:/canchas";
            }
            cancha.setComplejo(comp);
        }

        if (cancha.getId_cancha() != null) {
            Cancha existente = canchaRepository.findById(cancha.getId_cancha()).orElse(null);
            if (existente != null) {
                if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                        (existente.getComplejo().getDueno() == null || !existente.getComplejo().getDueno().getId().equals(usuario.getId()))) {
                    return "redirect:/canchas";
                }
                existente.setNombre_cancha(cancha.getNombre_cancha());
                existente.setTipo_cancha(cancha.getTipo_cancha());
                existente.setPrecio_hora(cancha.getPrecio_hora());
                existente.setComplejo(cancha.getComplejo());
                canchaRepository.save(existente);
                return "redirect:/canchas";
            }
        }

        cancha.setActivo(true);
        canchaRepository.save(cancha);
        return "redirect:/canchas";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null || !cancha.isActivo()) {
            return "redirect:/canchas";
        }

        if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                (cancha.getComplejo().getDueno() == null || !cancha.getComplejo().getDueno().getId().equals(usuario.getId()))) {
            return "redirect:/canchas";
        }

        List<Complejo> complejosDisponibles;
        if ("ADMIN_COMPLEJO".equals(usuario.getRol())) {
            complejosDisponibles = complejoRepositorio.findByDuenoAndActivoTrue(usuario);
        } else {
            complejosDisponibles = complejoRepositorio.findByActivoTrue();
        }

        model.addAttribute("cancha", cancha);
        model.addAttribute("complejos", complejosDisponibles);
        return "canchas/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCancha(@PathVariable("id") Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || "CLIENTE".equals(usuario.getRol())) {
            return "redirect:/canchas";
        }

        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha != null) {
            if ("ADMIN_COMPLEJO".equals(usuario.getRol()) &&
                    (cancha.getComplejo().getDueno() == null || !cancha.getComplejo().getDueno().getId().equals(usuario.getId()))) {
                return "redirect:/canchas";
            }
            cancha.setActivo(false);
            canchaRepository.save(cancha);
        }
        return "redirect:/canchas";
    }
}