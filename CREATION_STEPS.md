# Creation Steps: InfyInvenTrack Microservices Capstone

This document explains how to recreate the InfyInvenTrack microservices capstone from an empty folder. It captures the order of creation, why that order matters, the do's and don'ts followed, the concepts used in each file group, common traps, best use cases, and frequently asked questions.

Project target:

- Java 17+
- Spring Boot 3.x
- Maven multi-module build
- MySQL database: `microservices`
- MySQL username: `root`
- MySQL password: `password`
- Spring Cloud Gateway
- Spring Cloud Consul discovery/config
- Spring Security Basic Auth
- Spring Data JPA
- Resilience4j circuit breaker
- Actuator and Swagger/OpenAPI
- JUnit 5 and Mockito

---

## 1. Start With The Parent Maven Project

Create the root `pom.xml` first.

Files:

- `pom.xml`

What it does:

- Defines the project as a Maven parent using `<packaging>pom</packaging>`.
- Lists all modules: `common-lib`, `api-gateway`, `catalog-service`, `inventory-service`, `user-service`.
- Sets Java version to 17.
- Imports Spring Cloud dependency management.
- Keeps module versions aligned.

Why this comes first:

The parent build is the foundation. Every child module inherits Spring Boot, dependency versions, Java version, and build behavior from it. If this is created later, each service might drift into different versions, causing hard-to-debug dependency conflicts.

Do's:

- Use one parent Maven file for all services.
- Keep Spring Boot and Spring Cloud versions compatible.
- Declare modules in the order they should build.
- Keep shared version properties at the parent level.

Don'ts:

- Do not create separate unrelated Maven projects for each service.
- Do not hardcode different Spring versions in every module.
- Do not put service-specific dependencies in the parent unless every module needs them.

Common traps:

- Spring Boot 3 requires Java 17 or newer.
- Spring Cloud versions must match the Spring Boot generation.
- If `common-lib` is not listed before services, service modules may fail to resolve shared classes.

Best use case:

Use a parent Maven project whenever multiple Java services are developed and tested together in one repository.

---

## 2. Create `common-lib` Before The Services

Files:

- `common-lib/pom.xml`
- `common-lib/src/main/java/com/infy/inventrack/common/enums/UserRole.java`
- `common-lib/src/main/java/com/infy/inventrack/common/constants/InfyInvenTrackConstants.java`
- `common-lib/src/main/java/com/infy/inventrack/common/dto/*.java`
- `common-lib/src/main/java/com/infy/inventrack/common/exception/*.java`

What it does:

- Stores shared DTOs used across service boundaries.
- Stores the `UserRole` enum.
- Stores repeated constants and messages.
- Stores common exception classes.

Why this comes second:

Services need shared request/response classes before controllers and clients can compile. Creating this module early prevents duplication and keeps API models consistent.

Important design decision:

Only DTOs, constants, enums, and simple exception types are shared. Entities and repositories are not shared. This keeps microservice ownership clean.

Do's:

- Share API contracts such as request and response DTOs.
- Share simple enums used in API payloads.
- Share standard exception types if they carry no database or service-specific behavior.
- Use Bean Validation annotations in DTOs.

Don'ts:

- Do not share JPA entities.
- Do not share repositories.
- Do not share service classes.
- Do not put business logic in `common-lib`.

Common traps:

- Sharing entities creates tight coupling between services.
- Sharing repository code breaks the microservices database ownership rule.
- Putting too much into `common-lib` turns it into a hidden monolith.

Best use case:

Use `common-lib` for stable contracts and tiny shared building blocks only.

Concepts used:

- DTO pattern
- Bean Validation
- Enum-based role modeling
- Shared constants
- Custom runtime exceptions
- Maven module dependency

---

## 3. Create The API Gateway Module

Files:

- `api-gateway/pom.xml`
- `api-gateway/src/main/java/com/infy/inventrack/gateway/ApiGatewayApplication.java`
- `api-gateway/src/main/java/com/infy/inventrack/gateway/config/SecurityConfig.java`
- `api-gateway/src/main/resources/application.yml`

What it does:

- Runs the public entry point on port `8080`.
- Routes `/infyinventrack/products/**` to `catalog-service`.
- Routes `/infyinventrack/categories/**` to `catalog-service`.
- Routes `/infyinventrack/inventory/**` to `inventory-service`.
- Routes `/infyinventrack/users/**` to `user-service`.
- Uses load-balanced service names such as `lb://catalog-service`.
- Applies Basic Auth at the gateway.

