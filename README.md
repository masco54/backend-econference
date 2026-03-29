# eConference Backend

Monólito modular com Spring Boot 3.4 + Java 24.

## Módulos

| Módulo | Descrição |
|--------|-----------|
| `shared` | Kernel compartilhado: entidade base, exceções, config |
| `mod-auth` | Autenticação, JWT, usuários |
| `mod-event` | Criação e gestão de eventos |
| `mod-submission` | Submissões de artigos |
| `mod-review` | Avaliação e revisão |
| `mod-committee` | Comitê organizador |
| `mod-registration` | Inscrições em eventos |
| `application` | Ponto de entrada – monta todos os módulos |

## Rodar local (H2 em memória)

```bash
./mvnw spring-boot:run -pl application
```

## Rodar local com PostgreSQL

```bash
docker compose up -d
./mvnw spring-boot:run -pl application -Dspring-boot.run.profiles=postgres
```

## Build

```bash
./mvnw clean package
```

## Endpoints de health por módulo

- `GET /api/auth/health`
- `GET /api/events/health`
- `GET /api/submissions/health`
- `GET /api/reviews/health`
- `GET /api/committees/health`
- `GET /api/registrations/health`
