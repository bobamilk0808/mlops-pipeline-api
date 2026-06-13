# MLOps Pipeline Management API

A JAX-RS (Jersey 2) RESTful API for managing Machine Learning Workspaces and Models in a cloud-native AI platform. Built with an embedded Grizzly HTTP server and in-memory data stores (no database).

---

## API Design Overview

| Base Path | `http://localhost:8080/api/v1` |
|-----------|-------------------------------|
| Protocol | HTTP/1.1 |
| Format | JSON (`application/json`) |
| Tech | JAX-RS 2.1 (Jersey 2.41) + Grizzly 2 |
| Storage | In-memory `HashMap` / `ArrayList` |

### Resource Hierarchy

```
GET    /api/v1/                          Discovery endpoint
GET    /api/v1/workspaces                List all workspaces
POST   /api/v1/workspaces                Create a workspace
GET    /api/v1/workspaces/{id}           Get workspace by ID
HEAD   /api/v1/workspaces/{id}           Check if workspace exists
DELETE /api/v1/workspaces/{id}           Delete workspace

GET    /api/v1/models                    List models (filter by ?status=)
POST   /api/v1/models                    Register a model
GET    /api/v1/models/{id}               Get model by ID

GET    /api/v1/models/{id}/metrics       Get evaluation history
POST   /api/v1/models/{id}/metrics       Add evaluation metric
```

### Pre-loaded Test Data

| ID | Type | Details |
|----|------|---------|
| `WS-VISION-01` | Workspace | Computer Vision Lab, 500 GB |
| `WS-NLP-02` | Workspace | NLP Research Team, 250 GB |
| `WS-EMPTY-03` | Workspace | Empty workspace (safe to delete) |
| `MOD-8832` | Model | TensorFlow, DEPLOYED, accuracy 0.92 |
| `MOD-1234` | Model | PyTorch, TRAINING, accuracy 0.75 |
| `MOD-5678` | Model | Scikit-Learn, DEPRECATED, accuracy 0.61 |

---

## Build & Run

### Prerequisites

- Java 11 or higher
- Apache Maven 3.6 or higher

### Steps

```bash
cd mlops-api
mvn clean package
java -jar target/mlops-api-1.0.0.jar
```

Server starts at **http://localhost:8080/api/v1**.

---

## Sample curl Commands

### 1. Discovery
```bash
curl -s http://localhost:8080/api/v1/
```

### 2. List all Workspaces
```bash
curl -s http://localhost:8080/api/v1/workspaces
```

### 3. Create a Workspace
```bash
curl -s -X POST http://localhost:8080/api/v1/workspaces \
  -H "Content-Type: application/json" \
  -d '{"id":"WS-TEST-99","teamName":"Data Science Team","storageQuotaGb":300}'
```

### 4. Delete an empty Workspace (204)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/workspaces/WS-EMPTY-03 -w "\nHTTP %{http_code}"
```

### 5. Delete a populated Workspace (409)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/workspaces/WS-VISION-01
```

### 6. Check Workspace exists via HEAD
```bash
curl -s -I http://localhost:8080/api/v1/workspaces/WS-NLP-02
```

### 7. Register a Model
```bash
curl -s -X POST http://localhost:8080/api/v1/models \
  -H "Content-Type: application/json" \
  -d '{"framework":"PyTorch","status":"TRAINING","latestAccuracy":0.80,"workspaceId":"WS-NLP-02"}'
```

### 8. Register Model with invalid workspaceId (422)
```bash
curl -s -X POST http://localhost:8080/api/v1/models \
  -H "Content-Type: application/json" \
  -d '{"framework":"XGBoost","status":"TRAINING","latestAccuracy":0.70,"workspaceId":"WS-NONEXISTENT"}'
```

### 9. Filter Models by status
```bash
curl -s "http://localhost:8080/api/v1/models?status=DEPLOYED"
```

### 10. Add Evaluation Metric
```bash
curl -s -X POST http://localhost:8080/api/v1/models/MOD-8832/metrics \
  -H "Content-Type: application/json" \
  -d '{"accuracyScore":0.97}'
```
The model's latestAccuracy updates to 0.97 after this request.

### 11. Post metric to DEPRECATED model (403)
```bash
curl -s -X POST http://localhost:8080/api/v1/models/MOD-5678/metrics \
  -H "Content-Type: application/json" \
  -d '{"accuracyScore":0.50}'
```

### 12. Get Evaluation History
```bash
curl -s http://localhost:8080/api/v1/models/MOD-8832/metrics
```

### 13. Trigger 500 error (global safety net demo)
```bash
curl -s http://localhost:8080/api/v1/admin/trigger-error
```

---

## Answers to Coursework Questions

### Part 1.1 - Role of MessageBodyWriter / JSON Provider

When a JAX-RS method returns a Java object, the framework must convert it into a format that the client can handle. That's what the MessageBodyWriter interface is for — it converts a Java object into bytes for the HTTP response body.

When you add and register Jackson using `JacksonFeature`, it also includes the `JacksonJsonProvider` class that implements `MessageBodyWriter`. This provider uses Jackson's `ObjectMapper` to process each field in the object and convert it to JSON format. For example, a String becomes a JSON string, and a List turns into a JSON array. It also automatically sets the `Content-Type: application/json` header on the response.

If this provider is not registered, JAX-RS cannot convert a POJO to JSON. It will throw an error saying that no writer was found for that type.

---

### Part 1.2 - Statelessness and Horizontal Scaling

