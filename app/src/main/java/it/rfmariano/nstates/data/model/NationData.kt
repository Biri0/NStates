package it.rfmariano.nstates.data.model

data class NationData(
    val name: String = "",
    val fullName: String = "",
    val type: String = "",
    val motto: String = "",
    val category: String = "",
    val region: String = "",
    val flagUrl: String = "",
    val population: Long = 0,
    val currency: String = "",
    val animal: String = "",
    val leader: String = "",
    val capital: String = "",
    val founded: String = "",
    val lastActivity: String = "",
    val influence: String = "",
    val tax: Double = 0.0,
    val gdp: Long = 0,
    val income: Long = 0,
    val poorest: Long = 0,
    val richest: Long = 0,
    val majorIndustry: String = "",
    val crime: String = "",
    val sensibilities: String = "",
    val govtDescription: String = "",
    val industryDescription: String = "",
    val waStatus: String = "",
    val endorsements: String = "",
    val freedom: Freedom = Freedom(),
    val government: Government = Government(),
    val deaths: Deaths = Deaths()
)

data class Freedom(
    val civilRights: String = "",
    val economy: String = "",
    val politicalFreedom: String = ""
)

data class Government(
    val administration: Double = 0.0,
    val defence: Double = 0.0,
    val education: Double = 0.0,
    val environment: Double = 0.0,
    val healthcare: Double = 0.0,
    val commerce: Double = 0.0,
    val internationalAid: Double = 0.0,
    val lawAndOrder: Double = 0.0,
    val publicTransport: Double = 0.0,
    val socialEquality: Double = 0.0,
    val spirituality: Double = 0.0,
    val welfare: Double = 0.0
)

data class Deaths(
    val causes: List<DeathCause> = emptyList()
)

data class DeathCause(
    val type: String = "",
    val percentage: Double = 0.0
)
