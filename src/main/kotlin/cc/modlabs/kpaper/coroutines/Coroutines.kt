package cc.modlabs.kpaper.coroutines

import cc.modlabs.kpaper.main.PluginInstance
import org.bukkit.Bukkit
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

fun sync(block: () -> Unit) {
    Bukkit.getGlobalRegionScheduler().execute(PluginInstance, block)
}

fun async(block: () -> Unit) {
    Bukkit.getAsyncScheduler().runNow(PluginInstance) { block() }
}

/**
 * mcroutine guarantees execution on the server thread.
 * True blocking is illegal, see [mcasync] for options.
 */
fun <T> mcroutine(coroutine: suspend () -> T) {
    KPaperCoroutines.scope.launch(Dispatchers.mc) {
        coroutine()
    }
}

/**
 * mcasync guarantees execution away from the server thread.
 * True blocking is OK, mcasync uses an Unbound thread-pool, so it's
 * not an option for CPU bound tasks.
 */
suspend fun <T> mcasync(coroutine: suspend () -> T): T {
    return withContext(Dispatchers.async) {
        try {
            coroutine()
        } catch (e: Exception) {
            throw e
        }
    }
}

fun <T> mcasyncBlocking(coroutine: suspend () -> T) {
    KPaperCoroutines.scope.launch(Dispatchers.async) {
        coroutine()
    }
}


private object KPaperCoroutines {
    private val threadNumber = AtomicInteger()
    private var job = SupervisorJob()
    private var pool: ExecutorService = createPool()

    val scope: CoroutineScope
        get() = CoroutineScope(job + CoroutineExceptionHandler { _, error ->
            val plugin = runCatching { PluginInstance }.getOrNull()
            if (plugin != null) {
                plugin.logger.severe("Uncaught KPaper coroutine failure: ${error.message}")
                error.printStackTrace()
            }
        })

    fun executor(): ExecutorService = synchronized(this) {
        if (pool.isShutdown) pool = createPool()
        if (!job.isActive) job = SupervisorJob()
        pool
    }

    fun shutdown() = synchronized(this) {
        job.cancel(CancellationException("KPaper plugin disabled"))
        pool.shutdownNow()
    }

    private fun createPool(): ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kpaper-async-${threadNumber.incrementAndGet()}").apply { isDaemon = true }
    }
}

@Deprecated("Use the KPaper coroutine helpers; this executor is lifecycle-managed and must not be shut down by callers")
val globalPool: ExecutorService
    get() = KPaperCoroutines.executor()

fun initializeCoroutines() {
    KPaperCoroutines.executor()
}

fun shutdownCoroutines() {
    KPaperCoroutines.shutdown()
}

object MinecraftCoroutineDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!Bukkit.isGlobalTickThread()) {
            Bukkit.getGlobalRegionScheduler().execute(PluginInstance, block)
            return
        }
        block.run()
    }
}

object AsyncCoroutineDispatcher : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        !Thread.currentThread().name.startsWith("kpaper-async-")

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        KPaperCoroutines.executor().execute(block)
    }
}

val Dispatchers.async: CoroutineContext
    get() = AsyncCoroutineDispatcher

val Dispatchers.mc: CoroutineContext
    get() = MinecraftCoroutineDispatcher
