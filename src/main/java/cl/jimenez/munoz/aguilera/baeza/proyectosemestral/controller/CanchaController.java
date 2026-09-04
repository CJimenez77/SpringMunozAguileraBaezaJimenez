package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cancha;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
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
    public String listarCanchas(Model model) {
        model.addAttribute("canchas", canchaRepository.findAll());
        return "canchas/lista";
    }
    
    @GetMapping("/complejo/{idComplejo}")
    public String listarCanchasPorComplejo(@PathVariable("idComplejo") Long idComplejo, Model model) {
        model.addAttribute("canchas", canchaRepository.findByComplejoId(idComplejo));
        Complejo complejo = complejoRepositorio.findById(idComplejo).orElse(null);
        model.addAttribute("complejoFiltro", complejo);
        return "canchas/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("cancha", new Cancha());
        model.addAttribute("complejos", complejoRepositorio.findAll());
        return "canchas/formulario";
    }

    @PostMapping("/guardar")
    public String guardarCancha(@ModelAttribute Cancha cancha) {
        canchaRepository.save(cancha);
        return "redirect:/canchas";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Cancha cancha = canchaRepository.findById(id).orElse(null);
        if (cancha == null) {
            return "redirect:/canchas";
        }
        model.addAttribute("cancha", cancha);
        model.addAttribute("complejos", complejoRepositorio.findAll());
        return "canchas/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCancha(@PathVariable("id") Long id) {
        canchaRepository.deleteById(id);
        return "redirect:/canchas";
    }
}
