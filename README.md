# Guia de Configuracion y Ejecucion

## 1. Base de Datos
El proyecto utiliza PostgreSQL en Docker.

Para iniciar la base de datos:
```bash
docker compose up -d
```

Configuracion por defecto:
- Host: localhost
- Puerto: 5423
- Base de datos: canchas_db
- Usuario: postgres
- Password: adminpassword

## 2. Variables de Entorno (.env)
Crear un archivo llamado `.env` en la raiz del proyecto con el siguiente contenido:

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sistemadegestiondocumental26@gmail.com
MAIL_PASSWORD=zffisviwokejrvwj
MAIL_AUTH=true
MAIL_STARTTLS=true
```

## 3. Ejecucion de la Aplicacion
Para iniciar el servidor Spring Boot:

```bash
./gradlew bootRun
```

La aplicacion estara disponible en:
http://localhost:8081

## 4. Credenciales Transbank (Ambiente de Integracion)
Configuradas en `src/main/resources/application.properties`:
- Codigo de Comercio: 597055555532
- API Key: 579B532A7440BB061079D614E3D5CD45
- Ambiente: TEST
- URL de Retorno: http://localhost:8081/reservas/webpay-retorno

## 5. Datos de Prueba para Webpay Plus

### Tarjeta (Transaccion Aprobada)
- Tipo: VISA
- Numero de Tarjeta: 4051 8856 0044 6623
- Fecha de Expiracion: Cualquier fecha futura (ej. 12/28)
- CVV: 123

### Autenticacion Bancaria
- RUT: 11.111.111-1
- Clave: 123

## 6. Usuarios de Prueba

Se crean automáticamente al iniciar la aplicación por primera vez (ver `DataSeeder`):

Administrador General: 
- admin@canchasya.cl - admin123

Administrador de complejo
- complejo@canchasya.cl - complejo123

Cliente
- cliente@canchasya.cl - cliente123

## 7. Cancelaciones y pagos compartidos

- Las reservas se pueden cancelar hasta antes de su hora de inicio. El reembolso se procesa en Webpay: 100% si faltan más de 24 horas y 50% si faltan 24 horas o menos.
- Al crear una reserva individual, se puede activar **Dividir el pago con invitados** e indicar de 2 a 30 participantes, contando al titular.
- El titular paga su cuota y comparte el enlace generado desde el detalle de la reserva. Cada invitado debe iniciar sesión, unirse desde el enlace y pagar su cuota por Webpay.
- La reserva queda confirmada cuando todos los participantes han pagado. Si se cancela antes, cada pago aprobado se reversa según la política de anticipación.
- El pago compartido no está disponible para reservas recurrentes. El titular cubre cualquier diferencia de redondeo en pesos enteros.
- Para publicar enlaces de invitación desde otro dominio, configurar `PUBLIC_BASE_URL` con la URL pública base de la aplicación.
- Si Webpay no confirma un reverso, la cancelación queda pendiente de conciliación y no se vuelve a enviar automáticamente el mismo reembolso.