Why this comes before business services:

The gateway defines the public API shape. Once routes are known, each downstream service can keep its local paths clean, such as `/products`, `/categories`, `/inventory`, and `/users`.

Do's:

- Use the gateway as the public entry point.
- Keep external paths stable.
- Use route rewriting to remove `/infyinventrack` before forwarding.
- Keep gateway routes simple.
- Secure the gateway.

Don'ts:

- Do not put business logic in the gateway.
- Do not make the gateway access databases.
- Do not let every service expose unrelated public URL structures.

Common traps:

- A wrong `RewritePath` causes downstream services to receive paths they do not map.
- Missing Consul registration means `lb://service-name` cannot resolve.
- Gateway security and service security can conflict if credentials are not aligned.

Best use case:

Use a gateway when client apps need one stable entry point while backend services remain independently deployable.

Concepts used:

- Spring Cloud Gateway
- Reverse proxy routing
- Route predicates
- Rewrite filters
- Load-balanced service discovery
- Reactive Spring Security

---

## 4. Add Common Service Infrastructure

Files created in each service:

- `*ServiceApplication.java`
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`

Applies to:

- `catalog-service`
- `inventory-service`
- `user-service`

What it does:

- Creates one Spring Boot entry class per service.
- Enables caching where needed.
- Configures Basic Auth demo users.
- Enables method-level role checks using `@PreAuthorize`.
- Configures MySQL connection.
- Configures Consul discovery/config.
- Exposes Actuator and Swagger endpoints.
- Provides consistent error responses.

Why this comes before business code:

Every controller and service depends on application configuration, security rules, exception handling, and database access. Creating infrastructure first gives business code a stable runtime environment.

Do's:

- Keep each service independently runnable.
- Give each service its own port.
- Use the same MySQL schema only because this capstone requires it.
- Still keep table ownership separate by service.
- Return standard error JSON from all services.

Don'ts:

- Do not rely on role query parameters for security.
- Do not expose stack traces to clients.
- Do not disable security for business endpoints.
- Do not use one service's repository inside another service.

Common traps:

- `ddl-auto=update` is convenient for capstone demos, but should be replaced with migrations in production.
- Basic Auth is simple and acceptable for a capstone, but JWT or OAuth2 is preferred for larger systems.
- If MySQL is not running, the services will fail at startup.
- If Consul is not running, discovery will not work correctly in full gateway flow.

Best use case:

This setup is good for learning microservices structure, routing, service discovery, validation, and service ownership.

Concepts used:

- Spring Boot application bootstrapping
- Spring Security
- Method security
- Global exception handling
- MySQL datasource configuration
- JPA configuration
- Actuator
- Swagger/OpenAPI
- Consul discovery/config

---

## 5. Build The Catalog Service

Main files:

- `catalog-service/src/main/java/com/infy/inventrack/catalog/entity/Category.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/entity/Product.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/repository/CategoryRepository.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/repository/ProductRepository.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/mapper/CatalogMapper.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/service/CategoryService.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/service/ProductService.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/controller/CategoryController.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/controller/ProductController.java`
- `catalog-service/src/main/java/com/infy/inventrack/catalog/config/DataSeeder.java`

Responsibilities:

- Product creation
- Product retrieval by SKU
- Product update
- Product deletion
- Product search/filter
- Top N expensive products
- Product description update and cache eviction
- Category create/update/delete
- Prevent deleting categories that are in use

Why catalog is created before inventory:

Inventory must validate that a SKU exists in the catalog before reserving stock. Catalog is the upstream source of product identity, so it must exist before inventory's downstream client is useful.

Layer order:

1. Entity
2. Repository
3. Mapper
4. Service
5. Controller
6. Seeder
7. Tests

Why this layer order matters:

- Entities define table structure.
- Repositories depend on entities.
- Mappers depend on entities and DTOs.
- Services depend on repositories and mappers.
- Controllers depend on services.
- Seeders depend on repositories.
- Tests validate service behavior after dependencies exist.

Do's:

- Use DTOs in controllers.
- Use entities only inside the service/repository layer.
- Validate SKU uniqueness before product creation.
- Validate category existence before assigning a product.
- Use streams for search/filter and top N use cases in this capstone.
- Evict cache when a product is updated or deleted.

Don'ts:

- Do not expose `Product` or `Category` entities directly from controllers.
- Do not delete a category if products still reference it.
- Do not duplicate product SKUs.
- Do not trust request body category names without checking the category table.

Common traps:

- Lazy loading can fail if mapping happens outside transaction scope.
- Cache can return stale data if update/delete methods do not evict entries.
- Search parameters need validation, especially blank strings and negative prices.
- Pagination using streams works for capstone data, but database-level pagination is better for large datasets.

Best use cases:

- Product catalog browsing
- Admin product management
- Category management
- Search and filter workflows
- Cache invalidation demonstration

Concepts used:

- JPA entity mapping
- One-to-many concept through `Product` to `Category`
- Spring Data JPA repositories
- DTO mapping
- Service layer transactions
- Bean Validation
- Method security
- Java Streams
- Caching with `@Cacheable` and `@CacheEvict`
- Seeder with `CommandLineRunner`

---

## 6. Build The Inventory Service

Main files:

- `inventory-service/src/main/java/com/infy/inventrack/inventory/entity/InventoryItem.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/repository/InventoryRepository.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/config/RestClientConfig.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/client/CatalogClient.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/service/InventoryService.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/controller/InventoryController.java`
- `inventory-service/src/main/java/com/infy/inventrack/inventory/config/DataSeeder.java`

Responsibilities:

- View stock by SKU
- Reserve stock
- Calculate total inventory value
- Find low-stock products
- Validate product existence through catalog service
- Return fallback when catalog is unavailable

Why inventory comes after catalog:

Inventory depends on catalog for SKU validation. This is the first real inter-service communication in the project, so catalog must be available as the source of product truth.

Layer order:

1. Inventory entity
2. Inventory repository
3. RestTemplate configuration
4. Catalog client
5. Inventory service
6. Inventory controller
7. Seed data
8. Tests

Why this layer order matters:

The service cannot reserve stock until it has storage, and it cannot validate product identity until the catalog client exists. The controller should be last because it only exposes already-built behavior.

Do's:

- Keep inventory stock in `inventory_items`, not in catalog repositories.
- Validate SKU exists in catalog before reservation.
- Use a circuit breaker around catalog calls.
- Validate quantity is positive.
- Return a clear message for insufficient stock.
- Keep stock update transactional.

Don'ts:

- Do not directly read catalog tables from inventory service.
- Do not reserve stock without checking current stock.
- Do not make downstream calls without fallback behavior.
- Do not mix product CRUD with inventory responsibility.

Common traps:

- Inter-service calls can fail even when code is correct, because Consul or the downstream service may be down.
- Missing `@LoadBalanced` on `RestTemplate` prevents `http://catalog-service/...` from resolving.
- Fallback methods must have a compatible signature.
- Synchronized service methods are not enough for distributed concurrency in production. Use database locking or events for serious systems.

