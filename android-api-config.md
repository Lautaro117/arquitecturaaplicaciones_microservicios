# Configuración de API para Android

## Base URL

Desde un **emulador Android** la IP del host es `10.0.2.2`, no `localhost`.

```
Emulador:         http://10.0.2.2:8080
Dispositivo físico: http://<IP_DE_TU_PC>:8080
```

> Todos los requests pasan por el **API Gateway** (puerto 8080).

---

## Dependencias Gradle (build.gradle / app)

```groovy
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
}
```

---

## AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

---

## res/xml/network_security_config.xml

Necesario para permitir tráfico HTTP (sin HTTPS) hacia el backend local:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## Endpoints disponibles

| Método | Ruta                         | Auth    | Body (JSON)                                      | Respuesta                |
|--------|------------------------------|---------|--------------------------------------------------|--------------------------|
| POST   | `/auth/login`                | No      | `{ "username": "...", "password": "..." }`       | `TokenResponse`          |
| GET    | `/api/inventory/products`    | Bearer  | -                                                | `List<Product>`          |
| GET    | `/api/inventory/products/{id}` | Bearer | -                                                | `Product`                |
| POST   | `/api/inventory/products`    | Bearer  | `{ "name": "...", "quantity": 0, "price": 0.0 }` | `Product`                |

---

## Usuarios de prueba

| Usuario | Password  | Roles       |
|---------|-----------|-------------|
| admin   | admin123  | ADMIN, USER |
| user    | user123   | USER        |

---

## Modelos (data classes Kotlin)

```kotlin
// --- Auth ---
data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val token: String,
    val type: String,      // "Bearer"
    val expiresIn: Long    // 3600 (segundos)
)

// --- Inventory ---
data class Product(
    val id: Long? = null,
    val name: String,
    val quantity: Int,
    val price: Double
)
```

---

## Retrofit - Service Interfaces

```kotlin
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>
}

interface InventoryApi {
    @GET("/api/inventory/products")
    suspend fun getProducts(): Response<List<Product>>

    @GET("/api/inventory/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): Response<Product>

    @POST("/api/inventory/products")
    suspend fun createProduct(@Body product: Product): Response<Product>
}
```

---

## Interceptor para JWT

```kotlin
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        tokenProvider()?.let { token ->
            request.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(request.build())
    }
}
```

---

## Configuración de Retrofit

```kotlin
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var authToken: String? = null

    fun setToken(token: String) {
        authToken = token
    }

    fun clearToken() {
        authToken = null
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { authToken })
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val inventoryApi: InventoryApi by lazy { retrofit.create(InventoryApi::class.java) }
}
```

---

## Ejemplo de uso

```kotlin
// Login
val response = ApiClient.authApi.login(LoginRequest("admin", "admin123"))
if (response.isSuccessful) {
    val token = response.body()!!.token
    ApiClient.setToken(token)
}

// Obtener productos (ya autenticado)
val products = ApiClient.inventoryApi.getProducts()
if (products.isSuccessful) {
    val list = products.body()!!
}

// Crear producto
val newProduct = Product(name = "Monitor", quantity = 15, price = 299.99)
val created = ApiClient.inventoryApi.createProduct(newProduct)
```
