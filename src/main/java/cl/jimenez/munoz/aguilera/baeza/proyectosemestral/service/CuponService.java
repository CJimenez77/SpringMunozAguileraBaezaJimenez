package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Cupon;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Usuario;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.CuponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CuponService {

    private final CuponRepository cuponRepository;

    public CuponService(CuponRepository cuponRepository) {
        this.cuponRepository = cuponRepository;
    }

    @Transactional
    public Cupon crearCupon(String codigo, int porcentaje, int diasDuracion, Integer limiteUsos, Usuario creador) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código no puede estar vacío.");
        }
        if (porcentaje < 1 || porcentaje > 20) {
            throw new IllegalArgumentException("El porcentaje de descuento debe estar entre 1% y 20%.");
        }
        if (diasDuracion < 1) {
            throw new IllegalArgumentException("La duración debe ser de al menos 1 día.");
        }
        String codigoNormalizado = codigo.trim().toUpperCase();
        if (cuponRepository.findByCodigoIgnoreCaseAndActivoTrue(codigoNormalizado).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cupón activo con ese código.");
        }

        Cupon cupon = new Cupon(codigoNormalizado, porcentaje, diasDuracion, limiteUsos, creador);
        return cuponRepository.save(cupon);
    }

    public Optional<Cupon> obtenerCuponVigente() {
        List<Cupon> vigentes = cuponRepository.findCuponesVigentes();
        return vigentes.isEmpty() ? Optional.empty() : Optional.of(vigentes.get(0));
    }

    public Optional<Cupon> validarCupon(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Cupon> cuponOpt = cuponRepository.findByCodigoIgnoreCaseAndActivoTrue(codigo.trim().toUpperCase());
        if (cuponOpt.isPresent() && cuponOpt.get().esValido()) {
            return cuponOpt;
        }
        return Optional.empty();
    }

    @Transactional
    public void registrarUso(Cupon cupon) {
        if (cupon != null) {
            cupon.setUsosActuales(cupon.getUsosActuales() + 1);
            if (cupon.getLimiteUsos() != null && cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
                cupon.setActivo(false);
            }
            cuponRepository.save(cupon);
        }
    }

    @Transactional
    public void alternarEstado(Long cuponId) {
        cuponRepository.findById(cuponId).ifPresent(c -> {
            c.setActivo(!c.isActivo());
            cuponRepository.save(c);
        });
    }

    public List<Cupon> listarTodos() {
        return cuponRepository.findAllByOrderByFechaCreacionDesc();
    }
}
