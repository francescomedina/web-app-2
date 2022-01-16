package it.polito.wa2.warehouse.domain

import org.apache.commons.lang3.EnumUtils
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.validation.constraints.Min

@Document(collection = "product")
data class ProductEntity (
    @Id
    val id: ObjectId? = ObjectId.get(),

    val name: String,

    val description: String?,
    var pictureURL: String?,
    val category: Category?,

    @Min(0) //Amount greater or equal to 0
    val price: BigDecimal = BigDecimal(0),

    val averageRating: Float = 0F,
    val rating: List<RatingEntity> = emptyList(),

    val createdDate: Instant = Instant.now(),
)


enum class Category(val category: String){
    TELEPHONE("telephone"),
    CLOTHES("clothes"),
    FOOD("food"),
    ELECTRONIC("electronic"),
    HOME("home"),
    SPORT("sport")

}

fun isEnumValid(category: String): Boolean {
    if (!EnumUtils.isValidEnumIgnoreCase(Category::class.java, category)) {
        throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "The category '$category' is not valid. The valid categories are: ${enumValues<Category>().joinToString()}"
        )
    }
    return true
}

fun convertStringToEnum(category: String?): Category? {
    var categoryEnum: Category? = null
    if (category != null && isEnumValid(category)) {
        categoryEnum = enumValueOf<Category>(category.uppercase(Locale.getDefault()))
    }
    return categoryEnum
}