package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
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
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(
            @ModelAttribute Usuario usuario,
            @RequestParam("archivoFoto") MultipartFile foto
    ) {
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

            usuarioRepository.save(usuario);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return "redirect:/usuarios";
        }
        model.addAttribute("usuario", usuario);
        return "usuarios/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id) {
        usuarioRepository.deleteById(id);
        return "redirect:/usuarios";
    }
}