package org.kodein.db.impl.model

import org.kodein.db.Value
import org.kodein.db.model.Key
import org.kodein.db.model.Metadata
import org.kodein.db.react.DBListener
import org.kodein.memory.Closeable
import org.kodein.memory.use
import org.kodein.memory.util.getShadowed
import kotlin.test.*

@Suppress("ClassName")
class ModelDBTests_06_React : ModelDBTests() {

    @Test
    fun test00_PutDelete() {

        val me = Adult("Salomon", "BRYS", Date(15, 12, 1986))

        var setSubscriptionCalls = 0
        var willPutCalls = 0
        var didPutCalls = 0
        var willDeleteCalls = 0
        var didDeleteCalls = 0

        val listener = object : DBListener {
            override fun setSubscription(subscription: Closeable) {
                ++setSubscriptionCalls
            }

            override fun willPut(model: Any, typeName: String, metadata: Metadata) {
                assertSame(me, model)
                assertEquals("Adult", typeName)
                assertEquals(me.primaryKey, metadata.primaryKey)
                assertEquals(me.indexes, metadata.indexes)
                ++willPutCalls
            }

            override fun didPut(model: Any, typeName: String, metadata: Metadata) {
                assertSame(me, model)
                assertEquals("Adult", typeName)
                assertEquals(me.primaryKey, metadata.primaryKey)
                assertEquals(me.indexes, metadata.indexes)
                ++didPutCalls
            }

            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) {
                assertEquals(mdb.getHeapKey(me), key)
                assertEquals("Adult", typeName)
                val model = getModel()
                assertNotSame(me, model)
                assertEquals(me, model)
                assertSame(model, getModel())
                ++willDeleteCalls
            }

            override fun didDelete(key: Key<*>, typeName: String) {
                assertEquals(mdb.getHeapKey(me), key)
                assertEquals("Adult", typeName)
                ++didDeleteCalls
            }
        }

        mdb.register(listener)
        mdb.register(listener)

        assertEquals(1, setSubscriptionCalls)
        assertEquals(0, willPutCalls)
        assertEquals(0, didPutCalls)
        assertEquals(0, willDeleteCalls)
        assertEquals(0, didDeleteCalls)

        val key = mdb.putAndGetHeapKey(me).value

        assertEquals(1, setSubscriptionCalls)
        assertEquals(1, willPutCalls)
        assertEquals(1, didPutCalls)
        assertEquals(0, willDeleteCalls)
        assertEquals(0, didDeleteCalls)

        mdb.delete(key)

