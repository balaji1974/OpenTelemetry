# Open Telemetry   

## Service to Monitor 
```xml
Instrumentation - Metrics, Logs, Traces 

Example: A metric shows high latency (something's wrong), 
traces show the slow service (where), 
and logs provide the specific error message or user ID (why). 

``` 

## Create an order service 
```xml
1. Spring Initilizer
Go to spring initilizer page https://start.spring.io/ 
and add the following dependencies: 
Spring Web
DevTools


2. Create a project 'order-service' and download

3. The pom.xml will have the following dependencies: 
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-devtools</artifactId>
	<scope>runtime</scope>
	<optional>true</optional>
</dependency>


4. Create an Order model in the model directory
Create a package called model and inside it create
a Order.java file as follows:

import java.math.BigDecimal;
import java.time.ZonedDateTime;
public record Order(Long id, Long customerId, ZonedDateTime orderTime, BigDecimal totalAmount) {
}

5. Add a controller file inside the controller directory  
Create a package called controller and inside it create
OrderController.java as follows:

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.java.bala.springboot.order_service.model.Order;

@RestController
@RequestMapping ("/orders")
public class OrderController {
	
	@GetMapping ("/{id}")
	public Order findbyId(@PathVariable Long id) {
		return new Order(id, 1L, ZonedDateTime.now(), BigDecimal.TEN);
	}
	

}

Just for illustation purpose, its a simple controller 
that does nothing but takes an order id and creates a java record 
and returns it back with an order time and order amount


6. Check different ways of running the application below
``` 

## Auto Instrumentation - Agent Based

### Run the service as a jar file
```xml
Download open telemetry java agent:
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

Run the service with open telemetry java agent: 
java -javaagent:opentelemetry-javaagent.jar -jar target/order-service-0.0.1-SNAPSHOT.jar 
(Since we are not running open telemetry collector service sperately on port 4318 
we will have an error message Failed to connect to localhost/[0:0:0:0:0:0:0:1]:4318)

Log all metrics on the console:
java -javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=logging -Dotel.metrics.exporter=logging -Dotel.logs.exporter=logging  -jar target/order-service-0.0.1-SNAPSHOT.jar

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'

```

### Run the service directly from eclipse
```xml
Right click on the project -> Run Configuration -> Arguments (tab) -> VM Arguments ->
-javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=logging -Dotel.metrics.exporter=logging -Dotel.logs.exporter=logging

Now right click and run the project 

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'

```

### Run the service as a docker build
```xml
Clean and Build the application

Create a Dockerfile:
FROM eclipse-temurin:17-jre

ADD target/order-service-0.0.1-SNAPSHOT.jar order-service-0.0.1-SNAPSHOT.jar
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:/opentelemetry-javaagent.jar \ 
	-Dotel.traces.exporter=logging \ 
	-Dotel.metrics.exporter=logging \ 
	-Dotel.logs.exporter=logging  \
	-jar /order-service-0.0.1-SNAPSHOT.jar

Create a docker-compose.yml file:
version: '3'
services:
    order-service:
        build: ./
        ports: 
            - "8080:8080"


Make sure docker is running (docker desktop in my case)
Run the following command: 
docker compose up -d

Copy the container id by running the following command:
docker ps

Follow the logs
docker logs -f <container-id>

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'

Finally stop the application:
docker compose down

```

### Optimizing the docker build
```xml
Prevent the docker image from downloading otel everytime from the internet
Add the following mavel resource plugin 
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-my-file</id>
            <!-- Bind to a phase like validate, generate-resources, or compile -->
            <phase>validate</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <!-- The output directory to copy the resources to -->
                <outputDirectory>${project.build.directory}/agent</outputDirectory>
                <resources>
                    <resource>
                        <!-- The directory where your source file is located -->
                        <directory>${project.basedir}</directory>
                        <includes>
                            <!-- The specific file(s) you want to copy -->
                            <include>opentelemetry-javaagent.jar</include>
                        </includes>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>

Delete the docker image, Clean and Build the application 
and check if opentelemetry-javaagent.jar is copied from the project root folder 
to the /target/agent folder 

Modify the Dockerfile to remove the image from downloading from the github
and adding the environment variables during building of image.

FROM eclipse-temurin:17-jre

ADD target/order-service-0.0.1-SNAPSHOT.jar order-service-0.0.1-SNAPSHOT.jar
ADD target/agent/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:/opentelemetry-javaagent.jar -jar /order-service-0.0.1-SNAPSHOT.jar

Modify thedocker-compose.yml file to add OTel environment varibales:
version: '3'
services:
    order-service:
        build: ./
        environment:
            - OTEL_TRACES_EXPORTER=console
            - OTEL_METRICS_EXPORTER=console
            - OTEL_LOGS_EXPORTER=console
        ports: 
            - "8080:8080"

Make sure docker is running (docker desktop in my case)
Run the following command: 
docker compose up -d

Copy the container id by running the following command:
docker ps

Follow the logs
docker logs -f <container-id>

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'

Finally stop the application:
docker compose down

```

