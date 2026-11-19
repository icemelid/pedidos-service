# Manual de ejecución de la aplicación y de las pruebas

## 1. Requisitos previos

Para ejecutar el proyecto se necesita:

- Java JDK 21.
- Apache Maven 3.9 o superior.
- Git.
- Un entorno de desarrollo compatible con Java. El proyecto se ha desarrollado y probado en GitHub Codespaces con Visual Studio Code.

La aplicación utiliza Spring Boot y una base de datos H2 en memoria, por lo que no es necesario instalar ni configurar un servidor de base de datos externo.

## 2. Obtener el proyecto

Clonar el repositorio Git:

```bash
git clone URL_DEL_REPOSITORIO
```

Acceder al directorio del proyecto:

```bash
cd pedidos-service
```

Si el repositorio contiene una carpeta adicional para el proyecto Maven, acceder a ella hasta localizar el archivo `pom.xml`.

## 3. Comprobar el entorno

Verificar la versión de Java:

```bash
java -version
```

Verificar la instalación de Maven:

```bash
mvn -version
```

El entorno debe utilizar Java 21.

## 4. Compilar el proyecto

Desde el directorio que contiene el archivo `pom.xml`, ejecutar:

```bash
mvn clean compile
```

Este comando:

- Elimina los resultados de compilaciones anteriores.
- Compila el código fuente.
- Comprueba que las clases y dependencias necesarias sean correctas.

Si la compilación termina correctamente, Maven mostrará:

```text
BUILD SUCCESS
```

## 5. Iniciar la aplicación

Ejecutar:

```bash
mvn spring-boot:run
```

Spring Boot iniciará el servidor de aplicaciones.

La aplicación utiliza el puerto `8081`.

Cuando el arranque haya finalizado correctamente, aparecerá un mensaje similar a:

```text
Started PedidosServiceApplication
```

La aplicación permanecerá ejecutándose en ese terminal.

**Importante:** no cerrar ese terminal mientras se quieran realizar peticiones HTTP o pruebas manuales.

Para detener la aplicación, utilizar:

```text
Ctrl + C
```

## 6. Ejecutar una prueba manual de confirmación

Con la aplicación iniciada, abrir un segundo terminal y ejecutar:

```bash
curl -i -X POST http://localhost:8081/api/pedidos/1/confirmacion
```

Esta petición solicita la confirmación del pedido identificado con el número `1`.

La respuesta HTTP y los registros de ejecución permiten comprobar el comportamiento del servicio.

La confirmación puede finalizar correctamente o devolver un error controlado, dependiendo del estado del pedido y del resultado de la comunicación simulada con el proveedor.

## 7. Ejecutar los tests automatizados

Para ejecutar todos los tests del proyecto:

```bash
mvn test
```

Maven se encarga de:

1. Compilar el código principal.
2. Compilar el código de pruebas.
3. Detectar las clases de test.
4. Ejecutar las pruebas unitarias y de integración configuradas.
5. Mostrar el resultado de cada ejecución.

Si todas las pruebas finalizan correctamente, se mostrará:

```text
BUILD SUCCESS
```

### 7.1. Ejecutar una clase de test concreta

Para ejecutar únicamente la clase `PedidoServiceTest`:

```bash
mvn -Dtest=PedidoServiceTest test
```

### 7.2. Ejecutar los tests de nuevo desde cero

Para limpiar los resultados anteriores y ejecutar nuevamente toda la batería de pruebas:

```bash
mvn clean test
```

## 8. Consultar los resultados de las pruebas

Maven genera los informes de ejecución en:

```text
target/surefire-reports/
```

En este directorio se pueden consultar los resultados de las pruebas, incluyendo:

- Número de tests ejecutados.
- Número de pruebas satisfactorias.
- Número de fallos.
- Número de errores.
- Tiempo de ejecución.

Los archivos de resultados tienen normalmente los formatos `.txt` y `.xml`.

## 9. Configuración de la base de datos

El proyecto utiliza H2 en memoria.

La configuración se encuentra en:

```text
src/main/resources/application.properties
```

La configuración principal es:

```properties
spring.datasource.url=jdbc:h2:mem:pedidosdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

La base de datos se crea en memoria durante el arranque de la aplicación.

Esto permite ejecutar las pruebas sin depender de una instalación externa de base de datos.

**Nota:** al detener la aplicación, se pierde el contenido de la base de datos en memoria. Al iniciar una nueva ejecución se crea una nueva instancia.

## 10. Secuencia recomendada de ejecución

Para una comprobación completa del proyecto, se recomienda seguir este orden:

| Orden | Operación | Comando |
|---|---|---|
| 1 | Comprobar Java y Maven | `java -version` / `mvn -version` |
| 2 | Limpiar y compilar | `mvn clean compile` |
| 3 | Ejecutar los tests | `mvn test` |
| 4 | Iniciar la aplicación | `mvn spring-boot:run` |
| 5 | Realizar una petición manual | `curl -i -X POST ...` |
| 6 | Detener la aplicación | `Ctrl + C` |

## 11. Criterio de validación

Se considera que la ejecución técnica ha finalizado correctamente cuando:

- El proyecto compila sin errores.
- La aplicación arranca correctamente.
- El servidor queda disponible en el puerto configurado.
- Los tests automatizados finalizan sin fallos.
- Las peticiones manuales producen las respuestas esperadas según el escenario probado.

