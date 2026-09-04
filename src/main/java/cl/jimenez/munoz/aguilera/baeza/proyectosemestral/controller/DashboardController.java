package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CanchaRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ComplejoRepositorio;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UsuarioRepository usuarioRepository;
    private final ComplejoRepositorio complejoRepositorio;
    private final CanchaRepository canchaRepository;

    public DashboardController(UsuarioRepository usuarioRepository,
                               ComplejoRepositorio complejoRepositorio,
                               CanchaRepository canchaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.complejoRepositorio = complejoRepositorio;
        this.canchaRepository = canchaRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalComplejos", complejoRepositorio.count());
        model.addAttribute("totalCanchas", canchaRepository.count());

        return "dashboard";
    }
}