## Create an order service that connects to database
```xml
1. Spring Initilizer
Go to spring initilizer page https://start.spring.io/ 
and add the following dependencies: 
Spring Web
DevTools
Spring JPA
Postgres SQL Driver


2. Create a project 'order-service-jpa' and download

3. The pom.xml will have the following dependencies: 
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-devtools</artifactId>
	<scope>runtime</scope>
	<optional>true</optional>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<scope>runtime</scope>
</dependency>

4. Add the following mavel resource plugin 
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-my-file</id>
            <!-- Bind to a phase like validate, generate-resources, or compile -->
            <phase>validate</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <!-- The output directory to copy the resources to -->
                <outputDirectory>${project.build.directory}/agent</outputDirectory>
                <resources>
                    <resource>
                        <!-- The directory where your source file is located -->
                        <directory>${project.basedir}</directory>
                        <includes>
                            <!-- The specific file(s) you want to copy -->
                            <include>opentelemetry-javaagent.jar</include>
                        </includes>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>

5. Create a docker compose file (docker-compose.yml):
version: '3'
services:
    order-service:
        build: ./
        environment:
            - OTEL_TRACES_EXPORTER=console
            - OTEL_METRICS_EXPORTER=console
            - OTEL_LOGS_EXPORTER=console
        ports: 
            - "8080:8080"
        depends_on:
            - postgres

    postgres:
        container_name: postgres
        image: postgres:latest
        restart: always
        environment:
          - POSTGRES_DB=opentelementry
          - POSTGRES_PASSWORD=secret
          - POSTGRES_USER=myuser
        ports:
          - "5432:5432"

Note: The postgres service added. 

Test if the service creates a database correctly or not by running:
docker compose up postgres -d

6. In the application.properties files add the following database 
connection parameters
spring.application.name=order-service-jpa

spring.sql.init.mode=always

# Running this as application
#spring.datasource.url=jdbc:postgresql://localhost:5432/opentelementry

# Running this inside docker
spring.datasource.url=jdbc:postgresql://postgres:5432/opentelementry
spring.datasource.username=myuser
spring.datasource.password=secret

spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.default_schema=orders

7. Create a docker file and add the following
FROM eclipse-temurin:17-jre

ADD target/order-service-jpa-0.0.1-SNAPSHOT.jar order-service-jpa-0.0.1-SNAPSHOT.jar
ADD target/agent/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:/opentelemetry-javaagent.jar -jar /order-service-jpa-0.0.1-SNAPSHOT.jar

(nothing new here, and it is as per the previous example)

8. Create an Order Entity model in the model directory
Create a package called model and inside it create
a Order.java file as follows:

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="orders")
public class Order {
	
	@Id
	private Long id; 
	
	@Column(name="customer_id")
	private Long customerId;
	
	@Column(name="order_date")
	private ZonedDateTime orderTime;
	
	@Column(name="total_amount")
	private BigDecimal totalAmount;
	
	public Long getId() {
		return id;
	}
	public Long getCustomerId() {
		return customerId;
	}
	public ZonedDateTime getOrderTime() {
		return orderTime;
	}
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}	
}

9. Create an Order Repository in the repository directory
Create a package called repository and inside it create
a OrderRepository.java file as follows:

import org.springframework.data.jpa.repository.JpaRepository;
import com.java.bala.springboot.order_service_jpa.model.Order;
public interface OrderRepository extends JpaRepository<Order, Long> {

}

10. Add a controller file inside the controller directory  
Create a package called controller and inside it create
OrderController.java as follows:

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.java.bala.springboot.order_service_jpa.model.Order;
import com.java.bala.springboot.order_service_jpa.repository.OrderRepository;

@RestController
@RequestMapping ("/orders")
public class OrderController {
	
	private final OrderRepository orderRepository;
	
	public OrderController(OrderRepository orderRepository) {
		super();
		this.orderRepository = orderRepository;
	}

	@GetMapping ("/{id}")
	public Order findbyId(@PathVariable Long id) {
		return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid id :"+ id));
	}
}

Just for illustation purpose, its a simple controller 
that does nothing but takes an order id and fetches the 
Order details from the Postgres database and returns it

12. Create the database schema and data scripts
In the application root folder create the following files:
schema.sql 
CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders (
	id int8 NOT NULL,
	customer_id int8 NULL,
	order_date timestamptz(6) NULL,
	total_amount numeric(38,2) NULL,
	CONSTRAINT order_pkey PRIMARY KEY(id)	
);

data.sql
INSERT INTO orders.orders(id, customer_id, order_date, total_amount) VALUES (1, 1, now(), 10);
INSERT INTO orders.orders(id, customer_id, order_date, total_amount) VALUES (2, 2, now(), 20);
INSERT INTO orders.orders(id, customer_id, order_date, total_amount) VALUES (3, 3, now(), 30);

13. Build and Run the application 
Clean and Build the application and check if 
opentelemetry-javaagent.jar is copied from the project root folder 
to the /target/agent folder 

Make sure the application runs using: 
java -javaagent:opentelemetry-javaagent.jar -Dotel.traces.exporter=logging -Dotel.metrics.exporter=logging -Dotel.logs.exporter=logging

Check if database is created and records inserted into the postgres database
database name: opentelemetry
schema name: orders
table name: orders


14. Next make sure docker is running (docker desktop in my case)
Run the following command: 
docker compose up -d

Copy the container id by running the following command:
docker ps

Follow the logs
docker logs -f <container-id>

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'
curl --location 'http://localhost:8080/orders/2'
curl --location 'http://localhost:8080/orders/3'

Finally stop the application:
docker compose down

``` 

