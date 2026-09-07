package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/dashboard";
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

                if (!usuario.isActivo()) {
                    model.addAttribute("error", "Tu cuenta se encuentra desactivada.");
                    return "login/login";
                }

                session.setAttribute("usuarioLogueado", usuario);
                return "redirect:/dashboard";
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

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "login/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            model.addAttribute("error", "El correo ingresado ya está registrado.");
            return "login/registro";
        }
        usuario.setRol("CLIENTE");
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        return "redirect:/login?registrado";
    }

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword() {
        return "login/recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String procesarRecuperarPassword(@RequestParam("email") String email,
                                            jakarta.servlet.http.HttpServletRequest request,
                                            Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        boolean enviado = passwordResetService.solicitarRecuperacion(email, baseUrl);
        if (enviado) {
            model.addAttribute("exito", "Se ha enviado un enlace de recuperación a tu correo electrónico.");
        } else {
            model.addAttribute("error", "No encontramos ninguna cuenta asociada a ese correo.");
        }
        return "login/recuperar-password";
    }

    @GetMapping("/restablecer-password")
    public String mostrarRestablecerPassword(@RequestParam("token") String token, Model model) {
        Optional<Usuario> usuarioOpt = passwordResetService.validarToken(token);
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("tokenInvalido", true);
        } else {
            model.addAttribute("token", token);
        }
        return "login/restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String procesarRestablecerPassword(@RequestParam("token") String token,
                                              @RequestParam("password") String password,
                                              @RequestParam("confirmarPassword") String confirmarPassword,
                                              Model model) {
        if (!password.equals(confirmarPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "login/restablecer-password";
        }
        boolean exito = passwordResetService.restablecerPassword(token, password);
        if (exito) {
            return "redirect:/login?claveCambiada";
        } else {
            model.addAttribute("error", "El enlace ha expirado o es inválido.");
            model.addAttribute("tokenInvalido", true);
            return "login/restablecer-password";
        }
    }
}