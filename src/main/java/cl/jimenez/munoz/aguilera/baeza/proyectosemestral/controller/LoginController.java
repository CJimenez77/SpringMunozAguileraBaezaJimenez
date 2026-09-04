package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        // Si ya está logueado, redirige directo a la lista de usuarios
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/usuarios";
        }
        return "login/login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            if (usuario.getPassword() != null && usuario.getPassword().equals(password)) {

                if (Boolean.FALSE.equals(usuario.getActivo())) {
                    model.addAttribute("error", "Tu cuenta se encuentra desactivada.");
                    return "login/login";
                }

                session.setAttribute("usuarioLogueado", usuario);

                return "redirect:/usuarios";
            }
        }

        model.addAttribute("error", "Correo o contraseña incorrectos.");
        return "login/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}