# Publish-subscribe with Spring Boot and Redis
A "news application" that sends (publishes) news with category and an headline and some receivers collect (subscribes) them. 

## Execute
1. Start a Redis Docker-container: ```docker run --name SpringDataRedis -p 6379:6379 -d redis```
2. Execute application: ```./gradlew bootrun```