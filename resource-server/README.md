# resource-server

```shell
./mvnw spring-boot:run
```
Then you can test the API with a valid token

```shell
TOKEN=.....
http :8080/api/user Authorization:"Bearer $TOKEN"
```

