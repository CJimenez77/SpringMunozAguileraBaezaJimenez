package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.FidelizacionInfo;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FidelizacionService {

    private final ReservaRepository reservaRepository;

    public FidelizacionService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public int contarReservasValidasCiclo(Usuario usuario) {
        if (usuario == null) {
            return 0;
        }
        List<Reserva> validas = reservaRepository.findValidasPorUsuario(usuario);
        int contador = 0;
        for (Reserva r : validas) {
            if (r.isCuponFidelizacionAplicado()) {
                contador = 0;
            } else {
                contador++;
                if (contador > 2) {
                    contador = 2;
                }
            }
        }
        return contador;
    }

    public boolean tieneDescuentoDisponible(Usuario usuario) {
        return contarReservasValidasCiclo(usuario) >= 2;
    }

    public int reservasFaltantesParaDescuento(Usuario usuario) {
        return Math.max(0, 2 - contarReservasValidasCiclo(usuario));
    }

    public FidelizacionInfo obtenerInfoFidelizacion(Usuario usuario) {
        int completadas = contarReservasValidasCiclo(usuario);
        return new FidelizacionInfo(completadas);
    }
}
