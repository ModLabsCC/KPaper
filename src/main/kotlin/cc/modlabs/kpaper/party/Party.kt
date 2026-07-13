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

    fun replace(api: PartyAPI, closePrevious: Boolean = true) {
        val previous = this.api
        this.api = api
        if (closePrevious && previous !== api) (previous as? AutoCloseable)?.close()
    }

    fun close() {
        (api as? AutoCloseable)?.close()
        api = DefaultPartyAPI()
    }
}
