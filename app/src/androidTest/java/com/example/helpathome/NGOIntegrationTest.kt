import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NGOIntegrationTest {

    @Test
    fun testNGOUpdatePost() {
        val db = FirebaseDatabase.getInstance().getReference("NGOUpdates")
        val update = mapOf("message" to "", "timestamp" to System.currentTimeMillis()) // Bug: empty message
        val latch = CountDownLatch(1)
        var saved = false

        val ref = db.push()
        ref.setValue(update).addOnSuccessListener {
            db.child(ref.key!!).get().addOnSuccessListener { snapshot ->
                saved = snapshot.child("message").value.toString().isNotEmpty()
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("NGO update with empty message should not be valid", saved)
    }
}
