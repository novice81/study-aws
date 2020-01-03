# Java example

Java example based on Spring Boot.

## Run Server

```zsh
/example-java % gradle bootRun
```

## Music Collection List

```zsh
% curl localhost:8080/music/collections
[{"artist":"John Mayer","songTitle":"Carry Me Away"}]
```

## Put Music Item

```zsh
% curl -X POST \
    -H "Content-Type:application/json" \
    -d '{"artist":"John Mayer", "songTitle":"Carry Me Away"}' \
    localhost:8080/music/collections
{"artist":"John Mayer","songTitle":"Carry Me Away"}
```