## Push OpenTelemetry trace data to Jeager 
```xml
1. In the pom.xml add the following 2 dependencies. 
<!-- Micrometer Tracing bridge for OTel -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OTel Exporter for Jaeger/OTLP compatibility -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

2. Add the following 2 configurations in the application.properties files:
# Enable OTLP tracing endpoint
management.otlp.tracing.endpoint=http://jaeger:4318/v1/traces

# Set the tracing sampling rate to 100% (for development/testing)
management.tracing.sampling.probability=1.0

3. Modify the Dockerfile to remove the javaagent opentel dependency:

ADD target/order-service-jpa-0.0.1-SNAPSHOT.jar .
#ADD target/agent/opentelemetry-javaagent.jar .

#ENTRYPOINT java -javaagent:opentelemetry-javaagent.jar -jar order-service-jpa-0.0.1-SNAPSHOT.jar
ENTRYPOINT java -jar order-service-jpa-0.0.1-SNAPSHOT.jar

4. In the docker-compose.yml file make the following changes:
- OTEL_SERVICE_NAME=order-service-jpa
# - OTEL_TRACES_EXPORTER=console
- OTEL_TRACES_EXPORTER=jaeger

5. Clean and build the application

6. Next make sure docker is running (docker desktop in my case)
Run the following command: 
docker compose up -d

Copy the container id by running the following command:
docker ps

Follow the logs
docker logs -f <container-id>

Excute the curl command to see the logs:
curl --location 'http://localhost:8080/orders/1'
curl --location 'http://localhost:8080/orders/2'
curl --location 'http://localhost:8080/orders/3'\

7. Open the Jagger UI from the below URL:
http://localhost:16686/search

From the services dropdown search for 
order-service-jpa

select and click find traces
You will see the complete list of all http traces. 
Click on the trace and explore

8. Finally stop the application:
docker compose down

``` 

