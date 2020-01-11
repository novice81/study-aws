# Java example

Java example based on Spring Boot.

Referred the [Spring Boot Guide](https://github.com/spring-guides/gs-spring-boot).

## Run Server

### With Gradle

```zsh
# in the example-java directory
% gradle bootRun
```

### With jar

```zsh
% java -jar dynamodb-sample-0.1.0.jar
```

#### In the EC2 instance

This allows the app to use the `InstanceProfileCredentialsProvider`.
  Or use the `ProfileCredentialsProvider` with the profile name `dynamodb`.

```zsh
% java -jar dynamodb-sample-0.1.0.jar --server.port=$NOVICE_SERVICE_PORT --spring.profiles.active=stage
```

## Scan MusicCollection table

```zsh
% curl localhost:8080/music/collections
[{"artist":"John Mayer","songTitle":"Carry Me Away"}]
```

## Put MusicCollection item

```zsh
% curl -X POST \
    -H "Content-Type:application/json" \
    -d '{"artist":"John Mayer", "songTitle":"Carry Me Away"}' \
    localhost:8080/music/collections
{"artist":"John Mayer","songTitle":"Carry Me Away"}
```
