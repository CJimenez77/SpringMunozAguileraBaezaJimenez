package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Complejo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
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
    public String listarComplejos(Model model) {
        model.addAttribute("complejos", complejoRepositorio.findAll());
        return "complejos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("complejo", new Complejo());
        return "complejos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarComplejo(@ModelAttribute Complejo complejo) {
        complejoRepositorio.save(complejo);
        return "redirect:/complejos";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Complejo complejo = complejoRepositorio.findById(id).orElse(null);
        if (complejo == null) {
            return "redirect:/complejos";
        }
        model.addAttribute("complejo", complejo);
        return "complejos/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarComplejo(@PathVariable("id") Long id) {
        complejoRepositorio.deleteById(id);
        return "redirect:/complejos";
    }
}
