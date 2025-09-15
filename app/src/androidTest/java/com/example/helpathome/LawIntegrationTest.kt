import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LawIntegrationTest {

    @Test
    fun testPanicAlertVisibleToLaw() {
        val db = FirebaseDatabase.getInstance().getReference("Alerts")
        val alert = mapOf("message" to "SOS Panic Alert", "location" to "Zone A")
        val latch = CountDownLatch(1)
        var visible = false

        val ref = db.push()
        ref.setValue(alert).addOnSuccessListener {
            db.child(ref.key!!).get().addOnSuccessListener { snapshot ->
                visible = snapshot.child("message").value == "SOS Panic Alert"
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("Law enforcement could not see panic alert", visible)
    }
}