        assertEquals(1, setSubscriptionCalls)
        assertEquals(1, willPutCalls)
        assertEquals(1, didPutCalls)
        assertEquals(1, willDeleteCalls)
        assertEquals(1, didDeleteCalls)
    }

    @Test
    fun test01_Batch() {

        val me = Adult("Salomon", "BRYS", Date(15, 12, 1986))
        val her = Adult("Laila", "ATIE", Date(25, 8, 1989))

        var setSubscriptionCalls = 0
        var willPutCalls = 0
        var didPutCalls = 0
        var willDeleteCalls = 0
        var didDeleteCalls = 0

        val listener = object : DBListener {
            override fun setSubscription(subscription: Closeable) {
                ++setSubscriptionCalls
            }

            override fun willPut(model: Any, typeName: String, metadata: Metadata) {
                if (willPutCalls == 0)
                    assertSame(me, model)
                else
                    assertSame(her, model)
                ++willPutCalls
            }

            override fun didPut(model: Any, typeName: String, metadata: Metadata) {
                if (didPutCalls == 0)
                    assertSame(me, model)
                else
                    assertSame(her, model)
                ++didPutCalls
            }

            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) {
                if (willDeleteCalls == 0)
                    assertEquals(me, getModel())
                else
                    assertEquals(her, getModel())
                ++willDeleteCalls
            }

            override fun didDelete(key: Key<*>, typeName: String) {
                if (didDeleteCalls == 0)
                    assertEquals(mdb.getHeapKey(me), key)
                else
                    assertEquals(mdb.getHeapKey(her), key)
                ++didDeleteCalls
            }
        }

        mdb.register(listener)

        assertEquals(1, setSubscriptionCalls)
        assertEquals(0, willPutCalls)
        assertEquals(0, didPutCalls)
        assertEquals(0, willDeleteCalls)
        assertEquals(0, didDeleteCalls)

        lateinit var meKey: Key<Adult>
        lateinit var herKey: Key<Adult>

        mdb.newBatch().use {
            meKey = it.putAndGetHeapKey(me).value
            herKey = it.putAndGetHeapKey(her).value

            assertEquals(1, setSubscriptionCalls)
            assertEquals(2, willPutCalls)
            assertEquals(0, didPutCalls)
            assertEquals(0, willDeleteCalls)
            assertEquals(0, didDeleteCalls)

            it.write()
        }

        assertEquals(1, setSubscriptionCalls)
        assertEquals(2, willPutCalls)
        assertEquals(2, didPutCalls)
        assertEquals(0, willDeleteCalls)
        assertEquals(0, didDeleteCalls)

        mdb.newBatch().use {
            it.delete(meKey)
            it.delete(herKey)

            assertEquals(1, setSubscriptionCalls)
            assertEquals(2, willPutCalls)
            assertEquals(2, didPutCalls)
            assertEquals(2, willDeleteCalls)
            assertEquals(0, didDeleteCalls)

            it.write()
        }

        assertEquals(1, setSubscriptionCalls)
        assertEquals(2, willPutCalls)
        assertEquals(2, didPutCalls)
        assertEquals(2, willDeleteCalls)
        assertEquals(2, didDeleteCalls)
    }

    @Test
    fun test02_SubscriptionClosed() {

        var willPutCalled = false
        var willDeleteCalled = false

        val putListener = object : DBListener {
            private lateinit var sub: Closeable
            override fun setSubscription(subscription: Closeable) { sub = subscription }
            override fun willPut(model: Any, typeName: String, metadata: Metadata) {
                sub.close()
                willPutCalled = true
            }
            override fun didPut(model: Any, typeName: String, metadata: Metadata) = fail("didPut")
            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) = fail("willDeltete")
            override fun didDelete(key: Key<*>, typeName: String) = fail("didDelete")
        }

        val deleteListener = object : DBListener {
            private lateinit var sub: Closeable
            override fun setSubscription(subscription: Closeable) { sub = subscription }
            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) {
                sub.close()
                willDeleteCalled = true
            }
            override fun didDelete(key: Key<*>, typeName: String) = fail("didDelete")
        }

        mdb.register(putListener)
        mdb.register(deleteListener)

        assertFalse(willPutCalled)
        assertFalse(willDeleteCalled)

        val key = mdb.putAndGetHeapKey(Adult("Salomon", "BRYS", Date(15, 12, 1986))).value

        assertTrue(willPutCalled)
        assertFalse(willDeleteCalled)

        mdb.delete(key)

        assertTrue(willDeleteCalled)
    }

    @Test
    fun test03_WillOpExceptions() {

        var firstWillPutCalled = false
        var firstDidPutCalled = false
        var secondDidPutCalled = false
        var thirdWillPutCalled = false
        var thirdDidPutCalled = false

        var firstWillDeleteCalled = false
        var firstDidDeleteCalled = false
        var secondDidDeleteCalled = false
        var thirdWillDeleteCalled = false
        var thirdDidDeleteCalled = false

        val firstListener = object : DBListener {
            override fun willPut(model: Any, typeName: String, metadata: Metadata) { firstWillPutCalled = true }
            override fun didPut(model: Any, typeName: String, metadata: Metadata) { firstDidPutCalled = true }
            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) { firstWillDeleteCalled = true }
            override fun didDelete(key: Key<*>, typeName: String) { firstDidDeleteCalled = true }
        }

        val secondListener = object : DBListener {
            override fun willPut(model: Any, typeName: String, metadata: Metadata) = throw IllegalStateException("willPut")
            override fun didPut(model: Any, typeName: String, metadata: Metadata) { secondDidPutCalled = true }
            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) = throw IllegalStateException("willDelete")
            override fun didDelete(key: Key<*>, typeName: String) { secondDidDeleteCalled = true }
        }

        val thirdListener = object : DBListener {
            override fun willPut(model: Any, typeName: String, metadata: Metadata) { thirdWillPutCalled = true }
            override fun didPut(model: Any, typeName: String, metadata: Metadata) { thirdDidPutCalled = true }
            override fun willDelete(key: Key<*>, typeName: String, getModel: () -> Any?) { thirdWillDeleteCalled = true }
            override fun didDelete(key: Key<*>, typeName: String) { thirdDidDeleteCalled = true }
        }

        mdb.register(firstListener)
        mdb.register(secondListener)
        mdb.register(thirdListener)

        val me = Adult("Salomon", "BRYS", Date(15, 12, 1986))
        val key = mdb.getHeapKey(me)

        val putEx = assertFailsWith<IllegalStateException>("willPut") {
            mdb.put(me)
        }
        assertNull(putEx.cause)
        assertEquals(emptyList(), putEx.getShadowed())

        assertNull(mdb[key])

        assertTrue(firstWillPutCalled)
        assertFalse(firstDidPutCalled)
        assertFalse(secondDidPutCalled)
        assertFalse(thirdWillPutCalled)
        assertFalse(thirdDidPutCalled)

        val deleteEx = assertFailsWith<IllegalStateException>("willDelete") {
            mdb.delete(key)
        }
        assertNull(deleteEx.cause)
        assertEquals(emptyList(), deleteEx.getShadowed())

        assertTrue(firstWillDeleteCalled)
        assertFalse(firstDidDeleteCalled)
        assertFalse(secondDidDeleteCalled)
        assertFalse(thirdWillDeleteCalled)
        assertFalse(thirdDidDeleteCalled)
    }

    @Test
    fun test03_DidOpExceptions() {

        val firstListener = object : DBListener {
            override fun didPut(model: Any, typeName: String, metadata: Metadata) = throw IllegalStateException("first didPut")
            override fun didDelete(key: Key<*>, typeName: String) = throw IllegalStateException("first didDelete")
        }

        val secondListener = object : DBListener {
            override fun didPut(model: Any, typeName: String, metadata: Metadata) = throw IllegalStateException("second didPut")
            override fun didDelete(key: Key<*>, typeName: String) = throw IllegalStateException("second didDelete")
        }

        mdb.register(firstListener)
        mdb.register(secondListener)

        val me = Adult("Salomon", "BRYS", Date(15, 12, 1986))
        val key = mdb.getHeapKey(me)

        val putEx = assertFailsWith<IllegalStateException>("first didPut") {
            mdb.put(me)
        }
        assertNull(putEx.cause)
        assertEquals(1, putEx.getShadowed().size)
        assertEquals("second didPut", (putEx.getShadowed()[0] as IllegalStateException).message)

        assertNotNull(mdb[key])

        val deleteEx = assertFailsWith<IllegalStateException>("first willDelete") {
            mdb.delete(key)
        }
        assertNull(deleteEx.cause)
        assertEquals(1, deleteEx.getShadowed().size)
        assertEquals("second didDelete", (deleteEx.getShadowed()[0] as IllegalStateException).message)

        assertNull(mdb[key])
    }

}