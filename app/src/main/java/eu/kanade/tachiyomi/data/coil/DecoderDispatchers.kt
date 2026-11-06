package eu.kanade.tachiyomi.data.coil

import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The two lanes image decoding (and upscaling, which runs inside the decoder) can take.
 *
 * [low] is for pages that are only being prepared ahead of time. Its jobs run a little below normal priority, so the
 * threads that draw the screen win when both are busy, which keeps scrolling smooth. [normal] is for a page the user is
 * waiting for right now, so it is not held back behind the preparation work.
 *
 * The reader setting "Prepare pages at lower priority" is read for every job in [low]; with it off the lane runs at
 * normal priority too.
 */
object DecoderDispatchers {

    /** Tells whether the [low] lane should run below normal priority. Set once at startup. */
    @Volatile
    var lowPriorityEnabled: () -> Boolean = { true }

    val low: CoroutineDispatcher by lazy {
        val pool = pool("coil-decoder")
        Executor { job ->
            pool.execute {
                val lowered = runCatching { lowPriorityEnabled() }.getOrDefault(true)
                Process.setThreadPriority(if (lowered) LOW_PRIORITY else Process.THREAD_PRIORITY_DEFAULT)
                job.run()
            }
        }.asCoroutineDispatcher()
    }

    val normal: CoroutineDispatcher by lazy {
        val pool = pool("coil-decoder-now")
        Executor { job ->
            pool.execute {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
                job.run()
            }
        }.asCoroutineDispatcher()
    }

    private fun pool(name: String) = Executors.newFixedThreadPool(THREADS) { runnable ->
        Thread(runnable, name).apply { isDaemon = true }
    }

    private const val THREADS = 3

    /** Android nice value: 0 is normal, 10 and above is the restricted background group, so 4 stays in between. */
    private const val LOW_PRIORITY = 4
}
