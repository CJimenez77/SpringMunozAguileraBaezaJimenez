package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.jimenez.munoz.aguilera.baeza.proyectosemestral.model.Reserva;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:notificaciones@canchasya.cl}")
    private String remitente;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void enviarCorreoRecuperacion(String destinatario, String nombre, String enlaceRecuperacion) {
        String asunto = "Recuperación de Contraseña - CanchasYa";
        String contenidoHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h2 style='color: #0d6efd; margin: 0;'>CanchasYa</h2>"
                + "<p style='color: #6c757d; font-size: 14px;'>Portal de Gestión y Reservas Deportivas</p>"
                + "</div>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. Haz clic en el siguiente botón para continuar:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + enlaceRecuperacion
                + "' style='background-color: #0d6efd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Restablecer Contraseña</a>"
                + "</div>"
                + "<p style='font-size: 13px; color: #6c757d;'>Este enlace expirará en 1 hora. Si no solicitaste este cambio, puedes ignorar este correo con total tranquilidad.</p>"
                + "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>"
                + "<p style='font-size: 12px; color: #adb5bd; text-align: center;'>Equipo CanchasYa - Sistema Integral de Reservas</p>"
                + "</div>";

        enviarHtml(destinatario, asunto, contenidoHtml);
    }

    public void enviarConfirmacionReserva(Reserva reserva) {
        String destinatario = reserva.getUsuario().getEmail();
        String asunto = "Confirmación de Reserva #" + reserva.getCodigoReserva() + " - CanchasYa";

        String contenidoHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h2 style='color: #198754; margin: 0;'>Reserva Confirmada</h2>"
                + "<p style='color: #6c757d; font-size: 14px;'>Código: <strong>" + reserva.getCodigoReserva()
                + "</strong></p>"
                + "</div>"
                + "<p>Hola <strong>" + reserva.getUsuario().getNombre() + "</strong>,</p>"
                + "<p>Tu reserva ha sido procesada exitosamente con los siguientes detalles:</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 20px 0; font-size: 14px;'>"
                + "<tr style='background-color: #f8f9fa;'><td style='padding: 8px; border: 1px solid #dee2e6;'><strong>Complejo:</strong></td><td style='padding: 8px; border: 1px solid #dee2e6;'>"
                + reserva.getCancha().getComplejo().getNombre_complejo() + "</td></tr>"
                + "<tr><td style='padding: 8px; border: 1px solid #dee2e6;'><strong>Cancha:</strong></td><td style='padding: 8px; border: 1px solid #dee2e6;'>"
                + reserva.getCancha().getNombre_cancha() + " (" + reserva.getCancha().getTipo_cancha() + ")</td></tr>"
                + "<tr style='background-color: #f8f9fa;'><td style='padding: 8px; border: 1px solid #dee2e6;'><strong>Fecha:</strong></td><td style='padding: 8px; border: 1px solid #dee2e6;'>"
                + reserva.getFecha().format(dateFormatter) + "</td></tr>"
                + "<tr><td style='padding: 8px; border: 1px solid #dee2e6;'><strong>Horario:</strong></td><td style='padding: 8px; border: 1px solid #dee2e6;'>"
                + reserva.getHoraInicio().format(timeFormatter) + " - " + reserva.getHoraFin().format(timeFormatter)
                + " (" + reserva.getDuracionMinutos() + " min)</td></tr>"
                + "<tr style='background-color: #f8f9fa;'><td style='padding: 8px; border: 1px solid #dee2e6;'><strong>Total Pagado:</strong></td><td style='padding: 8px; border: 1px solid #dee2e6; font-weight: bold; color: #198754;'>$"
                + String.format("%,d", reserva.getPrecioTotal()).replace(',', '.') + " CLP</td></tr>"
                + "</table>"
                + "<p style='font-size: 13px; color: #6c757d;'>Recuerda presentarte al menos 10 minutos antes de tu horario. Cancelaciones permitidas hasta 24 horas antes del partido.</p>"
                + "</div>";

        enviarHtml(destinatario, asunto, contenidoHtml);
    }

    public void enviarCancelacionReserva(Reserva reserva) {
        String destinatario = reserva.getUsuario().getEmail();
        String asunto = "Cancelación de Reserva #" + reserva.getCodigoReserva() + " - CanchasYa";

        String contenidoHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h2 style='color: #dc3545; margin: 0;'>Reserva Cancelada</h2>"
                + "<p style='color: #6c757d; font-size: 14px;'>Código: <strong>" + reserva.getCodigoReserva()
                + "</strong></p>"
                + "</div>"
                + "<p>Hola <strong>" + reserva.getUsuario().getNombre() + "</strong>,</p>"
                + "<p>Te confirmamos que tu reserva en <strong>" + reserva.getCancha().getNombre_cancha()
                + "</strong> para el día <strong>" + reserva.getFecha().format(dateFormatter)
                + "</strong> a las <strong>" + reserva.getHoraInicio().format(timeFormatter)
                + " hrs</strong> ha sido cancelada dentro del plazo estipulado.</p>"
                + "<p>El monto de <strong>$" + String.format("%,d", reserva.getPrecioTotal()).replace(',', '.')
                + " CLP</strong> ha sido marcado para reembolso a través del mismo método de pago original.</p>"
                + "</div>";

        enviarHtml(destinatario, asunto, contenidoHtml);
    }

    public void enviarRecordatorioPartido(Reserva reserva) {
        String destinatario = reserva.getUsuario().getEmail();
        String asunto = "Recordatorio de Partido - Reserva #" + reserva.getCodigoReserva() + " - CanchasYa";

        String contenidoHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "<h2 style='color: #ffc107; margin: 0;'>¡Tu partido se acerca!</h2>"
                + "<p style='color: #6c757d; font-size: 14px;'>Faltan menos de 24 horas para tu reserva</p>"
                + "</div>"
                + "<p>Hola <strong>" + reserva.getUsuario().getNombre() + "</strong>,</p>"
                + "<p>Te recordamos tu próximo partido en <strong>"
                + reserva.getCancha().getComplejo().getNombre_complejo() + "</strong>:</p>"
                + "<ul>"
                + "<li><strong>Cancha:</strong> " + reserva.getCancha().getNombre_cancha() + "</li>"
                + "<li><strong>Fecha:</strong> " + reserva.getFecha().format(dateFormatter) + "</li>"
                + "<li><strong>Horario:</strong> " + reserva.getHoraInicio().format(timeFormatter) + " - "
                + reserva.getHoraFin().format(timeFormatter) + "</li>"
                + "</ul>"
                + "</div>";

        enviarHtml(destinatario, asunto, contenidoHtml);
    }

    private void enviarHtml(String destinatario, String asunto, String html) {
        try {
            if (mailSender != null) {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(html, true);
                if (remitente != null && !remitente.isEmpty()) {
                    helper.setFrom(remitente, "CanchasYa");
                } else {
                    helper.setFrom("notificaciones@canchasya.cl", "CanchasYa");
                }
                mailSender.send(mensaje);
                log.info("Correo enviado exitosamente a {}", destinatario);
            } else {
                log.info("[SIMULACION EMAIL] Para: {} | Asunto: {}", destinatario, asunto);
            }
        } catch (Exception e) {
            log.warn("Servidor SMTP no disponible. Notificacion registrada en log para {}: {}", destinatario,
                    e.getMessage());
        }
    }
}
