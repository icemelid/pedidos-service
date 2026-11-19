Control de concurrencia en la confirmación de pedidos
1. Objetivo
Garantizar que dos peticiones simultáneas de confirmación sobre un mismo pedido no puedan iniciar dos procesos independientes de confirmación.
El sistema debe asegurar que:
·	Solamente una petición pueda reservar la confirmación de un pedido.
·	Una segunda petición simultánea no pueda iniciar otra llamada al proveedor.
·	No se produzcan actualizaciones concurrentes que dejen los datos del pedido en un estado inconsistente.
·	El coste de envío, el importe total y el estado final del pedido se actualicen de forma coherente.
2. Solución técnica implementada
Se ha implementado un mecanismo de reserva mediante una actualización condicional del estado del pedido en base de datos.
2.1. Reserva atómica de la confirmación
Antes de contactar con el proveedor, el servicio ejecuta una actualización que únicamente permite cambiar el estado del pedido de CREATEDa CONFIRMINGsi todavía conserva el estado inicial.
La operación se implementa en PedidoRepository:
@Modifying @Query(""" UPDATE Pedido p SET p.estado = :nuevoEstado WHERE p.id = :pedidoId AND p.estado = :estadoActual """) int actualizarEstadoSiCoincide( @Param("pedidoId") Long pedidoId, @Param("estadoActual") EstadoPedido estadoActual, @Param("nuevoEstado") EstadoPedido nuevoEstado );
El método devuelve el número de registros actualizados:
·	Si devuelve 1, la petición ha reservado correctamente la confirmación.
·	Si devuelve 0, el pedido ya no se encuentra en el estado esperado y la petición no puede continuar.
Esta actualización condicional se ejecuta dentro de una transacción breve, mediante TransactionTemplate.
La propia base de datos garantiza la atomicidad de la operación, evitando que dos peticiones puedan reservar simultáneamente el mismo pedido.
2.2. Llamada al proveedor
Solamente la petición que consigue reservar el pedido continúa con el proceso y ejecuta la llamada al proveedor.
La llamada se realiza fuera de la transacción de base de datos utilizada para reservar el pedido. De esta manera, no mantenemos una transacción abierta mientras esperamos una respuesta externa.
La segunda petición no llega a ejecutar el proceso de comunicación con el proveedor.
2.3. Actualización del resultado
Una vez obtenida la respuesta del proveedor, el servicio actualiza los datos del pedido mediante una nueva transacción:
·	Coste de envío.
·	Importe total.
·	Estado final CONFIRMED.
Si se produce un error durante la confirmación, el pedido pasa a CONFIRMATION_FAILED, sin aplicar una actualización parcial de los importes.
3. Procedimiento de prueba
Para comprobar el comportamiento ante dos confirmaciones simultáneas se utiliza el mismo identificador de pedido en dos peticiones HTTP lanzadas prácticamente al mismo tiempo.
Desde el terminal se ejecutan:
(time curl -s -i -X POST http://localhost:8081/api/pedidos/1/confirmacion) & (time curl -s -i -X POST http://localhost:8081/api/pedidos/1/confirmacion) & wait
Las dos peticiones intentan confirmar el pedido 1.
3.1. Comportamiento esperado
1.	La primera petición consigue cambiar el estado de CREATEDa CONFIRMING.
2.	La segunda petición intenta realizar la misma actualización, pero ya no encuentra el estado inicial CREATED.
3.	La segunda petición no continúa con la confirmación.
4.	Solamente la primera petición ejecuta el proceso de comunicación con el proveedor.
3.2. Evidencias que deben verificarse
Para considerar superada la prueba, se deben comprobar los siguientes puntos:
·	Las respuestas HTTP de ambas peticiones: una debe reflejar la continuación del proceso y la otra el rechazo de la confirmación duplicada.
·	Los registros de ejecución deben mostrar una única entrada en el proceso de confirmación autorizado.
·	No debe iniciarse una segunda confirmación independiente para el mismo pedido.
·	En base de datos debe existir un único resultado coherente: el pedido confirmado con sus importes correspondientes, o bien marcado como fallido si el proveedor no responde correctamente.
4. Resultado y garantías
El mecanismo de reserva mediante actualización condicional permite evitar que dos peticiones simultáneas inicien dos confirmaciones independientes del mismo pedido.
La protección se basa en el estado persistido en base de datos y no en una comprobación previa realizada únicamente en memoria. Por tanto, no depende de que las peticiones se ejecuten en el mismo hilo de aplicación.
Nota:este control de concurrencia es independiente del mecanismo de reintentos y de la idempotencia frente al proveedor. El primero impide que dos peticiones concurrentes reserven el mismo pedido; la idempotencia permite evitar que los reintentos de una misma operación provoquen procesamientos duplicados en el proveedor.

