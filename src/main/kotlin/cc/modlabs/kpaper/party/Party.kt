package cc.modlabs.kpaper.party

/**
 * Central access point for the party system in KPaper.
 *
 * Projects can replace [api] at runtime with their own implementation
 * (e.g. a Redis-backed remote API). By default [DefaultPartyAPI] is used,
 * which is in-memory and non-persistent.
 */
object Party {
    @Volatile
    var api: PartyAPI = DefaultPartyAPI()
}
