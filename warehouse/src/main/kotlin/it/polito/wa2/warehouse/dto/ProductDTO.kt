package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.ProductEntity
import it.polito.wa2.warehouse.domain.RatingEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.xml.bind.annotation.XmlEnumValue


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
    val rating: List<RatingEntity> = emptyList(),
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
    val rating: List<RatingEntity> = emptyList(),
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
        rating = rating
    )
}