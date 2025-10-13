import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CommunityIntegrationTest {

    @Test
    fun testCommunityHelpRequest() {
        val db = FirebaseDatabase.getInstance().getReference("HelpRequests")
        val request = mapOf("type" to "Shelter", "location" to "Zone B")
        val latch = CountDownLatch(1)
        var stored = false

        val ref = db.push()
        ref.setValue(request).addOnSuccessListener {
            db.child(ref.key!!).get().addOnSuccessListener { snapshot ->
                stored = snapshot.child("type").value == "Shelter"
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("Help request not stored correctly", stored)
    }
}