## Push OpenTelemetry trace data to Zipkin 
```xml
1. Run zipkin on docker
Create a docker-compose.yml file
services:
    zipkin:
        image: openzipkin/zipkin:latest
        container_name: zipkin
        ports:
            - "9411:9411"


2. Run the docker zipkin container
docker compose up -d

3. Make sure Zipkin is running:
http://localhost:9411/

4. Create a spring boot app with the following dependencies:
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-devtools</artifactId>
	<scope>runtime</scope>
	<optional>true</optional>
</dependency>

<!-- Spring Boot Actuator for tracing and health endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Bridge Spring/Micrometer Observations to OpenTelemetry -->
<dependency>
	<groupId>io.micrometer</groupId>
	<artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- Export spans to Zipkin -->
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>

5. Export the traces to zipkin by configuring in the application.properties
# Tracing
management.tracing.sampling.probability=1.0
# Export traces to Zipkin
management.tracing.export.zipkin.endpoint=http://localhost:9411/api/v2/spans

6. Create a simple controller:
@RestController
@RequestMapping ("/orders")
public class OrderController {
	
	private final ObservationRegistry observationRegistry;
	
	public OrderController(ObservationRegistry observationRegistry) {
	    this.observationRegistry = observationRegistry;
	}

	@GetMapping ("/{id}")
	public Order findbyId(@PathVariable Long id) {
		return new Order(id, 1L, ZonedDateTime.now(), BigDecimal.TEN);
	}
	
}

7. Run the application
8. Excute the curl command:
curl --location 'http://localhost:8080/orders/1'
curl --location 'http://localhost:8080/orders/2'
curl --location 'http://localhost:8080/orders/3'

9. View the traces on Zipkin
http://localhost:9411/
Click Run Query 

10. Stop the application and bring the docker container down.
docker compose down


``` 

