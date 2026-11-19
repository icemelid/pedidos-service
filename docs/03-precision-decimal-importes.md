# Precisión decimal en importes monetarios – pedidos-service

## 1. Objetivo

Garantizar que los importes monetarios utilizados en la gestión de pedidos se representan, almacenan y calculan con precisión decimal adecuada, evitando los errores de aproximación asociados a los tipos de coma flotante.

La solución implementada utiliza `BigDecimal` en Java y columnas decimales con precisión y escala definidas en la base de datos.

## 2. Representación de los importes en Java

Todos los campos monetarios de la entidad `Pedido` utilizan el tipo `BigDecimal`:

```java
private BigDecimal importeProductos;

private BigDecimal costeEnvio;

private BigDecimal importeTotal;
```

Se ha elegido `BigDecimal` en lugar de `float` o `double` porque estos últimos utilizan representación binaria de coma flotante y pueden introducir aproximaciones en operaciones con valores decimales.

`BigDecimal` permite realizar operaciones aritméticas decimales con precisión controlada.

## 3. Precisión y escala en base de datos

Los campos monetarios están definidos mediante anotaciones JPA:

```java
@Column(nullable = false, precision = 12, scale = 2)
private BigDecimal importeProductos;

@Column(precision = 12, scale = 2)
private BigDecimal costeEnvio;

@Column(precision = 12, scale = 2)
private BigDecimal importeTotal;
```

La configuración establece:

- **Precisión: 12 dígitos en total.**
- **Escala: 2 posiciones decimales.**

Esto permite almacenar importes con hasta diez dígitos en la parte entera y dos en la parte decimal.

La definición corresponde a un tipo decimal equivalente a `DECIMAL(12,2)`.

Además, `importeProductos` se declara como obligatorio mediante `nullable = false`, mientras que `costeEnvio` e `importeTotal` pueden permanecer sin valor mientras el pedido no haya sido confirmado.

## 4. Cálculo del importe total

El cálculo del importe total se realiza mediante una operación de suma entre dos objetos `BigDecimal`.

Se ha aislado la operación en el método:

```java
BigDecimal calcularImporteTotal(
        BigDecimal importeProductos,
        BigDecimal costeEnvio) {

    return importeProductos.add(costeEnvio);
}
```

Durante la confirmación del pedido se utiliza el resultado para actualizar el importe total:

```java
pedido.setImporteTotal(
        calcularImporteTotal(
                pedido.getImporteProductos(),
                costeEnvio
        )
);
```

De esta manera, el cálculo evita conversiones a tipos de coma flotante y conserva la representación decimal de los valores utilizados.

## 5. Pruebas unitarias de precisión decimal

Se han implementado pruebas unitarias específicas en `PedidoServiceTest.java` para comprobar el cálculo del importe total.

Los escenarios comprobados son:

| Importe productos | Coste envío | Importe total esperado |
|---:|---:|---:|
| 100,00 € | 5,95 € | 105,95 € |
| 50,00 € | 0,00 € | 50,00 € |
| 0,00 € | 5,95 € | 5,95 € |

Las comparaciones se realizan utilizando `BigDecimal` y comprobando el resultado esperado.

### Resultado

```text
Tests run: 3
Failures: 0
Errors: 0
Skipped: 0
```

Las tres pruebas han finalizado correctamente.

## 6. Uso de valores decimales en las pruebas

Para evitar introducir aproximaciones al construir los valores de prueba, los importes se inicializan utilizando cadenas decimales:

```java
new BigDecimal("100.00");
new BigDecimal("5.95");
```

Este enfoque evita construir los importes a partir de valores `double`, cuya representación binaria puede introducir diferencias no deseadas.

## 7. Conclusiones

La implementación cumple el requisito de precisión decimal adecuada mediante las siguientes decisiones técnicas:

1. Utilización de `BigDecimal` para todos los importes monetarios.
2. Definición de precisión 12 y escala 2 en las columnas correspondientes.
3. Operaciones aritméticas mediante los métodos de `BigDecimal`, evitando `float` y `double`.
4. Inicialización de valores decimales mediante cadenas en las pruebas.
5. Verificación del cálculo mediante pruebas unitarias.

Con esta solución se evita introducir errores de aproximación propios de la representación binaria de coma flotante en los cálculos monetarios del servicio.

**Alcance:** la implementación utiliza dos posiciones decimales para los importes. No se ha definido una política adicional de redondeo para conversiones de moneda, impuestos u operaciones que requieran escalas diferentes.