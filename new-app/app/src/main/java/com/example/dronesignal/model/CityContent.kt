package com.example.dronesignal.model

/** City-scoped data contracts. These contain no UI assumptions or mock operational data. */
data class City(val id: String, val name: String)

data class Announcement(
    val id: String,
    val cityIds: Set<String>,
    val title: String,
    val description: String,
    val thumbnailUrl: String? = null,
    val actionUrl: String? = null,
    val phone: String? = null,
    val publishedAt: String? = null,
    val expiresAt: String? = null
) {
    init { require(actionUrl == null || phone == null) { "Only one primary action is allowed" } }
}

enum class AlertKind { DRONE, MISSILE, MASS_ATTACK, CLEAR }

data class SafetyAlert(
    val id: String,
    val cityIds: Set<String>,
    val kind: AlertKind,
    val message: String,
    val source: String,
    val publishedAt: String,
    val active: Boolean
)

enum class UtilityKind { ELECTRICITY, WATER }
enum class OutageStatus { PLANNED, ACTIVE, RESTORED, UNKNOWN }

data class CityOutage(
    val id: String,
    val cityId: String,
    val kind: UtilityKind,
    val status: OutageStatus,
    val addresses: List<String>,
    val startsAt: String? = null,
    val expectedRestoreAt: String? = null,
    val source: String,
    val sourceUrl: String? = null
)

/** Empty cityIds denotes a global announcement/alert, not an unknown city. */
fun Announcement.visibleIn(cityId: String): Boolean = cityIds.isEmpty() || cityId in cityIds
fun SafetyAlert.visibleIn(cityId: String): Boolean = cityIds.isEmpty() || cityId in cityIds
fun CityOutage.visibleIn(cityId: String): Boolean = this.cityId == cityId
