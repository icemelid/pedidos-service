# Documentación de pruebas – pedidos-service

## 1. Objetivo

El objetivo de las pruebas es verificar el correcto funcionamiento del servicio de gestión y confirmación de pedidos, prestando especial atención a:

- El cálculo del importe total.
- La gestión de errores del proveedor externo.
- La aplicación de timeout y reintentos.
- La persistencia correcta del estado del pedido.
- La integridad de los importes ante operaciones fallidas.

Se han implementado pruebas unitarias y pruebas de integración utilizando JUnit 5, Mockito, Spring Boot Test y una base de datos H2 en memoria.

---

## 2. Pruebas unitarias: cálculo del importe total

**Clase:** `PedidoServiceTest.java`

**Ubicación:** `src/test/java/com/example/pedidos/service/`

### Objetivo

Verificar que el importe total de un pedido se calcula correctamente a partir del importe de los productos y el coste de envío.

La operación se ha aislado en el método:

```java
BigDecimal calcularImporteTotal(
        BigDecimal importeProductos,
        BigDecimal costeEnvio) {

    return importeProductos.add(costeEnvio);
}
```

### Casos comprobados

| Caso | Productos | Envío | Resultado esperado |
|---|---:|---:|---:|
| Pedido con envío | 100,00 € | 5,95 € | 105,95 € |
| Pedido sin coste de envío | 50,00 € | 0,00 € | 50,00 € |
| Pedido sin importe de productos | 0,00 € | 5,95 € | 5,95 € |

### Resultado

3 pruebas ejecutadas correctamente, sin fallos ni errores.

---

## 3. Prueba de integración: error del proveedor

**Clase:** `PedidoProveedorErrorIntegrationTest.java`

**Ubicación:** `src/test/java/com/example/pedidos/integration/`

### Objetivo

Comprobar que el servicio gestiona correctamente los errores producidos por el proveedor externo y que el pedido no queda confirmado cuando se agotan los reintentos.

### Escenario de prueba

1. Se crea un cliente y se persiste en H2.
2. Se crea un pedido asociado al cliente, inicialmente en estado `CREATED`.
3. Se configura un proveedor simulado mediante Mockito (`@MockitoBean`).
4. El proveedor devuelve una excepción en cada llamada.
5. Se ejecuta `confirmarPedido(pedidoId)`.
6. Se comprueba que el servicio realiza exactamente tres llamadas al proveedor.
7. Se verifica el estado final del pedido recuperándolo desde la base de datos.

### Comprobaciones

- Se realizan exactamente tres llamadas al proveedor.
- Se agotan los reintentos.
- El servicio lanza una `IllegalStateException`.
- El pedido termina en estado `CONFIRMATION_FAILED`.
- `costeEnvio` permanece a `null`.
- `importeTotal` permanece a `null`.
- `importeProductos` conserva su valor original.

