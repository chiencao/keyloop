# keyloop

Sample Spring Boot 3 project (Java 21, Maven) with a small REST endpoint and
unit/slice test examples.

## Requirements

- Java 21 (`java -version`)
- No local Maven needed — the Maven Wrapper (`./mvnw`) is included.

## Run

```bash
./mvnw spring-boot:run
```

Then:

```bash
curl "http://localhost:8080/greeting?name=Chien"
# {"id":1,"content":"Hello, Chien!"}
```

## Test

```bash
./mvnw test
```

## Layout

```
src/main/java/com/example/keyloop
├── KeyloopApplication.java        # entry point
├── controller/GreetingController  # REST endpoint + error handling
├── service/GreetingService        # business logic (unit-testable)
├── model/Greeting.java            # response record
└── exception/InvalidNameException # domain exception

src/test/java/com/example/keyloop
├── service/GreetingServiceTest    # plain JUnit 5 unit test (no Spring)
├── controller/GreetingControllerTest # @WebMvcTest slice + Mockito
└── KeyloopApplicationTests        # @SpringBootTest context smoke test
```
