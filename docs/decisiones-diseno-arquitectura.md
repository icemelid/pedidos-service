# Documento de decisiones de diseño y arquitectura

## 1. Introducción

Este documento recoge las principales decisiones de diseño adoptadas durante el desarrollo del servicio de gestión y confirmación de pedidos.

El objetivo es explicar los criterios técnicos utilizados para garantizar la coherencia de los datos, controlar los estados del pedido y gestionar los posibles errores en las comunicaciones con servicios externos.

Las decisiones se han centrado en tres aspectos fundamentales:

- Gestión del ciclo de vida del pedido.
- Resiliencia frente a errores y problemas de disponibilidad del proveedor.
- Control de concurrencia para evitar confirmaciones duplicadas.

---

## 2. Gestión de estados del pedido

### 2.1. Decisión de diseño

Se ha definido un ciclo de vida explícito para la confirmación de los pedidos mediante un enumerado `EstadoPedido`.

Los estados utilizados son:

| Estado | Significado |
|---|---|
| `CREATED` | Pedido creado y pendiente de confirmación. |
| `CONFIRMING` | Confirmación reservada y en proceso. |
| `CONFIRMED` | Confirmación completada correctamente. |
| `CONFIRMATION_FAILED` | La confirmación ha finalizado con error. |

### 2.2. Justificación

La utilización de estados explícitos permite conocer en todo momento la situación del pedido y controlar qué operaciones pueden realizarse.

Se evita depender exclusivamente de valores numéricos, indicadores booleanos o comprobaciones indirectas sobre los importes.

El estado `CONFIRMING` tiene una función especialmente importante: representa la reserva de la operación antes de iniciar la comunicación con el proveedor.

El estado `CONFIRMATION_FAILED` permite distinguir un pedido cuya confirmación ha fallado de otro que todavía no ha iniciado el proceso.

### 2.3. Transiciones principales

El flujo normal de confirmación es:

```text
CREATED
   |
   v
CONFIRMING
   |
   +--------------------+
   |                    |
   v                    v
CONFIRMED       CONFIRMATION_FAILED
```

Las transiciones se realizan de forma controlada desde la capa de servicio.

---

## 3. Resiliencia frente a errores del proveedor

### 3.1. Decisión de diseño

Se ha implementado un mecanismo de resiliencia para gestionar errores y demoras en la comunicación con el proveedor externo.

La solución contempla:

- Timeout de 6 segundos por intento.
- Un máximo de 3 intentos.
- Una espera de 500 milisegundos entre reintentos.
- Tratamiento de excepciones.
- Actualización del pedido a `CONFIRMATION_FAILED` cuando se agotan los intentos o se produce un error definitivo.

### 3.2. Justificación del timeout

Una llamada externa no debe bloquear indefinidamente el proceso de confirmación.

Se establece un límite de 6 segundos para cada intento, permitiendo que el servicio recupere el control cuando el proveedor no responde dentro del tiempo establecido.

La llamada se ejecuta mediante un `ExecutorService`, utilizando `Future.get()` con un límite temporal.

Si se supera el tiempo configurado, se cancela la tarea de espera y se genera una excepción de timeout.

### 3.3. Justificación de los reintentos

Los errores de comunicación pueden ser transitorios.

Por este motivo, se permite repetir la operación hasta un máximo de tres intentos, dejando una pausa de 500 milisegundos entre ellos.

El objetivo es dar al proveedor la oportunidad de recuperarse sin mantener al usuario esperando indefinidamente.

Una vez agotados los intentos, se interrumpe el proceso y se registra el fallo de confirmación.

### 3.4. Coherencia de los datos ante un error

La operación de confirmación se divide en fases transaccionales:

1. Reserva del pedido.
2. Comunicación con el proveedor fuera de la transacción de base de datos.
3. Actualización de los importes y del estado final en una nueva transacción.

Si la comunicación falla, el pedido se marca como `CONFIRMATION_FAILED`.

No se actualizan parcialmente el coste de envío ni el importe total.

Esta separación evita mantener una transacción de base de datos abierta durante una llamada externa potencialmente lenta.

---

## 4. Control de concurrencia

### 4.1. Problema que se quiere evitar

Dos peticiones simultáneas de confirmación para un mismo pedido no deben provocar dos procesos independientes de confirmación.

Sin un mecanismo de protección, ambas peticiones podrían comprobar inicialmente que el pedido está en estado `CREATED` y continuar al mismo tiempo.

Esto podría provocar llamadas duplicadas al proveedor y actualizaciones inconsistentes.