### Resultado obtenido

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
Time elapsed: 10.42 s
```

**Conclusión:** un error reiterado del proveedor no provoca la confirmación del pedido ni modifica sus importes.

---

## 4. Prueba de integración: timeout del proveedor

**Clase:** `PedidoProveedorTimeoutIntegrationTest.java`

**Ubicación:** `src/test/java/com/example/pedidos/integration/`

### Objetivo

Verificar que el servicio detecta el incumplimiento del tiempo máximo de respuesta del proveedor, cancela la tarea de espera y aplica correctamente la política de reintentos.

### Configuración

- Timeout máximo por intento: 6 segundos.
- Número máximo de intentos: 3.
- Intervalo entre reintentos: 500 milisegundos.
- Tiempo simulado de respuesta del proveedor: 7 segundos.

Se utiliza un proveedor simulado independiente mediante Mockito. No se modifica la implementación `ProveedorClientLentoSimulado` utilizada por la aplicación.

### Escenario de prueba

1. Se crea y persiste un cliente en H2.
2. Se crea un pedido asociado, con estado inicial `CREATED`.
3. Se configura el proveedor simulado para que tarde 7 segundos en responder.
4. Se ejecuta `confirmarPedido(pedidoId)`.
5. El servicio espera un máximo de 6 segundos por intento.
6. Al producirse el timeout, se solicita la cancelación de la tarea mediante `future.cancel(true)`.
7. Se aplica el intervalo de 500 milisegundos y se repite el intento.
8. Tras agotar los tres intentos, el servicio lanza una excepción.
9. Se recupera el pedido desde H2 y se comprueba su estado e importes.

### Comportamiento de timeout implementado

```java
try {
    return future.get(6, TimeUnit.SECONDS);

} catch (TimeoutException ex) {

    future.cancel(true);

    throw new IllegalStateException(
        "Timeout: el proveedor ha superado los 6 segundos.",
        ex
    );
}
```

### Comprobaciones

- Se producen tres llamadas al proveedor.
- Cada intento queda limitado a 6 segundos de espera.
- Se solicita la cancelación de cada tarea que excede el límite.
- Se agotan los tres intentos.
- La excepción final identifica el agotamiento de los intentos.
- La causa de la excepción corresponde a un timeout.
- El pedido termina en estado `CONFIRMATION_FAILED`.
- `costeEnvio` e `importeTotal` permanecen a `null`.
- El importe original de productos no se modifica.

### Resultado obtenido

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
Time elapsed: 27.35 s
```

**Conclusión:** el servicio controla los tiempos de espera del proveedor y evita confirmar un pedido cuando no se obtiene respuesta dentro del límite establecido.

Nota técnica: `Future.cancel(true)` solicita la interrupción de la tarea, pero no garantiza que un proveedor externo real detenga un procesamiento que ya haya iniciado. El simulador de esta prueba respeta la interrupción.

---

## 5. Entorno de pruebas

| Elemento | Tecnología |
|---|---|
| Framework de pruebas | JUnit 5 |
| Simulación de dependencias | Mockito |
| Integración con Spring | Spring Boot Test |
| Persistencia | Spring Data JPA |
| Base de datos | H2 en memoria |
| Gestión de dependencias | Maven |

Las pruebas de integración utilizan el contexto real de Spring y los repositorios JPA, sustituyendo únicamente el proveedor externo mediante Mockito.

La base de datos H2 permite ejecutar los escenarios sin depender de una base de datos externa.

---

## 6. Resumen de resultados

| Tipo de prueba | Pruebas | Resultado |
|---|---:|---|
| Unitarias: cálculo del importe | 3 | Correctas |
| Carga del contexto Spring | 1 | Correcta |
| Integración: error del proveedor | 1 | Correcta |
| Integración: timeout | 1 | Correcta |
| **Total identificado** | **6** | **Correctas** |

También se ha ejecutado la batería completa de pruebas mediante:

```bash
mvn clean test
```

**Resultado:** `BUILD SUCCESS`.

---

## 7. Conclusiones

Las pruebas realizadas permiten verificar los siguientes aspectos del desarrollo:

1. El cálculo del importe total funciona correctamente en los escenarios comprobados.
2. Los errores del proveedor se gestionan mediante una política de hasta tres intentos.
3. Los intentos fallidos no provocan la confirmación del pedido.
4. El timeout limita a seis segundos la espera por cada respuesta del proveedor.
5. Se solicita la cancelación de las tareas que exceden el tiempo máximo.
6. Los fallos quedan reflejados en el estado persistido `CONFIRMATION_FAILED`.
7. Los importes originales del pedido se mantienen intactos cuando la confirmación falla.

Las pruebas constituyen evidencia funcional del comportamiento del servicio ante errores y falta de respuesta del proveedor.

**Alcance:** estas pruebas verifican los escenarios de cálculo, error y timeout. No sustituyen las pruebas específicas de concurrencia entre dos peticiones simultáneas ni una prueba contra un proveedor HTTP real.