# Checklist de publicación en Google Play - Guarda Estados

## Identidad de la app

- [ ] Confirmar nombre público de la app: Guarda Estados.
- [ ] Confirmar que el nombre coincide entre la ficha de Google Play, la app y la política de privacidad.
- [ ] Revisar si el ícono actual representa la app o crear un ícono final antes de publicar.
- [ ] Verificar que el ícono se vea correctamente en launcher, ajustes del sistema y Play Store.

## Ficha de Google Play

- [ ] Elegir categoría sugerida: Herramientas.
- [ ] Completar correo de contacto público.
- [ ] Publicar la política de privacidad en una URL pública, activa, no editable por usuarios, no geobloqueada y no PDF.
- [ ] Agregar la URL pública de la política de privacidad en Play Console.
- [ ] Preparar capturas de pantalla obligatorias para teléfonos Android.
- [ ] Preparar capturas adicionales para otros formatos si se decide soportarlos.
- [ ] Escribir texto corto sin afirmar funciones no implementadas.
- [ ] Escribir descripción completa sin afirmar guardado de videos hasta que esa función exista.
- [ ] Incluir aviso de no afiliación con WhatsApp LLC, Meta Platforms, Inc. ni aplicaciones compatibles.
- [ ] Indicar que el usuario debe guardar o compartir contenido solo con autorización de su creador.

## Privacidad y Data Safety

- [ ] Completar la sección Data Safety en Play Console.
- [ ] Declarar que la app no recopila datos de usuario.
- [ ] Declarar que la app no comparte datos de usuario.
- [ ] Declarar que la app no crea cuentas.
- [ ] Confirmar que la app funciona localmente.
- [ ] Confirmar que la app accede solo a la carpeta elegida por el usuario mediante el selector oficial de Android.
- [ ] Confirmar que el permiso URI persistente se usa solo para recordar la carpeta autorizada.
- [ ] Confirmar que compartir contenido ocurre solo cuando el usuario lo solicita mediante el selector oficial de Android.
- [ ] Revisar que la política de privacidad no mencione funciones que aún no estén implementadas.
- [ ] Completar en la política de privacidad: [correo de contacto].
- [ ] Completar en la política de privacidad: [fecha de actualización].

## Clasificación y cumplimiento

- [ ] Completar la clasificación de contenido en Play Console.
- [ ] Revisar que la descripción y capturas no usen marcas de terceros de forma engañosa.
- [ ] Verificar que no se pidan permisos innecesarios.
- [ ] Verificar que no se use MANAGE_EXTERNAL_STORAGE.
- [ ] Verificar que no haya Firebase, anuncios, analítica ni SDKs externos de recopilación de datos si no se van a declarar.

## Build y lanzamiento

- [ ] Crear o configurar la cuenta de desarrollador de Google Play.
- [ ] Configurar firma de lanzamiento de forma segura fuera del repositorio.
- [ ] Verificar que local.properties, keystores, *.jks, *.keystore y archivos de secretos estén ignorados por Git.
- [ ] Generar un Android App Bundle AAB de release.
- [ ] Probar el AAB de release antes de subirlo.
- [ ] Guardar credenciales y keystore fuera del repositorio.

## Prueba cerrada

- [ ] Crear una pista de prueba cerrada.
- [ ] Agregar testers.
- [ ] Subir el AAB a la pista de prueba cerrada.
- [ ] Probar selección de carpeta con el selector oficial de Android.
- [ ] Probar visualización de estados de imagen.
- [ ] Probar guardado de copias locales de imágenes.
- [ ] Probar compartir solo bajo acción explícita del usuario.
- [ ] Probar cambio de tema y persistencia.
- [ ] Probar restablecer configuración.
- [ ] Confirmar que las copias guardadas no se borran al restablecer configuración.

## Verificación final antes de revisión

- [ ] Ejecutar assembleDebug.
- [ ] Ejecutar testDebugUnitTest.
- [ ] Ejecutar lintDebug.
- [ ] Revisar que no haya secretos ni archivos sensibles incluidos.
- [ ] Revisar que la política de privacidad publicada coincida con el comportamiento real de la app.
- [ ] Revisar que Data Safety coincida con el comportamiento real de la app y sus dependencias.
- [ ] Revisar que la ficha no afirme soporte de videos hasta que esté implementado.
- [ ] Enviar a revisión cuando la ficha, Data Safety, política pública y build release estén completos.