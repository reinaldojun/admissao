# 🧮 Aplicação Spring Boot: Cálculos, MongoDB, Swagger e Health Check

Este projeto é uma API REST construída com **Spring Boot**, que oferece funcionalidades de cálculo com persistência em **MongoDB**, documentação via **Swagger**, monitoramento com **Spring Boot Actuator** e tratamento robusto de erros.

---

## 📚 Sumário

- [📁 Estrutura da API](#-estrutura-da-api)
- [✅ Pré-requisitos](#-pré-requisitos)
- [⚙️ Configuração](#️-configuração)
- [📦 Dependências Maven](#-dependências-maven)
- [📡 Endpoints e Exemplos](#-endpoints-e-exemplos)
- [🛠️ Erros e Validações](#️-erros-e-validações)
- [📚 Documentação](#-documentação)
- [👨‍💻 Autor](#-autor)

---

## 📁 Estrutura da API

| Endpoint | Descrição |
|---------|-----------|
| `POST /api/calculos` | Realiza cálculo e salva os dados |
| `GET /api/calculos/por-data` | Lista registros por data de admissão |
| `GET /api/calculos/por-salario` | Lista registros com salário mínimo |
| `GET /api/calculos` | Lista todos os registros com paginação |
| `GET /v3/api-docs` | Documentação OpenAPI |
| `GET /swagger-ui.html` | Interface Swagger UI |
| `GET /actuator/health` | Health check da aplicação |

---

## ✅ Pré-requisitos

- Java 17+
- MongoDB local (`localhost:27017`)
- Maven 3.8+

---

## ⚙️ Configuração

### `application.yml`

```yaml
server:
  port: 8081

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/admissao

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

---

## 📦 Dependências Maven

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

---

## 📡 Endpoints e Exemplos

### 🔹 `POST /api/calculos`

Calcula e persiste uma nova admissão.

**Exemplo `curl`**:

```bash
curl -X POST http://localhost:8081/api/calculos   -H "Content-Type: application/json"   -d '{
    "dataAdmissao": "2023-01-10",
    "salarioBruto": 4200.00,
    "cep": "66050080"
}'
```

### 🔹 `GET /api/calculos/por-data`

Filtra por data de admissão.

```bash
curl "http://localhost:8081/api/calculos/por-data?inicio=2023-01-01&fim=2024-01-01&page=0&size=5&sort=criadoEm,desc"
```

### 🔹 `GET /api/calculos/por-salario`

Filtra por salário mínimo.

```bash
curl "http://localhost:8081/api/calculos/por-salario?min=3000&page=0&size=10"
```

### 🔹 `GET /api/calculos`

Lista todos paginados.

```bash
curl "http://localhost:8081/api/calculos?page=0&size=20&sort=salarioBruto,desc"
```

### 🔹 `GET /v3/api-docs`

```bash
curl http://localhost:8081/v3/api-docs
```

### 🔹 `GET /swagger-ui.html`

Acesse via navegador:

- http://localhost:8081/swagger-ui.html
- http://localhost:8081/swagger-ui/index.html

### 🔹 `GET /actuator/health`

```bash
curl http://localhost:8081/actuator/health
```

> 🔄 Certifique-se de que o `management.endpoints.web.exposure.include` está com `"*"`.

---

## 🛠️ Erros e Validações

### 📥 Validações no DTO

- `dataAdmissao`: obrigatória, formato `yyyy-MM-dd`, passado ou presente.
- `salarioBruto`: obrigatório, > 0.
- `cep`: obrigatório, formato `12345-678` ou `12345678`.

### ❌ Respostas de Erro

#### `400 Bad Request`

- Dados inválidos no corpo JSON
- Parâmetros ausentes ou com tipo inválido

**Exemplo de retorno**:

```json
{
  "timestamp": "2025-08-06T15:20:00-03:00",
  "status": 400,
  "error": "Validation Error",
  "messages": [
    "salarioBruto: deve ser maior que zero",
    "cep: CEP inválido. Formato esperado: 12345-678 ou 12345678"
  ],
  "path": "/api/calculos"
}
```

#### `422 Unprocessable Entity`

- Erros de negócio lançados como `ApiException`.

#### `500 Internal Server Error`

- Qualquer exceção não tratada.

**Exemplo**:

```json
{
  "timestamp": "2025-08-06T14:06:39-03:00",
  "status": 500,
  "error": "Internal Server Error",
  "messages": [
    "Erro interno: NullPointerException"
  ],
  "path": "/actuator/health"
}
```

---

## 📚 Documentação

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/actuator-api/html/)
- [SpringDoc OpenAPI](https://springdoc.org/)

---

## 👨‍💻 Autor

**Reinaldo Viana**  
Desenvolvedor backend | Java | Spring | MongoDB

---