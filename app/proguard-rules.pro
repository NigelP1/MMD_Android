# kotlinx.serialization ships its own R8 rules; Retrofit's interface is kept by Retrofit's rules.

# Credential Manager finds its Play services provider by reflection
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
