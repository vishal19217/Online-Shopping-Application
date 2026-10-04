---
name: springboot-flyway-mysql
description: Troubleshoot and set up Flyway migrations + Dockerized MySQL for Spring Boot microservices. Use when Flyway migrations don't run, a MySQL docker-entrypoint init.sql is skipped, the app can't find its database, or entity/table names mismatch the schema. Covers Spring Boot 4 modular autoconfiguration, docker-compose MySQL init behavior, and JPA naming defaults.
---

# Spring Boot + Flyway + Dockerized MySQL

Checklist for getting Flyway migrations and a docker-compose MySQL to work together
in a Spring Boot service, plus the non-obvious failures that silently break them.

## 1. Flyway doesn't run at all (no log lines, no `flyway_schema_history`)

**Spring Boot 4 split autoconfiguration into per-feature modules.** Adding raw
`org.flywaydb:flyway-core` puts the Flyway *library* on the classpath but NOT the
`FlywayAutoConfiguration` that triggers migration on startup — that lives in the
separate `spring-boot-flyway` module. Result: Flyway does nothing, zero log output.

**Fix — use the starter, not the raw lib:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>   <!-- DB-specific support, keep this -->
</dependency>
```
Verify it pulled in the autoconfig module:
`./mvnw dependency:tree | grep -i flyway` should show `spring-boot-flyway`.

**Diagnosis clue:** in the startup log you see Hikari + Hibernate init but NO
`Flyway Community Edition` / `Migrating schema` lines. That means autoconfig is absent
(a wiring problem), not a migration error.

## 2. MySQL `init.sql` is skipped / database never created

`docker-entrypoint-initdb.d/*.sql` runs **only when the data directory is empty**
(first-time init). If your compose bind-mounts a host folder for `/var/lib/mysql`
and that folder already has data from a previous run, MySQL sees an initialized
datadir and **silently skips** every init script.

**Symptom:** `ERROR 1049 (42000) Unknown database 'xxx'` — the `CREATE DATABASE`
in init.sql never executed.

**Fix:** stop the container and clear/rename the stale data dir, then recreate:
```bash
docker compose down
mv docker/mysql/data docker/mysql/data_backup   # reversible; delete once verified
docker compose up -d
```

**Also check the compose file for these mistakes:**
- Invalid `version:` (e.g. `version: '4'` — Compose versions only go to 3.x; just omit it).
- A corrupted volume target — must be exactly `- ./docker/mysql/data:/var/lib/mysql`.

## 3. Split responsibilities: init.sql vs Flyway

- `init.sql` (docker-entrypoint) → **only** `CREATE DATABASE IF NOT EXISTS ...;`
  (Flyway needs the schema to exist before it can connect.)
- Flyway `V1__...sql`, `V2__...sql` → tables and seed data.
- Keep `spring.jpa.hibernate.ddl-auto=none` so Hibernate doesn't fight Flyway.

## 4. JPA entity ↔ migration name mismatches (runtime failures)

Even with the table created, queries fail if the entity maps to different names.
Spring Boot's default `CamelCaseToUnderscoresNamingStrategy` means:
- `@Table` with no `name` → table = lowercased class name (`inventory`), NOT `t_inventory`.
- field `skuCode` → column `sku_code`, NOT `skucode`.

**Fix — pin names explicitly to match the migration:**
```java
@Entity
@Table(name = "t_inventory")
public class Inventory {
    @Column(name = "skucode")
    private String skuCode;
}
```

## Verify end-to-end without the app compiling

To prove migrations work independently of app code, run Flyway via the Maven plugin:
```bash
./mvnw org.flywaydb:flyway-maven-plugin:<version>:migrate \
  -Dflyway.url=jdbc:mysql://localhost:3307/<db> \
  -Dflyway.user=root -Dflyway.password=<pw> \
  -Dflyway.locations=filesystem:src/main/resources/db/migration
```
Then confirm: `SELECT * FROM <db>.t_inventory;` and
`SELECT version,description,success FROM <db>.flyway_schema_history;`