# OTel Collector
```xml
OTel Collection -> 3 Components
1. Receiver
2. Processor 
3. Exporter

The OpenTelemetry Collector offers a vendor-agnostic implementation of how to receive, 
process and export telemetry data. It removes the need to run, operate, and maintain 
multiple agents/collectors. This works with improved scalability and supports open source 
observability data formats (e.g. Jaeger, Prometheus, Fluent Bit, etc.) sending to one or 
more open source or commercial backends.

Objectives
Usability: Reasonable default configuration, supports popular protocols, runs 
and collects out of the box.
Performance: Highly stable and performant under varying loads and configurations.
Observability: An exemplar of an observable service.
Extensibility: Customizable without touching the core code.
Unification: Single codebase, deployable as an agent or collector with support 
for traces, metrics, and logs.

Read everything here: 
https://opentelemetry.io/docs/collector/

```
![alt text](https://github.com/balaji1974/OpenTelemetry/blob/main/otel-collector.svg?raw=true)

## Random Number Generator (random-generator)
```xml
Lets first create a spring boot application that generates a random number
and we will use this application as a base for all our collector examples. 

1. Go to spring initilizer and add the following packages:
Spring Web
Spring boot Actuator
Spring boot DevTools

Save the project as random-generator and download. 
Open it in your IDE. 

2. In the pom.xml you will see the following base dependencies:
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-devtools</artifactId>
	<scope>runtime</scope>
	<optional>true</optional>
</dependency>

3. Create a controller 
Create a package called controller and 
inside it create a RandomNumberGenerator controller class
@RestController
@RequestMapping ("/random")
public class RandomNumberGenerator {

	@GetMapping ("/{id}")
	public Integer findbyId(@PathVariable Integer id) {
		
		// nextInt(int bound) generates a number from 0 up to (but not including) the bound
        return new Random().nextInt(id);
	}
	
}

This is simple controller that returns a random number within 
the range of any number that is sent as query parameter


4. Run the application
Save and run the application 
Excute the curl command to see the random numbers being returned:
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'

5. This class is simple but it will form the base for 
our OTel collector in further examples below

``` 

## Open Telemetry Collectors
Collecting Traces, Metrics and Logs (otel-collector)
```xml
1. Use the base project (random-generator) and create 
a new project called otel-collector

2. Add the following dependency management in the pom.xml
<dependencyManagement>
	<dependencies>
	    <dependency>
	        <groupId>io.opentelemetry.instrumentation</groupId>
	        <artifactId>opentelemetry-instrumentation-bom</artifactId>
	        <version>2.23.0</version>
	        <type>pom</type>
	        <scope>import</scope>
	    </dependency>
	</dependencies>
</dependencyManagement>

Add the following collector dependency 
<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>

3. Clean and Build the application 
Clean and Build the application and check if 
otel-collector-0.0.1-SNAPSHOT.jar is created on the /target folder 

4. Create a docker file
This file does step 3 using docker-compose
Create a docker file with the following contents:

FROM eclipse-temurin:17-jre
ADD target/otel-collector-0.0.1-SNAPSHOT.jar .
ENTRYPOINT java -jar otel-collector-0.0.1-SNAPSHOT.jar

5. Create a docker-compose.yaml file with the following contents:
services:
  random-generator:
    container_name: random-generator
    build: ./
    environment:
      OTEL_SERVICE_NAME: 'random-generator'
      OTEL_EXPORTER_OTLP_ENDPOINT: 'http://otel-collector:4318'
      OTEL_EXPORTER_OTLP_PROTOCOL: 'http/protobuf'
      OTEL_LOGS_EXPORTER: 'otlp'
      OTEL_TRACES_EXPORTER: 'otlp'
      OTEL_METRICS_EXPORTER: 'otlp'
    ports:
      - '8080:8080'
    depends_on:
      - otel-collector
    networks:
      - otel-network
 
  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    container_name: otel-collector
    command: ["--config=/etc/otelcol/config.yaml"]
    ports:
      - "4318:4318"  # OTLP HTTP endpoint
      - "8889:8889"  # Prometheus scrape endpoint
    volumes:
      - ./otel-collector-config.yaml:/etc/otelcol/config.yaml:ro  # Bind mount your config
    networks:
      - otel-network
      
networks:
  otel-network:
    driver: bridge


We have 2 sevices here, first is the random-generator which is our spring boot 
application. It is configured to emit traces, metrics and logs using otlp 
(open telemetry) protocol. All observability stats are exported to a collector 
which runs at http://otel-collector:4318

The open telemetry collector logs all observability stats on to the console. 
This is configured using otel-collector-config.yaml file.

6. Configuring the collector (otel-collector-config.yaml)
receivers:
  otlp:
    protocols:
      http: # listens on 4318 , required for metrics via OTLP/HTTP (port 4318) , it uses the default endpoint: 0.0.0.0:4318, which listens on all interfaces — meaning it's accessible from inside Docker and other hosts as well.
        endpoint: 0.0.0.0:4318
        # recommended especially if you’re using docker for the collector and your app is outside Docker.
      #grpc: # listens on 4317 , optional and good for traces
      
processors:
  memory_limiter:
    check_interval: 1s
    limit_mib: 256
    spike_limit_mib: 64

  batch:
    timeout: 5s
    send_batch_size: 2048
  
  transform:
    metric_statements:
      - context: datapoint
        statements:
        - set(attributes["host"], resource.attributes["host.name"])
        - set(attributes["service"], resource.attributes["service.name"])
        - set(attributes["container"], resource.attributes["container.id"])

exporters:
  debug:
    verbosity: detailed       # Logs incoming metrics and traces , Add debug exporter in your Collector config to visually verify metrics
    # Then when you run the collector, it will log received metrics to the console.

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [memory_limiter, batch , transform]
      exporters: [debug]

    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch , transform]
      exporters: [debug]
    
    logs:
      receivers: [otlp]
      exporters: [debug]


Here we have 3 main section, the receiver, processor and exporter.
The receiver receives all observability stats from spring boot application, 
the processor transforms, filters, or enriches telemetry data,
while the exporter exports the data to the receiving application. In our case
we export it to the console for now but will later add other monitoring applications.

7. Run the application from the project root directory
docker compose up -d

8. Excute the curl command to see the random numbers being returned:
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'

9. Check the logs of the collector
docker logs  -f -n100 otel-collector
We can see all metrics, logs and traces collected by our collector

10. Close the application
docker compose down

```

## Open Telemetry Collectors - Export traces to Zipkin & Jaeger
Collecting Traces (otel-trace-exporter)
```xml
Create spring boot application otel-trace-exporter and 
following steps 1 to 9 as otel-collector application (previous steps)
Check if all monitoring parameters are pushed to otel-collector 
docker logs  -f -n100 otel-collector

Lets now push tracing to Zipkin and Jaeger 
1. In docker-compose.yaml file add the following services:

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - otel-network
      
  jaeger:
    image: jaegertracing/jaeger:latest
    container_name: jaeger
    ports:
      - "16686:16686"
      - "4317:4317"
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    networks:
      - otel-network

2. Under the otel-collector service in docker-compose.yaml add the following dependency:
    depends_on:
      - zipkin
      - jaeger
This makes sure otel-collector is started only after zipkin and jaeger containers start.

3. In the otel-collector-config.yaml add the following under the exporter
to indicate the collector to export data to zipkin and jaeger (otlp supported export)
exporters:
  debug:
    verbosity: detailed       # Logs incoming metrics and traces , Add debug exporter in your Collector config to visually verify metrics
    # Then when you run the collector, it will log received metrics to the console.
  zipkin:
    endpoint: "http://zipkin:9411/api/v2/spans"
  otlp: # Jaeger supports OTLP directly. The default port for OTLP/gRPC is 4317
    endpoint: "jaeger:4317"
    tls:
      insecure: true # Use insecure connection if not using TLS


4. In the otel-collector-config.yaml add the following under the service -> pipeline 
-> traces -> exporters

service:
  pipelines:
    traces:
      exporters: [zipkin,otlp,debug]

* Note do not make any changes to other configurations already existing in this section.

5. Clean and Build the application 
Clean and Build the application and check if 
otel-trace-exporter-0.0.1-SNAPSHOT.jar is created on the /target folder 

6. Create a Dockerfile to build the container
FROM eclipse-temurin:17-jre
ADD target/otel-trace-exporter-0.0.1-SNAPSHOT.jar .
ENTRYPOINT java -jar otel-trace-exporter-0.0.1-SNAPSHOT.jar

7. Run the application from the project root directory
docker compose up -d

8. Excute the curl command to see the random numbers being returned:
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'

9. Check the logs of the collector
docker logs  -f -n100 otel-collector
We can see all metrics, logs and traces collected by our collector

10. Check if traces are exported to Zipkin:
Zipkin
http://localhost:9411/

11. Check if traces are exported to Jaeger 
Jaeger
http://localhost:16686/
Search -> Service Name (random-generator) -> Find Traces 

12. Close the application
docker compose down

```

## Open Telemetry Collectors - Export metrics to Prometheus (and forward to Grafana)
Collecting Metrics (otel-metrics-exporter)
```xml
Create spring boot application otel-metrics-exporter and 
following steps 1 to 9 as otel-collector application 
(follow otel-collector application instruction)
Check if all monitoring parameters are pushed to otel-collector 
docker logs  -f -n100 otel-collector

Lets now push metics to Prometheus and visualize in Grafana 
1. In docker-compose.yaml file add the following services:

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.retention.time=1h
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    networks:
      - otel-network
      
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    depends_on:
      - prometheus
    networks:
      - otel-network

2. Under the otel-collector service in docker-compose.yaml add the following dependency:
    depends_on:
      - prometheus
This makes sure otel-collector is started only after prometheus containers start.

3. In the otel-collector-config.yaml add the following under the exporter
to indicate the collector to export data to prometheus (otlp supported export)
exporters:
  debug:
    verbosity: detailed       # Logs incoming metrics and traces , Add debug exporter in your Collector config to visually verify metrics
    # Then when you run the collector, it will log received metrics to the console.
  prometheus:
    endpoint: "0.0.0.0:8889"  # Prometheus-compatible endpoint , Prometheus will scrape this endpoint to collect metrics.
  

4. In the otel-collector-config.yaml add the following under the service -> pipeline 
-> traces -> exporters

service:
  pipelines:
    metrics:
      exporters: [prometheus, debug]

* Note do not make any changes to other configurations already existing in this section.

5. Create a prometheus.yaml file under the root directory and add the following
global:
  scrape_interval: 15s # How often to scrape targets (e.g. 15 seconds)

scrape_configs:
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']  # Change port if your app runs on a different one

6. Clean and Build the application 
Clean and Build the application and check if 
otel-metrics-exporter-0.0.1-SNAPSHOT.jar is created on the /target folder 

7. Create a Dockerfile to build the container
FROM eclipse-temurin:17-jre
ADD target/otel-metrics-exporter-0.0.1-SNAPSHOT.jar .
ENTRYPOINT java -jar otel-metrics-exporter-0.0.1-SNAPSHOT.jar

8. Run the application from the project root directory
docker compose up -d

9. Excute the curl command to see the random numbers being returned:
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'

10. Check the logs of the collector
docker logs  -f -n100 otel-collector
We can see all metrics, logs and traces collected by our collector

11. Check if metrics are exported to Prometheus:
Prometheus Metrics 
Open in your browser http://localhost:8889/metrics
Search in the page for
jvm_class_loaded_total
job="random-generator"

12. Check if metrics are exported to PrometheusUI:
http://localhost:9090
Open Prometheus UI: http://localhost:9090
Run query in http://localhost:9090/
up{job="otel-collector"}
jvm_thread_count
sum by (job) (jvm_thread_count)
jvm_class_loaded_total
jvm_class_loaded_total{exported_job="random-generator"}

13. Visualize metrics in Grafna
http://localhost:3000
When prompted:
Select your Prometheus data source
Add the Prometheus server connection URL 
http://localhost:9090
Next go to Drilldown -> Metrics 
View all jvm related metrics here

14. Close the application
docker compose down

```

## Open Telemetry Collectors - Export logs to Grafana Dashboard
Collecting Logs (otel-log-exporter)
```xml
Create spring boot application otel-log-exporter and 
following steps 1 to 9 as otel-collector application 
(follow otel-collector application instruction)
Check if all monitoring parameters are pushed to otel-collector 
docker logs  -f -n100 otel-collector

Lets now push logs to otel-collector and visualize in Grafana 
1. In docker-compose.yaml file add the following services:

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    entrypoint:
      - sh
      - -euc
      - |
        mkdir -p /etc/grafana/provisioning/datasources
        cat <<EOF > /etc/grafana/provisioning/datasources/ds.yaml
        apiVersion: 1
        datasources:
        - name: Loki
          type: loki
          access: proxy
          orgId: 1
          url: http://loki:3100
          basicAuth: false
          isDefault: true
          version: 1
          editable: false
        EOF
        /run.sh
    depends_on:
      - loki
    networks:
      - otel-network
      
  loki:
    image: grafana/loki:latest
    container_name: loki
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/config.yml
    volumes:
      - ./loki-config.yml:/etc/loki/config.yml:ro
      - loki-data:/loki
    networks:
      - otel-network

volumes:
  loki-data:

2. Under the otel-collector service in docker-compose.yaml add the following dependency:
    depends_on:
      - grafana
This makes sure otel-collector is started only after grafana containers start.

3. In the otel-collector-config.yaml add the following under the exporter
to indicate the collector to export data to loki (otlp supported export)
exporters:
  debug:
    verbosity: detailed       # Logs incoming metrics and traces , Add debug exporter in your Collector config to visually verify metrics
    # Then when you run the collector, it will log received metrics to the console.
  otlphttp/loki:
    endpoint: "http://loki:3100/otlp"
    tls:
      insecure: true
  

4. In the otel-collector-config.yaml add the following under the service -> pipeline 
-> traces -> exporters

service:
  pipelines:
    logs:
      exporters: [otlphttp/loki, debug]

* Note do not make any changes to other configurations already existing in this section.

5. Create a loki-config.yaml file under the root directory and add the following: 
auth_enabled: false

server:
  http_listen_port: 3100

limits_config:
  allow_structured_metadata: true

common:
  path_prefix: /loki
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

storage_config:
  filesystem:
    directory: /loki/chunks

schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: loki_index_
        period: 24h

6. Edit the RandomNumberGenerator.java to add few logs:
@RestController
@RequestMapping ("/random")
public class RandomNumberGenerator {
	
	private final Logger LOG = LoggerFactory.getLogger(RandomNumberGenerator.class);

	@GetMapping ("/{id}")
	public Integer findbyId(@PathVariable Integer id) {
		LOG.info("Input number is : {}", id);
		Integer randomNumber=new Random().nextInt(id);
		LOG.info("Generated random number is : {}", randomNumber);
        return randomNumber;
	}
	
}

7. Clean and Build the application 
Clean and Build the application and check if 
otel-log-exporter-0.0.1-SNAPSHOT.jar is created on the /target folder 

8. Create a Dockerfile to build the container
FROM eclipse-temurin:17-jre
ADD target/otel-log-exporter-0.0.1-SNAPSHOT.jar .
ENTRYPOINT java -jar otel-log-exporter-0.0.1-SNAPSHOT.jar

9. Run the application from the project root directory
docker compose up -d

10. Excute the curl command to see the random numbers being returned:
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'
curl --location 'http://localhost:8080/random/100'

11. Check the logs of the collector
docker logs  -f -n100 otel-collector
We can see all metrics, logs and traces collected by our collector

12. Visualize logs in Grafna
http://localhost:3000
Go to Drilldown -> Logs 
View all application related logs here

13. Close the application
docker compose down

```

## Open Telemetry Collectors - All in one application
Collecting Traces, Metrics & Logs (spring-otel-exporter)
```xml
This samples puts all the above 3 examples (Traces, Metrics and Logs)
into one single application. 

```

## Open Telemetry with Springboot 4 (spring-observability)
```xml
With Springboot 4 Observability becomes much more easier.
Look at the below article for more information.
https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot

Also look at the below repo for a sample
https://github.com/danvega/ot/blob/master/README.md

Spring boot 4 OpenTelemetry uses the Grafana LGTM stack 
- Loki (logs), Grafana (Visualization), Tempo (Traces), Mimir (Metrics) 

Lets create a spring boot application version 4.0.1 
and we will use this application to check Otel Observability capabilites. 

1. Go to spring initilizer and add the following packages:
Spring Web
OpenTelemetry
Docker Compose Support
(**important to select version 4.0.1)

Save the project as spring-observability and download. 
Open it in your IDE. 

2. In the pom.xml you will see the following base dependencies:
<!-- For Tracing and Metrics-->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-docker-compose</artifactId>
	<scope>runtime</scope>
	<optional>true</optional>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-opentelemetry-test</artifactId>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-webmvc-test</artifactId>
	<scope>test</scope>
</dependency>

Add the below 2 extra dependencies:
<!-- For Logging-->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>2.21.0-alpha</version>
</dependency>
<!-- For Injecting Custom Metrics-->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-aop</artifactId>
	<version>3.3.5</version>
</dependency>

3. Create a compose.yaml file add the following services:
services:
  grafana-lgtm:
    image: 'grafana/otel-lgtm:latest'
    ports:
      - '3000:3000'
      - '4317:4317'
      - '4318:4318'

4. Add the following properties in the application.properties 
spring.otlp.metrics.export.url=http://localhost:4318/v1/metrics
spring.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
spring.opentelemetry.logging.export.otlp.endpoint=http://localhost:4318/v1/logs
management.tracing.sampling.probability=1.0
server.shutdown=immediate
logging.level.com.java.bala=INFO
logging.level.org.springframework.boot.actuator.autoconfigure.opentelemetry=DEBUG
logging.level.org.springframework.boot.docker.compose=DEBUG

5. For logging create a logback-spring.xml file under the resources folder
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>

    <appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="OTEL"/>
    </root>
</configuration>

6. Create a Contoller called HomeController.java under the controller package
@RestController
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/")
    public String home() {
        log.info("Home endpoint called");
        return "Hello World!";
    }

    @GetMapping("/greet/{name}")
    public String greet(@PathVariable String name) {
        log.info("Greeting user: {}", name);
        simulateWork();
        return "Hello, " + name + "!";
    }

    @GetMapping("/slow")
    @Observed(name = "my.slow.operation")
    public String slow() throws InterruptedException {
        log.info("Starting slow operation");
        Thread.sleep(500);
        log.info("Slow operation completed");
        return "Done!";
    }

    private void simulateWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

Note: **
@Observed(name = "my.slow.operation")
** 
This is for injecting custom metrics 

7. For injecting logging create a component called 
InstallOpenTelemetryAppender.java inside the component package
@Component
class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}


8. Clean, Build and Run the application using maven:
./mvnw spring-boot:run
or from eclipse maven set goal as 
spring-boot:run and run it.


9. Excute the curl command:
curl --location 'http://localhost:8080/'
curl --location 'http://localhost:8080/'
curl --location 'http://localhost:8080/greet/World'
curl --location 'http://localhost:8080/greet/World'
curl --location 'http://localhost:8080/slow'
curl --location 'http://localhost:8080/slow'

10. Visualize metrics, logs and traces in Grafna
http://localhost:3000
Go to Drilldown -> 
Metrics
Logs 
Traces
View all application telemetry here

11. Finally close the application

With just a few configuration steps Spring Boot 4.0.1 has 
enabled the LGTM stack for all our telemetry needs. 

```

## Reference
```xml
https://www.youtube.com/playlist?list=PLLMxXO6kMiNg6EcNCx6C6pydmgUlDDcZY
https://opentelemetry.io/docs/
https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot
https://www.youtube.com/watch?v=6_Y41z7OIv8
```



