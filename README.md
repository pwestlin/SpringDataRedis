# Publish-subscribe with Spring Boot and Redis
A "news application" that sends (publishes) news with category and an headline and some receivers collect (subscribes) them. 

## Execute

### With Docker Compose
1. Build a Docker image for the application: ```./gradlew bootBuildImage --imageName=pwestlin/springdataredis```
2. Run with Docker Compose: ```docker-compose -f src/main/docker/docker-compose.yml up```

### With Docker and Gradle
4. Start a Redis Docker-container: ```docker run --name SpringDataRedis -p 6379:6379 -d redis```
5. Execute application: ```./gradlew bootrun```
