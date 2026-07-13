# Quickstart: JWT via Cookie HttpOnly — Backend Spring Boot

**Feature**: 004-jwt-cookie-back

## Pré-requisitos

- Java 24 instalado (`java -version`)
- Docker Desktop rodando (para PostgreSQL em dev, ou usar H2)

## Rodar o backend

```bash
cd back-econference
./mvnw spring-boot:run -pl application
# Backend em http://localhost:8080
```

## Testar via curl

```bash
# 1. Login — verificar cookie no Set-Cookie
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@econference.com","password":"sua-senha"}' \
  -v 2>&1 | grep -i "set-cookie"

# 2. GET /auth/me com cookie
curl -b cookies.txt http://localhost:8080/api/v1/auth/me

# 3. GET /auth/me sem cookie → deve retornar 401
curl http://localhost:8080/api/v1/auth/me

# 4. Logout — verificar Max-Age=0
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/v1/auth/logout \
  -v 2>&1 | grep -i "set-cookie"
```

## Executar testes

```bash
# Testes do mod-auth
./mvnw test -pl mod-auth

# Todos os módulos
./mvnw test
```

## Arquivos a modificar

| Arquivo | Mudança |
|---|---|
| `mod-auth/.../AuthController.java` | Gap 1: adicionar `Secure` + `SameSite` no cookie de logout |
| `application/src/main/resources/application-production.properties` | Gap 2: adicionar `app.cors.allowed-origins` |
| `mod-auth/src/test/.../AuthControllerIntegrationTest.java` | Gap 3: criar testes de integração para fluxo de cookie |