Best use cases:

- Stock availability check
- Reservation workflow before purchase
- Low-stock reporting
- Inventory valuation
- Circuit breaker demonstration

Concepts used:

- Service-owned table
- Transactional stock mutation
- Load-balanced REST client
- Resilience4j circuit breaker
- Fallback response
- Inter-service communication
- Actuator metrics for resilience behavior

---

## 7. Build The User Service

Main files:

- `user-service/src/main/java/com/infy/inventrack/user/entity/AppUser.java`
- `user-service/src/main/java/com/infy/inventrack/user/repository/UserRepository.java`
- `user-service/src/main/java/com/infy/inventrack/user/service/UserService.java`
- `user-service/src/main/java/com/infy/inventrack/user/controller/UserController.java`
- `user-service/src/main/java/com/infy/inventrack/user/config/DataSeeder.java`

Responsibilities:

- Register users
- Store username, email, encoded password, and role
- Seed admin and customer users
- Return user responses without exposing passwords

Why user service is created after catalog and inventory:

The core product and stock workflows come first because they define the business domain. User service then adds identity and role-based access around that domain.

Layer order:

1. User entity
2. User repository
3. User service
4. User controller
5. User seed data
6. Tests

Why this layer order matters:

Registration depends on persistence and password encoding. The controller should only call the service after uniqueness checks and encoding behavior are implemented.

Do's:

- Encode passwords with BCrypt.
- Validate username and email uniqueness.
- Never return password fields in API responses.
- Validate password strength.
- Store roles as enums.

Don'ts:

- Do not store raw passwords.
- Do not expose internal entity fields.
- Do not allow duplicate usernames or emails.
- Do not accept invalid role values.

Common traps:

- Password regex must be escaped correctly in Java strings.
- A DTO may validate input, but service-level uniqueness checks are still required.
- Demo Basic Auth users in `SecurityConfig` and database users in `users` are separate in this simplified capstone.

Best use cases:

- Registration flow
- Role data ownership
- Password validation demonstration
- Secure response modeling

Concepts used:

- BCrypt password encoding
- User registration
- Unique constraints
- Enum persistence
- DTO response without sensitive fields

---

## 8. Add Seed Data

Files:

- `catalog-service/.../config/DataSeeder.java`
- `inventory-service/.../config/DataSeeder.java`
- `user-service/.../config/DataSeeder.java`

What seed data creates:

- Category: `Electronics`
- Product: `SKU12300`, Laptop, HP
- Inventory item: `SKU12300`, quantity 50
- Users: `admin`, `customer`

Why seed data comes after services:

Seeders depend on repositories and entities. They should be added once the persistence model is stable.

Do's:

- Check if data already exists before inserting.
- Keep seed data small and predictable.
- Use seed data to make demo APIs immediately testable.

Don'ts:

- Do not insert duplicate rows on every startup.
- Do not hide important setup only inside seeders. Document it too.
- Do not seed production systems with demo passwords.

Common traps:

- Catalog and inventory seeders must agree on SKU values.
- If one service starts and another does not, cross-service flows may still fail.

Best use case:

Use seed data for capstones, demos, and local development.

---

## 9. Add Tests After Business Logic

Files:

- `catalog-service/src/test/java/com/infy/inventrack/catalog/ProductServiceTest.java`
- `inventory-service/src/test/java/com/infy/inventrack/inventory/InventoryServiceTest.java`
- `user-service/src/test/java/com/infy/inventrack/user/UserServiceTest.java`

What tests cover:

- Duplicate SKU rejection
- Product creation success
- Inventory value calculation
- Stock reservation decrement
- Duplicate username rejection
- User registration response

Why tests come after implementation:

Tests need concrete service classes, repositories, and DTOs. Once the behavior exists, tests lock in the expected rules.

Do's:

- Use Mockito for service unit tests.
- Test success and failure cases.
- Keep unit tests independent from MySQL.
- Run `mvn test` from the parent project.

Don'ts:

- Do not require MySQL for every unit test.
- Do not test only controllers and ignore service rules.
- Do not skip failure cases such as duplicates and invalid input.

Common traps:

- Method security annotations may need Spring context tests if you want to validate authorization behavior directly.
- Mockito tests verify business logic, but not actual database mappings.
- Integration tests with MySQL need a running database and should be documented separately.

Best use case:

Use unit tests for fast feedback and integration tests later for full API/database behavior.

Concepts used:

- JUnit 5
- Mockito
- AssertJ
- Service unit testing
- Mock repositories and clients

---

## 10. Add README And Operational Instructions

Files:

- `README.md`

What it documents:

- Modules and ports
- MySQL setup
- Consul startup
- Service run order
- Test command
- Demo credentials
- Swagger URLs
- Gateway curl examples

Why README comes near the end:

The README should describe the final project shape. Writing it too early often leads to inaccurate commands and stale URLs.

Do's:

- Include exact run commands.
- Include database credentials required by the assignment.
- Include sample API calls.
- Include demo usernames and passwords.

Don'ts:

- Do not assume the reader knows startup order.
- Do not omit Consul or MySQL requirements.
- Do not document endpoints that do not exist.

Common traps:

- Forgetting quotes around URLs with `&` in PowerShell curl commands.
- Running gateway before services are registered.
- Starting services before MySQL exists.

---

## 11. Verify The Whole Project

Command:

```powershell
mvn test
```

Expected result:

- Reactor build succeeds.
- `common-lib` compiles first.
- `api-gateway` compiles.
- `catalog-service` tests pass.
- `inventory-service` tests pass.
- `user-service` tests pass.

Why verification is last:

Only the full Maven reactor proves all modules compile together and dependency boundaries are correct.

Do's:

