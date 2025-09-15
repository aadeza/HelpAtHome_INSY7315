import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdminIntegrationTest {

    @Test
    fun testAdminSuspendUser() {
        val db = FirebaseDatabase.getInstance().getReference("Users")
        val userId = "nonExistentUser123" // Bug: ID does not exist
        val latch = CountDownLatch(1)
        var suspended = false

        db.child(userId).child("status").setValue("suspended").addOnSuccessListener {
            db.child(userId).get().addOnSuccessListener { snapshot ->
                suspended = snapshot.child("status").value == "suspended"
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("Admin failed to suspend user", suspended)
    }
}
