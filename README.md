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

