package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.controller;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cupon;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.FidelizacionInfo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.CuponService;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service.FidelizacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final FidelizacionService fidelizacionService;
    private final CuponService cuponService;

    public GlobalExceptionHandler(FidelizacionService fidelizacionService, CuponService cuponService) {
        this.fidelizacionService = fidelizacionService;
        this.cuponService = cuponService;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    @ModelAttribute("fidelizacion")
    public FidelizacionInfo fidelizacion(HttpSession session) {
        if (session == null) {
            return null;
        }
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return null;
        }
        return fidelizacionService.obtenerInfoFidelizacion(usuario);
    }

    @ModelAttribute("cuponVigente")
    public Cupon cuponVigente() {
        return cuponService.obtenerCuponVigente().orElse(null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoMuyGrande(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "La imagen es muy pesada (máx. 10MB).");
        return "redirect:/canchas";
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String conflictoDeDatos(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Ese registro ya fue modificado, intenta de nuevo.");
        return "redirect:/canchas";
    }
}