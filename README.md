# kafka-spring

Proyecto de referencia mínimo para conectar una aplicación Spring Boot con Apache Kafka.

**Descripción:**
- Ejemplo sencillo que muestra cómo producir y consumir mensajes usando Spring Kafka.
- Crea el `topic` `l2ap-topic` desde la configuración y envía mensajes desde `CursoKafkaSpringApplication`.

**Archivos clave:**
- `CursoKafkaSpringApplication` (punto de entrada, producer y listener): [src/main/java/com/devl2ap/kafka/CursoKafkaSpringApplication.java](src/main/java/com/devl2ap/kafka/CursoKafkaSpringApplication.java#L1-L200)
- `KafkaConfiguration` (beans de producer/consumer, topic, métricas): [src/main/java/com/devl2ap/kafka/config/KafkaConfiguration.java](src/main/java/com/devl2ap/kafka/config/KafkaConfiguration.java#L1-L300)
- `application.properties` (config de la aplicación): [src/main/resources/application.properties](src/main/resources/application.properties#L1-L20)

Requisitos
- Java 11+ (o la versión configurada en el proyecto)
- Maven (o usar el wrapper `./mvnw` en macOS)
- Docker & Docker Compose (para ejecutar Kafka localmente)
- Recomendado: Apache Kafka 4.1.2 (modo KRaft para nuevos despliegues)

Inicio rápido (Kafka con Docker Compose - modo KRaft)

1) Crear un `docker-compose.yml` mínimo (KRaft, single-node):

```yaml
version: '3.8'
services:
  kafka:
    image: bitnami/kafka:4.1.2
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_CFG_PROCESS_ROLES: broker,controller
      KAFKA_CFG_NODE_ID: 1
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      ALLOW_PLAINTEXT_LISTENER: "yes"
    healthcheck:
      test: ["CMD", "bash", "-lc", "kafka-topics.sh --bootstrap-server localhost:9092 --list || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10

```
2) Levantar la pila:

```bash
docker-compose up -d
```

3) Crear el topic:

```bash
docker-compose exec kafka kafka-topics.sh --create --topic l2ap-topic --bootstrap-server localhost:9092 --partitions 5 --replication-factor 1
```

4) Verificar topics:

```bash
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

Probar con producer/consumer de consola

Producer (con Docker Compose):

```bash
docker-compose exec kafka kafka-console-producer --topic l2ap-topic --bootstrap-server localhost:9092
```

Consumer (desde el inicio, con Docker Compose):

```bash
docker-compose exec kafka kafka-console-consumer --topic l2ap-topic --from-beginning --bootstrap-server localhost:9092
```

Nota sobre ZooKeeper y KRaft (modo sin ZooKeeper)

- Históricamente Apache Kafka dependía de ZooKeeper para coordinar el clúster. Muchas guías antiguas y imágenes Docker usan ZooKeeper.
- En versiones recientes de Apache Kafka (KIP-500, KRaft) es posible ejecutar brokers sin ZooKeeper: el modo KRaft incorpora la funcionalidad de control en el propio broker. Para nuevos proyectos se recomienda evaluar KRaft y las imágenes/distribuciones que lo soporten.
- Mantengo el ejemplo con `docker-compose` basado en la configuración tradicional para compatibilidad y demostración; si prefieres una configuración KRaft (sin ZooKeeper) puedo añadir un `docker-compose.yml` alternativo o instrucciones para arrancar un broker KRaft usando las imágenes/distribuciones que lo soporten.

Si usas una distribución antigua o Confluent Platform en versiones que requieren ZooKeeper, conserva la sección anterior. Para despliegues nuevos, planifica usar KRaft cuando tu distribución y flujo de trabajo lo permitan.

Cómo funciona este proyecto (resumen técnico)

- En [KafkaConfiguration](src/main/java/com/devl2ap/kafka/config/KafkaConfiguration.java#L1-L300) se declaran:
	- `NewTopic l2apTopic()` — crea el topic `l2ap-topic` con 5 particiones.
	- `KafkaTemplate<String,String>` — bean para enviar mensajes.
	- `ConcurrentKafkaListenerContainerFactory` y `ConsumerFactory` — configuración del consumer, con `batchListener` y `concurrency` configurados.
	- `MeterRegistry` con Prometheus para métricas.

- En [CursoKafkaSpringApplication](src/main/java/com/devl2ap/kafka/CursoKafkaSpringApplication.java#L1-L200):
	- Método `run(...)` (implementa `CommandLineRunner`) envía 100 mensajes al topic `l2ap-topic` al arrancar.
	- Método `listen(...)` anotado con `@KafkaListener` recibe batches de `ConsumerRecord` y los procesa.
	- Hay un `@Scheduled` (comentado el envío) y ejemplos de envío síncrono/asincrónico en comentarios.

Ejemplo de uso (paso a paso)

1) Asegúrate de que Kafka esté corriendo en `localhost:9092` (ver sección Docker arriba).
2) Construir y ejecutar la app con Maven:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

3) Al inicio, la app enviará 100 mensajes desde `run(...)` y el `@KafkaListener` los procesará en batches (ver logs).

Configuración y cambios

- Actualmente `KafkaConfiguration` define `localhost:9092` estáticamente. Para usar otra dirección, modifica `producerProperties()` y `consumerProperties()` o externaliza con `@Value("${spring.kafka.bootstrap-servers}")`.
- Logging: los niveles se controlan en [application.properties](src/main/resources/application.properties#L1-L20).

Notas y recomendaciones

- Para entornos reales, usa variables de entorno o `application.yml` con seguridad (no codificar servidores en clases Java).
- Ajusta `concurrency` y `partitions` según carga y capacidad.
- Habilita métricas Prometheus si quieres monitorizar producción.

¿Quieres que:
- ejecute el proyecto localmente y valide que los mensajes se envían y consumen?
- agregue un `docker-compose.yml` al repo con la configuración mostrada?

