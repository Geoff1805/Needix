package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OffProductResponse(
    val status: Int? = null,
    val product: OffProductDetails? = null
)

@JsonClass(generateAdapter = true)
data class OffProductDetails(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_fr") val productNameFr: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @Json(name = "image_front_small_url") val imageUrl: String? = null,
    @Json(name = "categories_tags") val categories: List<String>? = null
)

data class ProductLookupResult(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val suggestedCategory: String = "aliments",
    val imageUrl: String? = null
)

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OffProductResponse
}

object ProductLookupService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    suspend fun lookupBarcode(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        try {
            val response = api.getProduct(barcode.trim())
            if (response.status == 1 && response.product != null) {
                val p = response.product
                val name = p.productNameFr?.takeIf { it.isNotBlank() }
                    ?: p.productName?.takeIf { it.isNotBlank() }
                    ?: "Produit ($barcode)"
                val brand = p.brands?.takeIf { it.isNotBlank() }
                val fullName = if (!brand.isNullOrBlank() && !name.contains(brand, ignoreCase = true)) {
                    "$name ($brand)"
                } else name

                // Simple category guessing based on tags
                val tags = p.categories?.joinToString(" ")?.lowercase() ?: ""
                val categoryId = when {
                    tags.contains("cleaning") || tags.contains("menage") || tags.contains("detergent") -> "menager"
                    tags.contains("hygiene") || tags.contains("cosmetic") || tags.contains("beauty") -> "hygiene"
                    tags.contains("health") || tags.contains("pharma") || tags.contains("medicin") -> "pharmacie"
                    tags.contains("condiment") || tags.contains("spice") || tags.contains("oil") || tags.contains("epice") -> "cuisine"
                    else -> "aliments"
                }

                ProductLookupResult(
                    barcode = barcode,
                    name = fullName,
                    brand = brand,
                    suggestedCategory = categoryId,
                    imageUrl = p.imageUrl
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
