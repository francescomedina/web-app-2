package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.ProductEntity
import it.polito.wa2.warehouse.domain.RatingEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull


data class ProductDTO(

    val id: ObjectId? = null,

    @field:NotBlank(message = "Name is required")
    val name: String = "",

    // Optional field
    val description: String? = "",
    val pictureURL: String? = "",
    val category: String? = null,

    @field:NotNull(message ="Price is required")
    @field:DecimalMin(value = "0.0", message = "Price must be positive")
    val price: BigDecimal? = null,

    val averageRating: Float = 0.0f,
    val rating: List<RatingDTO> = emptyList(),
)


data class UpdateProductDTO(

    val id: ObjectId? = null,
    val name: String?,

    // Optional field
    val description: String?,
    val pictureURL: String?,
    val category: String? = null,

    @field:DecimalMin(value = "0.0", message = "Price must be positive")
    val price: BigDecimal? = null,

    val averageRating: Float = 0.0f,
    val rating: List<RatingDTO> = emptyList(),
)

fun ProductEntity.toProductDTO(): ProductDTO {
    return ProductDTO(
        id = id,
        name = name,
        description = name,
        category = category?.name,
        pictureURL = pictureURL,
        price = price,
        averageRating = averageRating,
        rating = rating.map { it.toRatingDTO() }
    )
}


data class RatingDTO(
    @field:NotBlank(message = "Title is required")
    val title: String ="",
    val body: String = "",

    @field:Min(value = 1, message = "Minimum quantity must be 1")
    @field:Max(value = 5, message = "Maximum quantity must be 5")
    @field:NotNull(message ="Stars number is required")
    val stars: Int? = null,

    val createdDate: Instant?,
)

fun RatingEntity.toRatingDTO() : RatingDTO {
    return RatingDTO(title = title, body = body, stars = stars, createdDate = createdDate)
}