- Run the build from the root folder.
- Use JDK 17 or newer.
- Fix compile errors before committing.
- Keep generated `target/` folders out of Git.

Don'ts:

- Do not commit without running tests.
- Do not commit `target/` build outputs.
- Do not assume one module passing means the whole reactor passes.

Common traps:

- Java 8 cannot compile Spring Boot 3 projects.
- UTF-8 BOM in Java files can cause `illegal character: '\ufeff'` errors.
- Incorrect Java regex escaping causes compile errors.
- Missing dependencies appear only when the affected module compiles.

---

## 12. Git Commit And Push

Files:

- `.gitignore`
- All source and config files

Recommended commands:

```powershell
git init
git branch -M main
git remote add origin https://github.com/imnav00/microservice_Capstone
git add .
git commit -m "Add InfyInvenTrack microservices capstone"
git push -u origin main
```

Why Git comes after verification:

The first commit should be a working baseline. This makes the repository useful for future changes, debugging, and submission.

Do's:

- Add `.gitignore` before staging.
- Commit source, config, tests, and docs.
- Push from the capstone project folder.

Don'ts:

- Do not commit `target/` folders.
- Do not commit IDE-specific files unless the team wants them.
- Do not commit secrets for real systems. This capstone uses assignment-provided local credentials only.

---

## Overall Creation Sequence

Follow this order:

1. Root Maven parent
2. `common-lib`
3. API gateway
4. Shared service infrastructure
5. Catalog service
6. Inventory service
7. User service
8. Seed data
9. Tests
10. README
11. Full Maven verification
12. Git commit and push

Why this exact sequence matters:

- The parent controls the build.
- Shared contracts must exist before services use them.
- The gateway defines the public API shape.
- Infrastructure keeps service behavior consistent.
- Catalog must exist before inventory validates SKUs.
- Inventory demonstrates cross-service communication after catalog exists.
- User service adds identity and roles after domain workflows are clear.
- Tests are meaningful only once behavior exists.
- Documentation should describe the finished system.
- Git should capture a verified working baseline.

---

## General Do's Used In This Capstone

- Use Java 17+ for Spring Boot 3.
- Use Maven multi-module structure for related services.
- Keep service responsibilities separate.
- Keep entities private to each service.
- Use DTOs for all request and response payloads.
- Use Bean Validation for input rules.
- Use service-layer transactions for writes.
- Use repositories only inside the owning service.
- Use Basic Auth and role checks for protected operations.
- Use BCrypt for passwords.
- Use global exception handling.
- Use standard error responses.
- Use Swagger for API documentation.
- Use Actuator for health and metrics.
- Use Consul for discovery/config.
- Use Resilience4j around inter-service calls.
- Use seed data for easy demo startup.
- Use unit tests for business rules.
- Use `.gitignore` to avoid committing build output.

## General Don'ts Used In This Capstone

- Do not edit unrelated existing projects.
- Do not expose entities directly through APIs.
- Do not share repositories across services.
- Do not share database access code between services.
- Do not put business logic in controllers.
- Do not put business logic in the gateway.
- Do not store raw passwords.
- Do not use role query parameters as security.
- Do not call another service without fallback handling.
- Do not delete categories that are in use.
- Do not allow duplicate SKUs, usernames, emails, or categories.
- Do not commit `target/` folders.
- Do not assume MySQL or Consul is running without documenting it.

---

## Common Traps And Pitfalls

1. Java version mismatch

Spring Boot 3 requires Java 17+. Java 8 will fail.

2. BOM character in Java files

Some Windows write methods add UTF-8 BOM. Java may fail with `illegal character: '\ufeff'`.

3. Regex escaping

Java strings need double escaping. A password regex using `\d` in regex must be written as `\\d` in a Java string.

4. Wrong gateway rewrite rule

If `/infyinventrack` is not stripped, downstream controllers may not match.

5. Missing Consul

`lb://catalog-service` works only when services are registered in discovery.

6. Missing MySQL database

The services point to MySQL only. Create the `microservices` schema or allow `createDatabaseIfNotExist=true` to create it.

7. Confusing demo Basic Auth with database users

The capstone uses in-memory Basic Auth for service security and a user table for registration data. A production system would connect authentication to user-service or a token provider.

8. Shared schema does not mean shared ownership

The assignment uses one schema, but each service should only access its own tables.

9. Cache staleness

