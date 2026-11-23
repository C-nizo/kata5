# Kata 5 — Balanceo de carga dinámico con NGINX y Consul

Práctica de Ingeniería del Software 2 (ULPGC). El proyecto implementa un sistema de balanceo de carga dinámico utilizando NGINX como proxy inverso y Consul para el registro, la comprobación de salud y el descubrimiento de réplicas.

Se reutiliza el servicio web de divisores desarrollado en la Kata 4, incorporando dos grupos de réplicas independientes. El sistema permite añadir y retirar instancias en tiempo de ejecución sin modificar la configuración ni reiniciar NGINX.

Repositorio: [C-nizo/kata5](https://github.com/C-nizo/kata5).

## Requisitos y dependencias

- Java 27.
- Maven e IntelliJ IDEA (o un IDE compatible con Maven).
- Docker Desktop con Docker Compose.
- Javalin 7.2.4, para implementar el servicio HTTP.
- Gson 2.15.0, para serializar las respuestas JSON.
- Consul 2.1.0, mediante la imagen `hashicorp/consul:2.1.0`.
- NGINX, mediante la imagen `nginx:stable`.

Durante las comprobaciones se utilizó NGINX 1.30.7, que admite el descubrimiento dinámico mediante DNS SRV en su versión de código abierto.

Las dependencias Java están declaradas en los archivos `pom.xml` de los módulos correspondientes.

## Arquitectura y estructura

El proyecto contiene dos módulos Maven independientes y un directorio de configuración del balanceador:

| Componente | Responsabilidad |
| --- | --- |
| `divisors-service` | Implementa el servicio HTTP que calcula los divisores de un número. |
| `divisors-client` | Realiza peticiones a los dos upstreams y muestra qué réplica responde. |
| `load-balancer/compose.yaml` | Define los contenedores de NGINX y Consul. |
| `load-balancer/webservice.conf` | Configura las rutas públicas y los upstreams dinámicos de NGINX. |

El servicio mantiene una separación de responsabilidades basada en arquitectura hexagonal:

| Clase | Responsabilidad |
| --- | --- |
| `DivisorsCalculator` | Calcula los divisores positivos de un número. |
| `DivisorsCalculationOperation` | Coordina el cálculo y la serialización. |
| `DivisorsSerializer` | Define el puerto de serialización. |
| `JsonDivisorsSerializer` | Implementa la serialización mediante Gson. |
| `DivisorsEndPoint` | Atiende las peticiones HTTP y prepara las respuestas. |
| `ConsulServiceRegistrar` | Registra cada réplica y su comprobación de salud en Consul. |
| `Main` del servidor | Configura Javalin, arranca la instancia y solicita su registro en Consul. |
| `Main` del cliente | Consulta ambos upstreams y muestra los resultados recibidos. |

La lógica del dominio y el caso de uso no dependen de Javalin, Consul ni NGINX.

## Topología y configuración

NGINX y Consul se ejecutan en contenedores Docker. Las instancias del servicio Java se ejecutan directamente en el equipo anfitrión.

Los puertos principales de la versión final son:

| Componente | Puerto | Función |
| --- | --- | --- |
| NGINX | 8090 | Punto de entrada público del sistema. |
| Consul | 8500 | API HTTP e interfaz de administración. |
| Consul DNS | 8600 | Descubrimiento de servicios dentro de la red Docker. |
| Réplicas Java | 8081–8084 | Instancias iniciales del servicio de divisores. |

NGINX utiliza dos bloques `upstream`, cada uno asociado a un nombre de servicio diferente en Consul:

| Ruta pública | Upstream de NGINX | Servicio en Consul |
| --- | --- | --- |
| `/v1/divisors/{n}` | `divisors_v1` | `divisors-v1` |
| `/v2/divisors/{n}` | `divisors_v2` | `divisors-v2` |

Ambos upstreams utilizan el mismo código Java, pero sus réplicas se registran como servicios independientes.

La configuración utiliza `zone`, `resolver`, `service` y `resolve` para descubrir dinámicamente las instancias disponibles mediante registros DNS SRV de Consul.

Cada instancia anuncia la dirección `host.docker.internal`, su puerto y una comprobación HTTP en `/health`. Consul realiza estas comprobaciones cada 5 segundos.

## Compilación y ejecución

### 1. Obtener y compilar el proyecto

Clona el repositorio:

```bash
git clone https://github.com/C-nizo/kata5.git
cd kata5
```

Abre el proyecto en IntelliJ IDEA, selecciona el JDK 27 y carga los dos módulos Maven.

También puedes compilarlos desde una terminal que tenga Maven instalado:

```bash
mvn -f divisors-service/pom.xml clean package
mvn -f divisors-client/pom.xml clean package
```

### 2. Arrancar NGINX y Consul

Con Docker Desktop en funcionamiento, ejecuta desde la raíz del proyecto:

```bash
docker compose -f load-balancer/compose.yaml up -d
```

Consul estará disponible en:

http://localhost:8500

Para consultar la versión de NGINX:

```bash
docker compose -f load-balancer/compose.yaml exec nginx nginx -v
```

### 3. Arrancar las réplicas del servicio

Ejecuta desde IntelliJ la clase `software.ulpgc.katas.infrastructure.Main` del módulo `divisors-service`.

Cada instancia recibe cuatro argumentos:

```text
<puerto> <identificador> <nombre-servicio> <dirección-consul>
```

Para reproducir la tercera repetición, utiliza estas configuraciones:

| Puerto | Identificador | Nombre de servicio | Dirección de Consul |
| --- | --- | --- | --- |
| 8081 | `v1-x` | `divisors-v1` | `http://localhost:8500` |
| 8082 | `v1-y` | `divisors-v1` | `http://localhost:8500` |
| 8083 | `v2-x` | `divisors-v2` | `http://localhost:8500` |
| 8084 | `v2-y` | `divisors-v2` | `http://localhost:8500` |

Cada instancia arranca Javalin y se registra en Consul con su identificador, dirección, puerto y comprobación de salud.

En la interfaz de Consul deben aparecer los servicios `divisors-v1` y `divisors-v2`, cada uno con dos réplicas en estado `passing`.

### 4. Ejecutar el cliente

Ejecuta desde IntelliJ la clase `software.ulpgc.katas.Main` del módulo `divisors-client`.

El cliente realiza peticiones continuas a ambos upstreams utilizando un número aleatorio entre 1 y 100 en cada iteración.

La salida tiene este formato:

```text
[v1 | réplica=v1-x] divisores(10) = [1,2,5,10] | HTTP 200
[v2 | réplica=v2-y] divisores(10) = [1,2,5,10] | HTTP 200
```

El identificador de la réplica se obtiene de la cabecera HTTP `X-Node`, añadida por cada instancia del servicio.

El cliente continúa realizando peticiones hasta detenerlo manualmente.

## Contrato HTTP

Las dos rutas públicas aceptan un número entero positivo y devuelven sus divisores en formato JSON.

Ejemplos desde PowerShell:

```powershell
curl.exe -i "http://localhost:8090/v1/divisors/10"
curl.exe -i "http://localhost:8090/v2/divisors/10"
```

En ambos casos, la respuesta correcta es **HTTP 200**, con tipo `application/json` y el cuerpo:

```json
[1,2,5,10]
```

La respuesta incluye la cabecera `X-Node`, cuyo valor permite identificar la réplica que atendió la petición.

Para entradas inválidas, el servicio devuelve HTTP 400:

```powershell
curl.exe -i "http://localhost:8090/v1/divisors/abc"
curl.exe -i "http://localhost:8090/v1/divisors/0"
```

## Verificación del balanceo dinámico

Las comprobaciones se realizan manteniendo NGINX en funcionamiento y utilizando las mismas rutas públicas durante cada prueba.

### Reparto de peticiones

Con las cuatro réplicas iniciales arrancadas:

1. Ejecuta el cliente.
2. Comprueba que las peticiones de `v1` son atendidas por `v1-x` y `v1-y`.
3. Comprueba que las peticiones de `v2` son atendidas por `v2-x` y `v2-y`.
4. Verifica que los resultados son correctos independientemente de la réplica que responde.

### Baja de una réplica

1. Mantén el cliente funcionando.
2. Detén la instancia `v1-y`.
3. Espera a que Consul detecte el cambio de estado y NGINX actualice los destinos disponibles.
4. Comprueba que `v1-y` deja de responder y que `v1-x` sigue atendiendo las peticiones de `v1`.
5. Comprueba que `v2-x` y `v2-y` siguen funcionando normalmente.

No es necesario modificar la configuración ni reiniciar NGINX.

### Incorporación de una réplica

Con `v1-y` detenida, arranca otra instancia con estos argumentos:

```text
8085 v1-z divisors-v1 http://localhost:8500
```

Tras su registro y descubrimiento, el cliente debe mostrar respuestas tanto de `v1-x` como de `v1-z`.

El upstream `v2` debe seguir funcionando independientemente.

Estas comprobaciones se realizaron satisfactoriamente en las tres repeticiones.

## Repeticiones

Se repitió tres veces el despliegue del mismo sistema de balanceo dinámico, conservando los dos upstreams y la lógica del servicio de divisores.

1. **Primera repetición:** configuración inicial con NGINX en el puerto `8080`, rutas `/api/v1/divisors/{n}` y `/api/v2/divisors/{n}`, y réplicas identificadas como `v1-a`, `v1-b`, `v2-a` y `v2-b`.

2. **Segunda repetición:** se cambió el puerto público de NGINX a `8090` y se adaptó el cliente. Se conservaron las rutas y los nombres de servicio de la primera repetición.

3. **Tercera repetición (versión final):** se modificaron las rutas públicas a `/v1/divisors/{n}` y `/v2/divisors/{n}`, manteniendo los upstreams. Se utilizaron los identificadores `v1-x`, `v1-y`, `v2-x` y `v2-y`.

En cada repetición se comprobó el reparto de peticiones, la retirada de una réplica y la incorporación de una nueva, sin reiniciar ni recargar NGINX.

Las repeticiones están identificadas mediante commits en el historial Git:

```bash
git log --oneline
```

Pueden consultarse sus configuraciones mediante `git show`:

```bash
git show ef4fd4d:load-balancer/webservice.conf
git show b0110c5:load-balancer/compose.yaml
git show 5f833b8:load-balancer/webservice.conf
```

La rama `master` contiene la tercera repetición, que constituye la versión final evaluable.

## Detener el sistema

Detén el cliente y las instancias Java desde IntelliJ.

Después, ejecuta:

```bash
docker compose -f load-balancer/compose.yaml down
```

Esto detiene y elimina los contenedores del despliegue y su red, conservando las imágenes descargadas y los archivos del proyecto.

## Vídeo explicativo

**Pendiente de grabación.** Se añadirá aquí el enlace al vídeo explicativo (duración máxima de 7 minutos) mediante un commit posterior.