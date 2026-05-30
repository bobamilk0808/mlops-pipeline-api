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

Server starts at **http://localhost:8080/api/v1**. Press Ctrl+C to stop.

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

When a JAX-RS method returns a Java object, the framework needs something to convert it into a format the client can actually receive. This is handled by the `MessageBodyWriter` interface, which is responsible for serialising a Java object into bytes that get written to the HTTP response body.

When Jackson is added and registered using `JacksonFeature`, it registers a `JacksonJsonProvider` class that implements `MessageBodyWriter`. It uses Jackson's `ObjectMapper` under the hood to go through each field in the object and convert it to its JSON equivalent. For example, a String becomes a JSON string, a List becomes a JSON array and so on. It also sets the `Content-Type: application/json` header on the response automatically.

Without this provider being registered, JAX-RS would have no way of converting a POJO to JSON and would throw an error saying no writer was found for that type.

---

### Part 1.2 - Statelessness and Horizontal Scaling

Statelessness in REST means the server does not store any information about the client between requests. Every request has to be self-contained and include all the information the server needs, such as authentication tokens or query parameters. The server doesn't keep track of who called it last or what they were doing.

This is really useful for scaling horizontally because if no session data is stored on any individual server, a load balancer can send each request to any server in the cluster without worrying about it. If you have 10 servers and one goes down, the others can handle the remaining traffic without any issue because they were never holding any client state to begin with. Adding more servers is straightforward too since you don't need to sync session data across machines. In contrast, a stateful API would need sticky sessions, where each client is locked to a specific server, which makes scaling much harder.

---

### Part 2.1 - HTTP Cache-Control Headers

Adding `Cache-Control: max-age=60` to the GET /workspaces response would mean that clients and any intermediate proxies can store the response for 60 seconds. During that time, repeated requests would be served from cache rather than hitting the server, which reduces unnecessary load and speeds up response times for the client.

For individual workspace lookups, you could use an `ETag` header alongside `Cache-Control: no-cache`. The client would then send an `If-None-Match` header on follow-up requests, and if nothing has changed the server returns a `304 Not Modified` with no body at all. This is more efficient than sending the full JSON every time when the data hasn't actually changed.

---

### Part 2.2 - HEAD Instead of GET for Existence Checks

The client should use the `HEAD` method. HEAD works the same as GET in terms of what the server does, it performs the lookup and returns the same status code, but it doesn't include a response body. So if a workspace exists you get a 200 with just headers, and if it doesn't you get a 404, all without downloading the actual JSON.

This is useful when you only want to check if something exists without wasting bandwidth downloading data you don't need. It's safe and idempotent so there are no side effects. This is why I implemented a `HEAD /workspaces/{workspaceId}` endpoint alongside the GET one in `WorkspaceResource`.

---

### Part 3.1 - Server-Side ID Generation

Letting the server generate the ID using `UUID.randomUUID()` rather than accepting one from the client is better for a few reasons.

From a security perspective, if a client could supply their own ID they could overwrite an existing resource by sending the same ID, or try to guess valid IDs to access data they shouldn't. With UUIDs generated server-side, IDs are unpredictable and unique so neither of those attacks work.

For data integrity, generating IDs server-side guarantees they will always be unique. Two clients making requests at the same time can't end up with the same ID, which would happen if clients were picking IDs themselves without any coordination.

Regarding Bean Validation vs manual checks: Jakarta Bean Validation (JSR 380) lets you add constraints directly to POJO fields with annotations like `@NotNull` or `@Size`, and when `@Valid` is used on a method parameter Jersey will validate them automatically. This works well for simple things like checking a field isn't null or checking a string length. However it can't handle relational checks, like verifying a `workspaceId` actually exists in the data store, because that requires querying the data store which annotations can't do. For that reason I used a manual if-check in `ModelResource` and threw a `LinkedWorkspaceNotFoundException` when the workspace wasn't found, which then gets handled by a dedicated exception mapper returning a 422.

---

### Part 3.2 - URL Encoding

The client would need to encode the value like this:

```
?framework=Scikit%20Learn%20%26%20Tools
```

Spaces become `%20` and the `&` character becomes `%26`.

This is necessary because URLs have reserved characters that mean specific things in the structure of the URL. The `&` symbol is used to separate query parameters, so if it appears inside a parameter value without encoding, the server would think the URL contains multiple parameters and parse it incorrectly. Spaces aren't valid in URLs at all. By percent-encoding these characters, the client ensures the value is transmitted as a single unambiguous string. JAX-RS automatically decodes it on the server side, so `@QueryParam` gets the original clean value without the encoding.

---

### Part 4.1 - Class-Level vs Method-Level @Produces

Putting `@Produces(MediaType.APPLICATION_JSON)` at the class level means it applies to every method in that class by default, so you don't have to repeat it on every single method. It keeps things cleaner and means if you ever need to change the media type you only have to change it in one place.

Method-level annotations override the class-level one for that specific method. If a method has its own `@Produces`, JAX-RS uses that and ignores the class-level annotation entirely for that method. For example if most methods produce JSON but one needs to return plain text, you'd just annotate that one method with `@Produces("text/plain")` and everything else would still use the class-level JSON default. There's no merging, the method-level annotation fully replaces the class one for that endpoint.

---

### Part 5.2 - Why Validation Failures Should Be 4xx Not 5xx

The 4xx range means the problem was caused by the client. The 5xx range means something went wrong on the server side. When a client sends a `workspaceId` that doesn't exist, the server has done everything right and the API is working correctly. The request itself is the problem because the client provided invalid data. Returning a 5xx would incorrectly suggest the server had an internal error or crashed, which isn't true.

Using 422 Unprocessable Entity makes sense here because the JSON was valid and the request was understood, but the content couldn't be processed because it references something that doesn't exist. It tells the client they need to fix their request, not retry it or report a server issue.

---

### Part 5.4 - How JAX-RS Picks the Right Exception Mapper

JAX-RS looks for the most specific mapper available for the exception that was thrown. It goes up the class hierarchy of the exception and picks the mapper registered for the closest matching type.

So if `WorkspaceNotEmptyException` is thrown, JAX-RS finds the `WorkspaceNotEmptyExceptionMapper` straight away and uses it. The `GlobalExceptionMapper<Throwable>` is never involved.

If something unexpected like a `NullPointerException` is thrown and there's no specific mapper for it, JAX-RS works up the hierarchy through `RuntimeException` and `Exception` until it gets to `Throwable`, where it finds the global mapper and uses that. This means specific mappers always win and the global one is only a fallback for anything that isn't handled elsewhere.

In my `GlobalExceptionMapper` I also check for `WebApplicationException` and pass those through directly so Jersey's own built-in error responses aren't broken by the catch-all.

---

### Part 5.5 - HTTP Metadata from Filter Contexts

Two useful pieces of metadata from the filter contexts:

1. The request URI from `requestContext.getUriInfo().getRequestUri()` combined with `requestContext.getMethod()` gives you the exact endpoint that was called including any query parameters. This is the most important thing to log because when something goes wrong you can immediately see what was called and reproduce it.

2. The response status code from `responseContext.getStatus()` tells you straight away whether the request succeeded or failed and what kind of failure it was. Combined with the `Authorization` header from the request, you can see who made the call and what happened, which is useful for tracking down access control issues or failed requests from specific clients.
