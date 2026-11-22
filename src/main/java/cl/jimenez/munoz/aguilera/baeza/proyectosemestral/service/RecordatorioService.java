package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);
    private static final long MINUTOS_24_HORAS = 24L * 60L;
    private static final long MINUTOS_2_HORAS = 2L * 60L;

    private final ReservaRepository reservaRepository;
    private final EmailService emailService;

    public RecordatorioService(ReservaRepository reservaRepository, EmailService emailService) {
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void enviarRecordatoriosProgramados() {
        procesarRecordatorios(LocalDateTime.now());
    }

    public int procesarRecordatorios(LocalDateTime ahora) {
        LocalDate desde = ahora.toLocalDate();
        LocalDate hasta = desde.plusDays(2);
        List<Reserva> candidatas = reservaRepository.findConfirmadasParaRecordatorio(desde, hasta);

        int enviados = 0;
        for (Reserva reserva : candidatas) {
            try {
                LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
                long minutosRestantes = Duration.between(ahora, inicio).toMinutes();

                if (minutosRestantes <= 0) {
                    continue;
                }

                if (minutosRestantes <= MINUTOS_2_HORAS && !reserva.isRecordatorio2hEnviado()) {
                    emailService.enviarRecordatorioPartido(reserva, 2);
                    reserva.setRecordatorio2hEnviado(true);
                    reservaRepository.save(reserva);
                    enviados++;
                    log.info("Recordatorio de 2 horas enviado para reserva {}", reserva.getCodigoReserva());
                } else if (minutosRestantes > MINUTOS_2_HORAS
                        && minutosRestantes <= MINUTOS_24_HORAS
                        && !reserva.isRecordatorio24hEnviado()) {
                    emailService.enviarRecordatorioPartido(reserva, 24);
                    reserva.setRecordatorio24hEnviado(true);
                    reservaRepository.save(reserva);
                    enviados++;
                    log.info("Recordatorio de 24 horas enviado para reserva {}", reserva.getCodigoReserva());
                }
            } catch (Exception e) {
                log.error("Error procesando recordatorio para reserva {}: {}", reserva.getCodigoReserva(), e.getMessage());
            }
        }
        return enviados;
    }
}
