# InfyInvenTrack: Product Catalog & Inventory Management Microservices

Spring Boot 3 microservices capstone using Java 17, Maven, Spring Cloud Gateway, Consul discovery/config, MySQL, Spring Security Basic Auth, JPA, Swagger, Actuator, Resilience4j, JUnit 5, and Mockito.

## Modules

| Module | Port | Responsibility |
|---|---:|---|
| api-gateway | 8080 | Public entry point for `/infyinventrack/**` |
| catalog-service | 8081 | Products, categories, search/filter, cache bust |
| inventory-service | 8082 | Stock view, reservations, low stock, inventory value |
| user-service | 8083 | Registration and users |
| common-lib | - | DTOs, constants, errors, enums |

## MySQL

Use only MySQL. The configured credentials are `root` / `password` and schema `microservices`.

```sql
CREATE DATABASE IF NOT EXISTS microservices;
```

Tables are service-owned even though they live in the same schema: catalog owns `categories` and `products`, inventory owns `inventory_items`, and user owns `users`.

## Run

Start Consul first:

```powershell
consul agent -dev
```

Then run services:

```powershell
mvn -pl catalog-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

## Test

```powershell
mvn clean test
```

## Demo Users

| Username | Password | Role |
|---|---|---|
| admin | Admin@123 | ROLE_ADMIN |
| customer | Customer@123 | ROLE_CUSTOMER |

## Swagger

- Catalog: http://localhost:8081/swagger-ui.html
- Inventory: http://localhost:8082/swagger-ui.html
- User: http://localhost:8083/swagger-ui.html

## Gateway Examples

```powershell
curl -u admin:Admin@123 http://localhost:8080/infyinventrack/products/SKU12300
curl -u admin:Admin@123 "http://localhost:8080/infyinventrack/products/search?category=Electronics&page=0&size=5&sortBy=category"
curl -u customer:Customer@123 -H "Content-Type: application/json" -d '{"quantity":3}' http://localhost:8080/infyinventrack/inventory/SKU12300/reserve
curl -u admin:Admin@123 http://localhost:8080/infyinventrack/inventory/value
```
