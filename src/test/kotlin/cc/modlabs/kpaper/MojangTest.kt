package cc.modlabs.kpaper

import cc.modlabs.kpaper.utils.MojangAPI
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MojangTest: FunSpec() {

    init {
        coroutineTestScope = true
        test("testMojangAPI") {
            val api = MojangAPI()
            val uuid = api.getUser("Notch")
            println("UUID: $uuid")

            val username = api.getUser(uuid!!.id)

            username?.username shouldBe "Notch"
            println("Username: $username")

            val fail = api.getUser("NotchNotch2348723467823467887462387")
            fail shouldBe null

            println("Fail: $fail")
        }
    }
}