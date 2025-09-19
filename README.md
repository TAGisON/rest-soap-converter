# Dynamic REST to SOAP Converter

A Spring Boot application that dynamically exposes SOAP endpoints and converts upstream REST API responses to SOAP responses based on database configuration.

## Features

- **Dynamic SOAP Endpoints**: Create, update, and delete SOAP endpoints at runtime via Admin REST API
- **Multiple Authentication Support**: Support for Basic, API Key, Bearer, and OAuth2 client credentials for upstream REST APIs
- **Flexible Mapping Engine**: MapStruct, JOLT, and Groovy script support for data transformation
- **Resilience Patterns**: Circuit breaker, retry, and timeout handling with Resilience4j
- **Caching**: Local in-memory caching with configurable TTL per endpoint
- **Observability**: Structured logging, metrics, and health checks
- **Offline Operation**: Works in private/offline environments without runtime internet dependency

## Technology Stack

- **Java 8**
- **Spring Boot 2.7.18**
- **PostgreSQL** (with configurable dev credentials)
- **Spring Web Services** for SOAP endpoints
- **WebClient** for REST API calls
- **MapStruct, JOLT, Groovy** for data mapping
- **Resilience4j** for resilience patterns
- **Caffeine** for local caching
- **Flyway** for database migrations
- **WireMock** for testing

## Quick Start

### Prerequisites

- Java 8 or higher
- PostgreSQL 12+ running on localhost:5432
- Maven 3.6+

### Database Setup

Create a PostgreSQL database and user:

```sql
-- Connect to PostgreSQL as superuser
CREATE DATABASE postgres;  -- or use existing
-- Ensure user 'postgres' exists with password 'coral@123'
-- Default configuration uses: 
-- Host: localhost, Port: 5432, Database: postgres, User: postgres, Password: coral@123
```

### Build and Run

1. **Clone/extract the project:**
   ```bash
   cd rest-soap-converter/
   ```

2. **Build the project:**
   ```bash
   mvn clean package -DskipTests=false
   ```

3. **Run the application:**
   ```bash
   java -jar target/rest-soap-converter-1.0.0.jar
   ```

4. **Verify startup:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Usage

### Admin API Examples

#### 1. Create a SOAP Endpoint

```bash
curl -X POST http://localhost:8080/admin/endpoints   -H "Content-Type: application/json"   -d '{
    "name": "UserLookupEndpoint",
    "operationName": "GetUserInfo", 
    "namespace": "http://example.com/userservice",
    "enabled": true,
    "restCalls": [
      {
        "sequenceOrder": 1,
        "method": "GET",
        "urlTemplate": "https://jsonplaceholder.typicode.com/users/1",
        "authType": "NONE",
        "timeoutMs": 30000
      }
    ],
    "mappings": [
      {
        "mappingType": "JOLT",
        "mappingDefinition": "[{"operation":"shift","spec":{"call_1":{"body":{"name":"data.userName","email":"data.userEmail"}},"statusCode":"status"}},{"operation":"default","spec":{"status":200,"message":"User data retrieved"}}]",
        "executionOrder": 1
      }
    ]
  }'
```

#### 2. List All Endpoints

```bash
curl http://localhost:8080/admin/endpoints
```

#### 3. Get Specific Endpoint

```bash
curl http://localhost:8080/admin/endpoints/1
```

#### 4. Update Endpoint

```bash
curl -X PUT http://localhost:8080/admin/endpoints/1   -H "Content-Type: application/json"   -d '{
    "name": "UpdatedEndpoint",
    "operationName": "UpdatedOperation",
    "namespace": "http://example.com/updated",
    "enabled": true,
    "restCalls": [...],
    "mappings": [...]
  }'
```

#### 5. Delete Endpoint

```bash
curl -X DELETE http://localhost:8080/admin/endpoints/1
```

#### 6. Test Endpoint (Dry Run)

```bash
curl -X POST http://localhost:8080/admin/endpoints/1/test   -H "Content-Type: application/xml"   -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
        <soapenv:Body>
          <GetUserInfo xmlns="http://example.com/userservice">
            <id>1</id>
          </GetUserInfo>
        </soapenv:Body>
      </soapenv:Envelope>'
```

## Testing

### Run All Tests
```bash
mvn test
```

### Run Integration Tests Only
```bash
mvn test -Dtest=EndToEndIntegrationTest
```

## Final Checks

Run these commands to verify the setup:

1. **Build verification**:
   ```bash
   mvn clean package -DskipTests=false
   echo "Exit code: $?"  # Should be 0
   ```

2. **JAR verification**:
   ```bash
   ls -la target/rest-soap-converter-1.0.0.jar
   java -jar target/rest-soap-converter-1.0.0.jar --help
   ```

3. **Database verification**:
   ```bash
   # Start application and check if tables are created
   java -jar target/rest-soap-converter-1.0.0.jar &
   sleep 10
   curl http://localhost:8080/actuator/health
   pkill -f rest-soap-converter
   ```

## License

This project is licensed under the MIT License.