Statelessness in REST means that the server doesn't keep any information about the client between requests. Each request must be independent and include all the details the server needs, such as authentication tokens or query parameters. The server doesn't remember who contacted it last or what they were doing.

This setup is very helpful for scaling horizontally. Since no session data is stored on any single server, a load balancer can direct each request to any server in the cluster without any concerns. If you have 10 servers and one fails, the others can handle the remaining traffic smoothly because they never held any client state. It's also easy to add more servers since you don’t have to synchronize session data across machines. 

---

### Part 2.1 - HTTP Cache-Control Headers

Adding `Cache-Control: max-age=60` to the GET /workspaces response allows clients and any middle proxies to store the response for 60 seconds. During this period, repeated requests come from the cache instead of reaching the server. This cuts down on the load and speeds up response times for the client.

For individual workspace lookups, you can use an `ETag` header with `Cache-Control: no-cache`. The client will send an `If-None-Match` header with follow-up requests. If there are no changes, the server responds with a `304 Not Modified` status and no content. This approach is more efficient than sending the complete JSON every time when the data hasn’t changed.

---

### Part 2.2 - HEAD Instead of GET for Existence Checks

The client should use the `HEAD` method. HEAD works like GET in terms of what the server does. It checks for the resource and returns the same status code, but it does not include a response body. If a workspace exists, you will receive a 200 status with only headers. If it does not exist, you will get a 404, all without downloading the actual JSON.

This is useful when you want to check if something exists without wasting bandwidth on unnecessary data. It is safe and idempotent, meaning there are no side effects. That’s why I added a `HEAD /workspaces/{workspaceId}` endpoint along with the GET one in `WorkspaceResource`.

---

### Part 3.1 - Server-Side ID Generation

Letting the server create the ID with `UUID.randomUUID()` instead of allowing the client to provide one offers several benefits.

If a client could input their own ID, they might overwrite an existing resource by using the same ID. They could also try to guess valid IDs to access restricted data. When UUIDs are generated on the server, these IDs are unpredictable and unique, which helps prevent such attacks.

For data integrity, generating IDs on the server ensures they are always unique. If two clients make requests at the same time, they will not receive the same ID. This problem could happen if clients chose IDs themselves without coordination.

---

### Part 3.2 - URL Encoding

The client would need to encode the value like this:

```

?framework=Scikit%20Learn%20%26%20Tools

```


Spaces become `%20` and the `&` character becomes `%26`.

This is necessary because URLs have reserved characters that indicate specific functions in their structure. The `&` symbol separates query parameters, so if it appears inside a parameter value without encoding, the server may interpret the URL as containing multiple parameters and parse it incorrectly. Spaces are not valid in URLs. By percent-encoding these characters, the client ensures the value is sent as a single, clear string. JAX-RS automatically decodes it on the server side, so `@QueryParam` receives the original clean value without the encoding.

---

### Part 4.1 - Class-Level vs Method-Level @Produces

Putting `@Produces(MediaType.APPLICATION_JSON)` at the class level means it applies to every method in that class by default. This way, you do not have to repeat it for each method. It keeps things cleaner and allows you to change the media type in one place if needed.

Method-level annotations override the class-level annotation for specific methods. If a method has its own `@Produces`, JAX-RS uses that and ignores the class-level annotation for that method. For example, if most methods produce JSON but one needs to return plain text, you would just annotate that method with `@Produces("text/plain")`. Everything else would still use the class-level JSON default. There is no merging; the method-level annotation completely replaces the class one for that endpoint.

---

### Part 5.2 - Why Validation Failures Should Be 4xx Not 5xx

The 4xx range shows that the problem came from the client. The 5xx range indicates there was an issue on the server side. When a client sends a `workspaceId` that doesn't exist, the server has acted correctly, and the API is working as it should. The problem lies with the request because the client supplied invalid data. Returning a 5xx status would falsely suggest that the server encountered an internal error or crashed, which isn’t true.

422 makes more sense here because the JSON was valid, and the request was understood, but the content could not be processed since it refers to something that doesn’t exist. It lets the client know they need to correct their request instead of retrying it or reporting a server issue.

---

### Part 5.4 - How JAX-RS Picks the Right Exception Mapper

JAX-RS looks for the most specific mapper for the thrown exception. It checks the class hierarchy of the exception and selects the registered mapper for the closest matching type.

If `WorkspaceNotEmptyException` is thrown, JAX-RS finds and uses the `WorkspaceNotEmptyExceptionMapper`. The `GlobalExceptionMapper<Throwable>` is not involved.

If an unexpected exception, like `NullPointerException`, is thrown and there is no specific mapper for it, JAX-RS goes up the hierarchy through `RuntimeException` and `Exception` until it reaches `Throwable`. There, it finds the global mapper and uses that. 

In my `GlobalExceptionMapper`, I also check for `WebApplicationException` and pass those through directly. 

---

### Part 5.5 - HTTP Metadata from Filter Contexts

Two useful pieces of metadata from the filter contexts:

1. The request URI from `requestContext.getUriInfo().getRequestUri()` combined with `requestContext.getMethod()` gives you the exact endpoint that was called, including any query parameters. This is the most important detail to log. When something goes wrong, you can quickly see what was called and reproduce the issue.

2. The response status code from `responseContext.getStatus()` immediately shows whether the request succeeded or failed and the nature of the failure. When you pair this with the `Authorization` header from the request, you can identify who made the call and what happened. This helps in tracking down access control issues or failed requests from specific clients.
