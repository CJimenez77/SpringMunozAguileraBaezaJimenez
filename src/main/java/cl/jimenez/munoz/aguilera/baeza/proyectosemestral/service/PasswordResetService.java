package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public PasswordResetService(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @Transactional
    public boolean solicitarRecuperacion(String email, String baseUrl) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setTokenExpiracion(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        String enlace = baseUrl + "/restablecer-password?token=" + token;
        emailService.enviarCorreoRecuperacion(usuario.getEmail(), usuario.getNombre(), enlace);
        return true;
    }

    public Optional<Usuario> validarToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenRecuperacion(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getTokenExpiracion() != null && usuario.getTokenExpiracion().isAfter(LocalDateTime.now())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public boolean restablecerPassword(String token, String nuevaPassword) {
        Optional<Usuario> usuarioOpt = validarToken(token);
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        Usuario usuario = usuarioOpt.get();
        usuario.setPassword(nuevaPassword);
        usuario.setTokenRecuperacion(null);
        usuario.setTokenExpiracion(null);
        usuarioRepository.save(usuario);
        return true;
    }
}
