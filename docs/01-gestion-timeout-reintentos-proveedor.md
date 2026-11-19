# Gestión de tiempos de espera y reintentos del proveedor

## 1. Objetivo

Definir cómo debe comportarse el sistema cuando el proveedor externo encargado de calcular el coste de envío tarda demasiado en responder o devuelve un error.

El objetivo es evitar que una incidencia del proveedor deje el proceso bloqueado indefinidamente o que el pedido termine en un estado que no refleje correctamente el resultado de la operación.

## 2. Decisiones de diseño

Se han establecido los siguientes parámetros:

| Parámetro | Valor |
|---|---:|
| Tiempo máximo de espera por intento | 6 segundos |
| Número máximo de intentos | 3 |
| Espera entre reintentos | 500 ms |

El tiempo máximo de espera se aplica individualmente a cada intento de comunicación con el proveedor.

Si los tres intentos agotan su tiempo máximo de espera, el tiempo total aproximado puede alcanzar los 19 segundos: 18 segundos correspondientes a los tres intentos y 1 segundo adicional de espera entre ellos, sin contar pequeños tiempos de procesamiento.

## 3. Gestión del timeout

La llamada al proveedor se ejecuta en un hilo independiente mediante un `ExecutorService`.

El sistema espera el resultado durante un máximo de 6 segundos:

- Si el proveedor responde dentro del plazo, se obtiene el resultado.
- Si supera los 6 segundos, se produce un timeout.
- Se cancela la tarea mediante `Future.cancel(true)`, solicitando la interrupción del hilo que ejecuta la operación.
- El timeout se trata como un fallo recuperable y puede dar lugar a un nuevo intento.

La interrupción es una solicitud: no garantiza que el proveedor externo haya detenido realmente el procesamiento de la operación.

## 4. Política de reintentos

Cuando se produce un error o un timeout, el sistema vuelve a intentar la operación, respetando los límites establecidos.

El comportamiento es el siguiente:

1. Primer intento.
2. Si falla, espera de 500 ms.
3. Segundo intento.
4. Si vuelve a fallar, espera otros 500 ms.
5. Tercer y último intento.
6. Si también falla, se considera agotada la política de reintentos y se devuelve un error al proceso llamante.

No se realizan más de tres intentos.

## 5. Estados del pedido

El proceso utiliza los siguientes estados:

| Estado | Significado |
|---|---|
| `CREATED` | Pedido creado y pendiente de confirmación. |
| `CONFIRMING` | Confirmación reservada y operación con el proveedor en curso. |
| `CONFIRMED` | Confirmación completada correctamente. |
| `CONFIRMATION_FAILED` | No se ha podido completar la confirmación. |

El flujo de estados es:

**En caso de éxito:**

`CREATED → CONFIRMING → CONFIRMED`

**En caso de agotarse los intentos:**

`CREATED → CONFIRMING → CONFIRMATION_FAILED`

El pedido solo pasa a `CONFIRMED` cuando se ha obtenido correctamente el resultado del proveedor y se han guardado los datos correspondientes.

Si se agotan los intentos, el sistema registra `CONFIRMATION_FAILED` y propaga el error.

## 6. Gestión de transacciones

La llamada al proveedor externo no se mantiene dentro de una transacción de base de datos.

El proceso se divide en operaciones diferenciadas:

1. Una transacción corta reserva la confirmación, cambiando el estado de `CREATED` a `CONFIRMING`.
2. Se realiza la comunicación con el proveedor fuera de la transacción.
3. Si hay éxito, una nueva transacción actualiza el coste de envío, el importe total y el estado `CONFIRMED`.
4. Si se produce un fallo definitivo, otra transacción actualiza el estado a `CONFIRMATION_FAILED`.

Esta separación evita mantener abierta una transacción de base de datos mientras se espera una respuesta externa.

## 7. Comportamiento ante los distintos escenarios

| Escenario | Resultado esperado |
|---|---|
| El proveedor responde antes de 6 segundos | Se acepta la respuesta y se confirma el pedido. |
| El primer intento agota el tiempo, pero el segundo responde correctamente | Se confirma el pedido. |
| Los dos primeros intentos fallan y el tercero responde correctamente | Se confirma el pedido. |
| Los tres intentos fallan o agotan el tiempo | El pedido queda en `CONFIRMATION_FAILED`. |
| El proveedor no responde dentro del tiempo establecido | Se produce un timeout y se solicita la interrupción de la tarea. |

## 8. Pruebas realizadas

Para comprobar este comportamiento se ha utilizado un proveedor simulado, permitiendo controlar el tiempo de respuesta.

### Prueba A. Timeout y agotamiento de reintentos

Se configura el proveedor simulado para que tarde 7 segundos en responder, superando el límite de 6 segundos.

Resultado esperado:

- Se producen tres intentos.
- Cada intento supera el tiempo máximo permitido.
- Se realizan dos esperas de 500 ms.
- La operación termina con un error.
- El pedido queda en `CONFIRMATION_FAILED`.
- No se actualizan el coste de envío ni el importe total como si la confirmación hubiera tenido éxito.

### Prueba B. Respuesta correcta dentro del plazo

Se configura el proveedor para que responda dentro de los 6 segundos.

Resultado esperado:

- La operación finaliza correctamente.
- Se actualizan el coste de envío y el importe total.
- El pedido pasa a `CONFIRMED`.

### Prueba C. Recuperación mediante reintento

Se provoca un fallo inicial y se permite que un intento posterior responda correctamente.

Resultado esperado:

- El sistema no abandona el proceso tras el primer fallo.
- Se respeta la espera de 500 ms.
- Se obtiene el resultado en uno de los intentos posteriores.
- El pedido termina en `CONFIRMED`.

## 9. Clases y mecanismos implicados

- `PedidoService`: coordina la confirmación, los reintentos, los timeouts y las transacciones.
- `ProveedorClient`: define la interfaz de comunicación con el proveedor.
- `ProveedorClientLentoSimulado`: permite simular respuestas lentas y comprobar el comportamiento.
- `ExecutorService`: ejecuta la llamada externa en un hilo independiente.
- `Future.get(6, TimeUnit.SECONDS)`: establece el límite de espera.
- `Future.cancel(true)`: solicita la interrupción de la tarea cuando se supera el tiempo máximo.

## 10. Resultado del diseño

El sistema establece un límite temporal para las llamadas al proveedor, permite recuperarse de fallos transitorios mediante reintentos y evita confirmar un pedido cuando no se ha obtenido una respuesta válida.

Además, separa las operaciones externas de las transacciones de base de datos y mantiene un estado explícito del pedido cuando la confirmación no puede completarse.

La política de reintentos y el estado final permiten conocer qué ha sucedido y evitan presentar como confirmada una operación que realmente ha fallado.