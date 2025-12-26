Zipkin
http://localhost:9411/

Jaeger
http://localhost:16686/

Prometheus Metrics 
Open in your browser http://localhost:8889/metrics
Search in the page for
jvm_class_loaded_total
job="spring-otel-exporter"


Prometheus UI
http://localhost:9090
Open Prometheus UI: http://localhost:9090
Run query in http://localhost:9090/
up{job="otel-collector"}
jvm_thread_count
sum by (job) (jvm_thread_count)
jvm_class_loaded_total
jvm_class_loaded_total{exported_job="spring-otel-exporter"}


Grafna
http://localhost:3000
When prompted:
Select your Prometheus data source
http://localhost:9090
Built the following readymade dashboards
A) JVM / Micrometer dashboard
Dashboards → Import
Import ID:
4701
B) Spring Boot dashboard
Import ID:
6756
C) OpenTelemetry Collector dashboard
Import ID:
15983


Loki
http://localhost:3100/ready