### 4.2. Decisión de diseño

Se ha implementado una actualización condicional del estado del pedido mediante una consulta JPQL en `PedidoRepository`.

La operación únicamente modifica el pedido cuando su estado actual coincide con el esperado:

```java
UPDATE Pedido p
SET p.estado = :nuevoEstado
WHERE p.id = :pedidoId
AND p.estado = :estadoActual
```

La actualización devuelve el número de registros modificados.

- Un registro actualizado significa que la petición ha reservado la confirmación.
- Ningún registro actualizado significa que otra operación ya ha modificado el estado o que el pedido no se encuentra disponible para confirmar.

### 4.3. Justificación

La protección se realiza en la base de datos y no mediante una variable compartida en memoria.

Esto permite controlar las peticiones concurrentes incluso cuando proceden de distintos hilos de ejecución.

Solamente la petición que consigue cambiar el estado de `CREATED` a `CONFIRMING` puede continuar con la comunicación con el proveedor.

La segunda petición no puede iniciar una nueva confirmación.

---

## 5. Idempotencia frente a llamadas repetidas al proveedor

### 5.1. Problema que se quiere evitar

Los mecanismos de reintento pueden provocar varias llamadas al proveedor para una misma operación lógica.

Aunque el servicio necesite repetir una petición por un timeout, no debería provocar que el proveedor procese varias veces la misma operación.

### 5.2. Decisión de diseño

Se ha incorporado un simulador de proveedor que utiliza una estructura concurrente basada en `ConcurrentHashMap` y `CompletableFuture`.

La operación se identifica mediante el identificador del pedido.

Las llamadas repetidas para un mismo pedido reutilizan el resultado de una única operación de procesamiento, en lugar de iniciar procesamientos independientes.

### 5.3. Justificación

Este mecanismo permite comprobar la diferencia entre:

- Número de llamadas recibidas por el proveedor.
- Número de procesamientos efectivos realizados.

La simulación permite verificar que varios intentos pueden compartir una única operación en curso.

**Alcance:** esta implementación demuestra el comportamiento idempotente dentro del simulador y del ciclo de vida de la aplicación. Una integración real debe acordar con el proveedor una clave de idempotencia estable y disponer de garantías de persistencia y retención adecuadas.

---

## 6. Separación de responsabilidades

Se han mantenido responsabilidades diferenciadas entre las distintas capas:

| Componente | Responsabilidad |
|---|---|
| Controller | Recibir las peticiones HTTP y devolver las respuestas. |
| Service | Coordinar el proceso de confirmación y sus reglas de negocio. |
| Repository | Acceder a la base de datos y realizar las actualizaciones condicionales. |
| Entity | Representar la información persistida del pedido. |
| Enum EstadoPedido | Definir los estados posibles del pedido. |
| ProveedorClient | Abstraer la comunicación con el proveedor externo. |
| ProveedorClientLentoSimulado | Simular el comportamiento del proveedor para las pruebas. |

Esta separación facilita el mantenimiento, las pruebas y la evolución de la aplicación.

---

## 7. Resumen de las decisiones principales

| Área | Decisión | Beneficio |
|---|---|---|
| Estados | Ciclo de vida explícito | Control del proceso de confirmación. |
| Timeout | 6 segundos por intento | Evitar esperas indefinidas. |
| Reintentos | Máximo 3 intentos | Recuperación ante errores transitorios. |
| Espera | 500 ms entre intentos | Evitar reintentos inmediatos. |
| Transacciones | Separar reserva, llamada externa y resultado | Evitar mantener transacciones abiertas durante llamadas externas. |
| Concurrencia | Actualización condicional atómica | Impedir reservas simultáneas del mismo pedido. |
| Idempotencia | Reutilización de una operación por identificador | Evitar procesamientos duplicados en el simulador. |
| Arquitectura | Separación por responsabilidades | Facilitar mantenimiento y pruebas. |

## 8. Conclusión

El diseño busca que la confirmación de pedidos sea un proceso controlado, tolerante a errores y protegido frente a ejecuciones concurrentes.

Las decisiones adoptadas permiten abordar los principales riesgos de una operación que depende de un servicio externo:

- Duplicidad de confirmaciones.
- Indisponibilidad o lentitud del proveedor.
- Repetición de llamadas.
- Actualizaciones parciales de datos.
- Inconsistencias derivadas de peticiones simultáneas.

La solución se ha desarrollado de forma que cada mecanismo tenga una responsabilidad concreta y pueda verificarse mediante pruebas específicas.