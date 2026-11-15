package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext

/** Comprueba que repositorio y adapter usan el dispatcher que reciben por constructor. */
@OptIn(ExperimentalCoroutinesApi::class)
class InjectedDispatcherTest {

    /** Dispatcher de prueba que cuenta cuántas veces se le pidió ejecutar trabajo (lo corre en el mismo hilo). */
    private class CountingDispatcher : CoroutineDispatcher() {
        var dispatches = 0
            private set

        override fun isDispatchNeeded(context: CoroutineContext) = true

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatches++
            block.run()
        }
    }

    @Test
    fun `el repositorio ejecuta la llamada en su dispatcher`() = runBlocking {
        val repositoryDispatcher = CountingDispatcher()
        val adapterDispatcher = UnconfinedTestDispatcher()
        val repository = AlbumRepository(
            AlbumServiceAdapter(FakeBackendApi(albums = listOf(albumDto())), adapterDispatcher),
            repositoryDispatcher
        )

        repository.getAlbums()

        assertTrue(repositoryDispatcher.dispatches > 0)
    }

    @Test
    fun `el adapter convierte los DTO en su dispatcher`() = runBlocking {
        val adapterDispatcher = CountingDispatcher()
        val adapter = AlbumServiceAdapter(FakeBackendApi(albums = listOf(albumDto())), adapterDispatcher)

        val albums = adapter.getAlbums()

        assertEquals(1, albums.size)
        assertTrue(adapterDispatcher.dispatches > 0)
    }

    @Test
    fun `una lista servida desde la cache no vuelve a usar el dispatcher`() = runBlocking {
        val repositoryDispatcher = CountingDispatcher()
        val repository = AlbumRepository(
            AlbumServiceAdapter(FakeBackendApi(albums = listOf(albumDto())), UnconfinedTestDispatcher()),
            repositoryDispatcher
        )
        repository.getAlbums()
        val before = repositoryDispatcher.dispatches

        repository.getAlbums()

        assertEquals(before, repositoryDispatcher.dispatches)
    }
}
