package com.example.helpathome


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
) {

    fun login(
        email: String,
        password: String,
        onSuccess: (userId: String, userType: String, firstName: String, lastName: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        database.child(userId).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val userType = snapshot.child("userType").getValue(String::class.java) ?: ""
                                    val fName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                                    val lName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                                    onSuccess(userId, userType, fName, lName)
                                } else {
                                    onError("User data not found in database!")
                                }
                            }
                            .addOnFailureListener { e ->
                                onError("Database error: ${e.message}")
                            }
                    } else {
                        onError("User ID not found!")
                    }
                } else {
                    onError("Login failed: ${task.exception?.message}")
                }
            }
    }
}
