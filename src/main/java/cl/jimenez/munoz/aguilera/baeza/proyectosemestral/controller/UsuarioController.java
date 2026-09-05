package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listarUsuarios(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"ADMIN_SISTEMA".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }
        model.addAttribute("usuarios", usuarioRepository.findByActivoTrue());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"ADMIN_SISTEMA".equals(usuario.getRol())) {
            return "redirect:/dashboard";
        }
        model.addAttribute("usuario", new Usuario());
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(
            @ModelAttribute Usuario usuario,
            @RequestParam("archivoFoto") MultipartFile foto,
            HttpSession session
    ) {
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (sesion == null || !"ADMIN_SISTEMA".equals(sesion.getRol())) {
            return "redirect:/dashboard";
        }

        try {
            if (foto != null && !foto.isEmpty()) {
                String imagenBase64 = Base64.getEncoder().encodeToString(foto.getBytes());
                String tipo = foto.getContentType();
                usuario.setFoto("data:" + tipo + ";base64," + imagenBase64);
                String carpeta = "src/main/resources/static/uploads/";
                File directorio = new File(carpeta);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }
                Path ruta = Paths.get(carpeta + foto.getOriginalFilename());
                Files.write(ruta, foto.getBytes());
            } else if (usuario.getId() != null) {
                Usuario usuarioExistente = usuarioRepository.findById(usuario.getId()).orElse(null);
                if (usuarioExistente != null) {
                    usuario.setFoto(usuarioExistente.getFoto());
                }
            }
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (sesion == null || !"ADMIN_SISTEMA".equals(sesion.getRol())) {
            return "redirect:/dashboard";
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            return "redirect:/usuarios";
        }
        model.addAttribute("usuario", usuario);
        return "usuarios/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id, HttpSession session) {
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (sesion == null || !"ADMIN_SISTEMA".equals(sesion.getRol())) {
            return "redirect:/dashboard";
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/dar-de-baja")
    public String autoEliminarCuenta(HttpSession session) {
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (sesion != null) {
            Usuario usuario = usuarioRepository.findById(sesion.getId()).orElse(null);
            if (usuario != null) {
                usuario.setActivo(false);
                usuarioRepository.save(usuario);
            }
            session.invalidate();
        }
        return "redirect:/login?desactivada";
    }
}