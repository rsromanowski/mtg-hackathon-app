package io.github.rsromanowski.scryfall

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.UUID

interface MagicCardClient {
    suspend fun getMkmCards()
}

class ScryfallClient(
    private val log : Logger
) : MagicCardClient {
    private val client = HttpClient(CIO) {
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val BASE_URL = "https://api.scryfall.com"

    override suspend fun getMkmCards() {
        val baseCards: ListResponse<ScryfallCard> = search {
            url {
                parameters.append("q", "e:mkm cn≥1 cn≤286")
                parameters.append("unique", "prints")
            }
        }.body()

        val listCards: ListResponse<ScryfallCard> = search {
            url {
                parameters.append(
                    "q",
                    "e:plst (((cn≥ cn≤) OR cn:\"APC-117\" OR cn:\"MH1-21\" OR cn:\"DIS-33\" OR cn:\"XLN-91\" OR cn:\"C16-47\" OR cn:\"SOM-96\" OR cn:\"STX-64\" OR cn:\"MH2-191\" OR cn:\"ISD-183\" OR cn:\"DKA-143\" OR cn:\"DST-40\" OR cn:\"MRD-99\" OR cn:\"ELD-107\" OR cn:\"DKA-4\" OR cn:\"M20-167\" OR cn:\"RTR-140\" OR cn:\"ONS-89\" OR cn:\"WAR-54\" OR cn:\"DOM-130\" OR cn:\"HOU-149\" OR cn:\"MBS-10\" OR cn:\"RAV-277\" OR cn:\"2X2-17\" OR cn:\"STX-220\" OR cn:\"M14-213\" OR cn:\"KLD-221\" OR cn:\"ARB-68\" OR cn:\"JOU-153\" OR cn:\"RNA-182\" OR cn:\"C21-19\" OR cn:\"UMA-138\" OR cn:\"MH2-46\" OR cn:\"VOW-207\" OR cn:\"ONS-272\" OR cn:\"UMA-247\" OR cn:\"SOM-98\" OR cn:\"DDU-50\" OR cn:\"CLB-85\" OR cn:\"DIS-173\" OR cn:\"SOI-262\"))"
                )
                parameters.append("unique", "prints")
            }
        }.body()

        log.info("Base cards: ${baseCards.totalCards} - ${baseCards.data.shuffled().first()}")
        // log.info("List cards: ${listCards.totalCards} - ${listCards.data.first()}")
    }

    private suspend fun search(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.get("$BASE_URL/cards/search", block)
    }
}

/**
 * @param id A unique ID for this card in Scryfall’s database.
 */
@Serializable
data class ScryfallCard(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val layout: String, // A code for this card’s layout.
    @SerialName("card_faces")
    val cardFaces: List<CardFace>? = null, // An array of Card Face objects, if this card is multifaced.
    val cmc: Float, // The card’s mana value. Note that some funny cards have fractional mana costs.
    @SerialName("color_identity")
    val colorIdentity: Set<Color>,
    val colors: Set<Color>, // This card’s colors, if the overall card has colors defined by the rules. Otherwise the colors will be on the card_faces objects, see below.
    @SerialName("mana_cost")
    val manaCost: String, //  Nullable 	The mana cost for this card. This value will be any empty string "" if the cost is absent. Remember that per the game rules, a missing mana cost and a mana cost of {0} are different values. Multi-faced cards will report this value in card faces.
    val name: String, // The name of this card. If this card has multiple faces, this field will contain both names separated by ␣//␣.
    @SerialName("image_uris")
    val imageUris: ImageUris? = null, // An object listing available imagery for this card. See the Card Imagery article for more information.
    val rarity:	String, // This card’s rarity. One of common, uncommon, rare, special, mythic, or bonus.
    val preview: PreviewMetadata? = null,
)
@Serializable
data class CardFace(
    @SerialName("mana_cost")
    val manaCost: String,
    val name: String,
    @SerialName("oracle_text")
    val oracleText: String,
    @SerialName("type_line")
    val typeLine: String
)
enum class Color {
    W, U, B, R, G
}
enum class Rarity {
    common, uncommon, rare, special, mythic, bonus
}

@Serializable
data class PreviewMetadata(
    @Serializable(with = LocalDateIso8601Serializer::class)
    @SerialName("previewed_at")
    val previewedAt: LocalDate, // Date 	Nullable 	The date this card was previewed. "2024-02-29"
)

@Serializable
data class ListResponse<T>(
    @SerialName("total_cards")
    val totalCards: Int,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("next_page")
    val nextPage: String? = null,
    val data: List<T>,
)

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: UUID) { encoder.encodeString(value.toString()) }
}

@Serializable
data class ImageUris(
    val png: String, // 745 × 1040 PNG A transparent, rounded full card PNG. This is the best image to use for videos or other high-quality content.
    @SerialName("border_crop")
    val borderCrop: String, // 480 × 680 JPG A full card image with the rounded corners and the majority of the border cropped off. Designed for dated contexts where rounded images can’t be used.
    @SerialName("art_crop")
    val artCrop: String, // Varies JPG A rectangular crop of the card’s art only. Not guaranteed to be perfect for cards with outlier designs or strange frame arrangements
    val large: String, // JPG A large full card image
    val normal: String, // 488 × 680 JPG A medium-sized full card image
    val small: String, // 146 × 204 JPG A small full card image. Designed for use as thumbnail or list icon.
)