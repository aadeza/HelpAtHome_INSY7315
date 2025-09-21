import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HomeIntegrationTest {

    @Test
    fun testHomeResourcesLoad() {
        val db = FirebaseDatabase.getInstance().getReference("Resources")

        val resource = mapOf("title" to "Emergency Numbers", "content" to "10111, 10177, 112")
        val latch = CountDownLatch(1)
        var found = false

        val ref = db.push()
        ref.setValue(resource).addOnSuccessListener {
            db.child(ref.key!!).get().addOnSuccessListener { snapshot ->
                found = snapshot.child("title").value == "Emergency Numbers"
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("Resource not found in Firebase", found)
    }
}
