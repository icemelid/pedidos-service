# Prueba técnica Java Senior — Servicio de pedidos e integración logística

## 1. Arquitectura y modelo de datos

### Objetivo

Desarrollar un servicio REST con Spring Boot para gestionar pedidos de clientes e integrarse con un proveedor logístico externo que calcula los costes de envío.

La solución utiliza una arquitectura por capas, separando las entidades de persistencia, la lógica de negocio, los controladores REST y la integración externa.

### 1.1. Creación de las entidades JPA

Hemos comenzado definiendo el modelo de datos mediante tres entidades:

- **Cliente:** representa al cliente que realiza el pedido.
- **Pedido:** representa la cabecera del pedido, incluyendo cliente, estado, fecha e importes.
- **LineaPedido:** representa cada producto incluido en un pedido, con su cantidad y precio unitario.

### 1.2. Relaciones entre entidades

Hemos definido las siguientes relaciones:

- Un cliente puede tener varios pedidos: `@OneToMany` / `@ManyToOne`.
- Un pedido puede contener varias líneas: `@OneToMany` / `@ManyToOne`.

En la relación Pedido–LineaPedido hemos utilizado una asociación bidireccional.

La entidad `LineaPedido` es el lado propietario porque contiene la clave foránea `pedido_id`, definida mediante `@JoinColumn`.

En `Pedido` utilizamos `mappedBy = "pedido"` para indicar que la relación ya está gestionada desde el atributo `pedido` de `LineaPedido`.

### 1.3. Estrategia de carga

Hemos utilizado `FetchType.LAZY` en las relaciones para evitar cargar automáticamente entidades relacionadas cuando no son necesarias.

Esto permite controlar mejor las consultas a la base de datos y evitar recuperaciones innecesarias de información.

### 1.4. Estados del pedido

Hemos definido un enumerado `EstadoPedido` para representar el ciclo de vida del pedido:

- `CREATED`: pedido creado.
- `CONFIRMING`: confirmación en curso.
- `CONFIRMED`: pedido confirmado.
- `CONFIRMATION_FAILED`: error durante la confirmación logística.

Utilizamos `@Enumerated(EnumType.STRING)` para almacenar el nombre del estado en la base de datos, evitando los problemas de utilizar posiciones numéricas.

### 1.5. Constructores y reflexión

Las entidades JPA incluyen un constructor sin argumentos, tal como exige la especificación JPA.

Hibernate utiliza mecanismos de reflexión e instanciación dinámica para crear objetos de las entidades cuando recupera información de la base de datos.

La reflexión permite inspeccionar y utilizar clases, atributos y constructores durante la ejecución.

---

## 2. Próximos componentes

Pendientes de implementar:

1. Completar las entidades y sus relaciones bidireccionales.
2. DTOs de entrada y salida.
3. Repositorios JPA.
4. Servicio de pedidos y cálculo de importes.
5. Controladores REST.
6. Cliente HTTP para el proveedor logístico.
7. Gestión de errores, timeout y reintentos.
8. Control de concurrencia en las confirmaciones.
9. Pruebas unitarias y de integración.
10. README con decisiones técnicas y transiciones de estados.
