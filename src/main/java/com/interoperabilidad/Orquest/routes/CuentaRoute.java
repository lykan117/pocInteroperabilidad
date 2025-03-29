package com.interoperabilidad.Orquest.routes;

import com.interoperabilidad.Orquest.dto.CuentaDTO;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CuentaRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CuentaRoute.class);

    @Override
    public void configure() {

        // 🔧 Global error handler con reintentos automáticos
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(3000)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .onRedelivery(exchange -> {
                    int counter = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class) != null ?
                            exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class) + 1 : 1;
                    log.warn("🔁 Reintentando operación... intento {}", counter);
                })
        );

        restConfiguration()
                .contextPath("/api")
                .port(8081)
                .bindingMode(org.apache.camel.model.rest.RestBindingMode.json);

        from("direct:dlq")
                .log("📦 DLQ: ${body}")
                .log("🚨 Mensaje enviado a DLQ tras fallos.");

        from("direct:eventBus")
                .multicast()
                .to("direct:metricas", "direct:notificacion", "direct:auditoria");

        from("direct:metricas")
                .log("📊 Evento para métricas: ${body}");

        from("direct:notificacion")
                .log("📨 Evento de notificación (simulado): ${body}");

        from("direct:auditoria")
                .log("🗂️ Evento de auditoría (simulado): ${body}");

        from("direct:exito")
                .log("✅ Evento de éxito: ${body}");

        from("direct:error")
                .log("❌ Evento de error: ${body}");

        rest("/procesarCuenta")
                .post()
                .consumes("application/json")
                .to("direct:procesarCuenta");

        from("direct:procesarCuenta")
                .routeId("orquestadorCuenta")
                .log("📥 Mensaje recibido: ${body}")
                .doTry()
                .log("🌐 Llamando al CRUD externo...")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .marshal().json(JsonLibrary.Jackson)
                .to("http://localhost:8080/cuentas?bridgeEndpoint=true&throwExceptionOnFailure=true&connectTimeout=2000&socketTimeout=2000")
                .log("✅ Llamada exitosa al CRUD")
                .to("direct:eventBus")
                .to("direct:exito")
                .doCatch(Exception.class)
                .log("⚠️ Error llamando al CRUD: ${exception.message}")
                .to("direct:error")
                .to("direct:dlq")
                .end();

        rest("/cuenta")
                .post()
                .type(CuentaDTO.class)
                .to("direct:crearCuenta")
                .get()
                .to("direct:leerTodas")
                .get("/{numeroCuenta}")
                .to("direct:leerUna")
                .put("/{numeroCuenta}")
                .type(CuentaDTO.class)
                .to("direct:actualizarCuenta")
                .delete("/{numeroCuenta}")
                .to("direct:eliminarCuenta");

        from("direct:crearCuenta")
                .log("📥 [Crear] ${body}")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .marshal().json(JsonLibrary.Jackson)
                .to("http://localhost:8080/cuentas?bridgeEndpoint=true")
                .convertBodyTo(String.class)
                .to("direct:eventBus")
                .to("direct:exito");

        from("direct:leerTodas")
                .log("📥 [Leer todas]")
                .to("http://localhost:8080/cuentas?bridgeEndpoint=true&httpMethod=GET")
                .convertBodyTo(String.class)
                .to("direct:eventBus");

        from("direct:leerUna")
                .routeId("leerCuenta")
                .log("📥 [Leer una] númeroCuenta = ${header.numeroCuenta}")
                .doTry()
                .toD("http://localhost:8080/cuentas/${header.numeroCuenta}?bridgeEndpoint=true&throwExceptionOnFailure=true&connectTimeout=2000&socketTimeout=2000")
                .convertBodyTo(String.class)
                .to("direct:eventBus")
                .doCatch(Exception.class)
                .log("🚨 Error capturado al leer cuenta: ${exception.message}")
                .to("direct:dlq")
                .end();

        from("direct:actualizarCuenta")
                .log("📥 [Actualizar] ${body}")
                .setHeader("Content-Type", constant("application/json"))
                .marshal().json(JsonLibrary.Jackson)
                .toD("http://localhost:8080/cuentas/${header.numeroCuenta}?bridgeEndpoint=true&httpMethod=PUT")
                .convertBodyTo(String.class)
                .to("direct:eventBus")
                .to("direct:exito");

        from("direct:eliminarCuenta")
                .log("📥 [Eliminar] númeroCuenta = ${header.numeroCuenta}")
                .toD("http://localhost:8080/cuentas/${header.numeroCuenta}?bridgeEndpoint=true&httpMethod=DELETE")
                .convertBodyTo(String.class)
                .to("direct:eventBus")
                .to("direct:exito");
    }
}