Whenever product data changes, cached SKU reads must be evicted.

10. Overusing `common-lib`

Only contracts and simple shared types belong there. Business logic belongs in services.

---

## Best Use Cases For This Capstone Design

- Learning Spring Boot microservices basics.
- Demonstrating API Gateway routing.
- Demonstrating service discovery with Consul.
- Demonstrating service-owned tables.
- Demonstrating role-based access.
- Demonstrating validation and standard errors.
- Demonstrating product catalog and inventory workflows.
- Demonstrating circuit breaker fallback.
- Demonstrating Maven reactor builds.
- Demonstrating unit tests with Mockito.

This design is less suitable for:

- Production-scale authentication without JWT/OAuth2.
- High-concurrency inventory reservation without database locks or event-driven architecture.
- Large catalog search without database pagination or search indexes.
- Complex distributed transactions.

---

## Concepts Used In This Project

- Java 17
- Maven parent project
- Maven multi-module build
- Spring Boot 3
- Spring Cloud Gateway
- Spring Cloud Consul Discovery
- Spring Cloud Consul Config
- Spring Cloud LoadBalancer
- Spring Security Basic Auth
- Method-level security with `@PreAuthorize`
- BCrypt password encoding
- Spring Data JPA
- JPA entities
- JPA repositories
- MySQL datasource configuration
- DTO pattern
- Mapper pattern
- Bean Validation
- Global exception handling
- Standard API error response
- Service layer pattern
- Controller layer pattern
- Repository layer pattern
- Transactions with `@Transactional`
- Java Streams
- Predicate-based filtering
- Cache abstraction
- `@Cacheable`
- `@CacheEvict`
- Resilience4j circuit breaker
- Fallback method
- `RestTemplate`
- `@LoadBalanced`
- Actuator health and metrics
- Swagger/OpenAPI documentation
- `CommandLineRunner` seed data
- JUnit 5
- Mockito
- AssertJ
- Git initialization
- Git remote push
- `.gitignore`

---

## Most Asked Questions

### 1. Why use microservices instead of one Spring Boot app?

Microservices separate business capabilities. Catalog, inventory, users, and gateway can evolve independently. This improves maintainability and demonstrates cloud-native design.

### 2. Why use a gateway?

A gateway gives clients one stable entry point and hides internal service ports. It also centralizes routing and can centralize cross-cutting concerns.

### 3. Why use Consul?

Consul lets services register themselves and discover each other by service name. This avoids hardcoding host and port values for inter-service calls.

### 4. Why use DTOs instead of entities?

DTOs protect internal database structure, prevent accidental exposure of sensitive fields, and allow API contracts to evolve separately from persistence models.

### 5. Why not share entities in `common-lib`?

Shared entities tightly couple services to one database model. In microservices, each service owns its own data model.

### 6. Why use Resilience4j?

Distributed systems fail in parts. Resilience4j prevents one failing service from breaking the whole request chain and gives a controlled fallback response.

### 7. Why use `@Transactional`?

`@Transactional` makes database write operations atomic. If something fails, changes can be rolled back safely.

### 8. Why use Bean Validation?

Bean Validation catches bad input before it reaches business logic. This keeps services cleaner and error handling consistent.

### 9. Why use BCrypt?

BCrypt hashes passwords with salt and work factor. It is safer than storing raw passwords or using weak hashes.

### 10. Why use MySQL instead of H2?

The capstone requirement specifies MySQL. MySQL also behaves closer to a real deployed relational database than an in-memory test database.

### 11. Why is `common-lib` built first?

All services depend on its DTOs, enums, constants, and exceptions. Maven must compile it before modules that import it.

### 12. Why is catalog built before inventory?

Inventory validates product existence through catalog. Catalog is the source of product identity.

### 13. Why not make inventory directly query the products table?

That would break service ownership. Inventory should communicate through catalog's API, not its database tables.

### 14. Why include seed data?

Seed data makes demos and local testing easier because the APIs have known records immediately after startup.

### 15. Why keep tests independent of MySQL?

Unit tests should be fast and reliable. They validate business logic without requiring external infrastructure.

### 16. What would be improved in a production version?

Use JWT/OAuth2, database migrations with Flyway or Liquibase, Docker Compose, centralized logging, distributed tracing, Testcontainers, database locks for reservations, and CI/CD pipelines.
