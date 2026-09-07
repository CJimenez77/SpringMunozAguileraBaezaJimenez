package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Pago;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.PagoRepository;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class WebpaySandboxService {

    public static final String CODIGO_COMERCIO = "597055555532";
    public static final String API_KEY_SECRET = "579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C";

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    public WebpaySandboxService(PagoRepository pagoRepository,
            ReservaRepository reservaRepository,
            EmailService emailService) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
    }

    public Pago iniciarTransaccion(Reserva reserva, String metodoPago) {
        Pago pago = pagoRepository.findByReserva(reserva).orElse(new Pago());
        pago.setReserva(reserva);
        pago.setMonto(reserva.getPrecioTotal());
        pago.setMetodoPago(metodoPago != null ? metodoPago : "WEBPAY_PLUS");
        pago.setEstado("PENDIENTE");
        pago.setTransaccionId("TBK-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase());
        pago.setFechaPago(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago procesarPagoWebpay(Long reservaId, String numeroTarjeta, String mesVencimiento, String anioVencimiento,
            String cvv, String tipoTarjeta) {
        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return null;
        }

        Pago pago = pagoRepository.findByReserva(reserva).orElseGet(() -> iniciarTransaccion(reserva, "WEBPAY_PLUS"));

        String numLimpio = (numeroTarjeta != null) ? numeroTarjeta.replaceAll("\\s+", "").replaceAll("-", "") : "";
        boolean esAprobada = true;

        if (numLimpio.contains("5186059559590568") || numLimpio.contains("5186008541233829")
                || numLimpio.contains("5186174110629480")) {
            esAprobada = false;
        }

        String ultimos4 = (numLimpio.length() >= 4) ? numLimpio.substring(numLimpio.length() - 4) : "6623";

        if (esAprobada) {
            int codigoAuth = 100000 + random.nextInt(900000);
            pago.setEstado("PAGADO");
            pago.setCodigoAutorizacion(String.valueOf(codigoAuth));
            pago.setUltimosDigitos(ultimos4);
            pago.setTipoTarjeta(tipoTarjeta != null ? tipoTarjeta : "Crédito / Débito");
            pago.setFechaPago(LocalDateTime.now());

            reserva.setEstado("CONFIRMADA");
            reservaRepository.save(reserva);
            pagoRepository.save(pago);

            emailService.enviarConfirmacionReserva(reserva);
        } else {
            pago.setEstado("FALLIDO");
            pago.setCodigoAutorizacion(null);
            pago.setUltimosDigitos(ultimos4);
            pago.setTipoTarjeta(tipoTarjeta != null ? tipoTarjeta : "Rechazada");
            pago.setFechaPago(LocalDateTime.now());
            pagoRepository.save(pago);
        }

        return pago;
    }

    @Transactional
    public Pago simularTransferencia(Long reservaId, String bancoOrigen, String comprobanteNumero) {
        Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) {
            return null;
        }

        Pago pago = pagoRepository.findByReserva(reserva).orElseGet(() -> iniciarTransaccion(reserva, "TRANSFERENCIA"));
        pago.setEstado("PAGADO");
        pago.setMetodoPago("TRANSFERENCIA");
        pago.setCodigoAutorizacion("TRF-" + (100000 + random.nextInt(900000)));
        pago.setUltimosDigitos(bancoOrigen != null ? bancoOrigen : "BANCO");
        pago.setTipoTarjeta("Transferencia Directa");
        pago.setFechaPago(LocalDateTime.now());

        reserva.setEstado("CONFIRMADA");
        reservaRepository.save(reserva);
        pagoRepository.save(pago);

        emailService.enviarConfirmacionReserva(reserva);
        return pago;
    }

    @Transactional
    public boolean reembolsarPago(Reserva reserva) {
        Pago pago = pagoRepository.findByReserva(reserva).orElse(null);
        if (pago != null && "PAGADO".equals(pago.getEstado())) {
            pago.setEstado("REEMBOLSADO");
            pagoRepository.save(pago);
            return true;
        }
        return false;
    }
}
