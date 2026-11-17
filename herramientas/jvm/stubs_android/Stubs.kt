// Stubs de tipos (NO funcionales) para comprobar que TrazaAndroid.kt compila sin el SDK de Android.
// No sustituyen la compilación real de play-compilar.yml.
package android.content
open class Context { open val applicationContext: Context get() = this; open val filesDir: java.io.File get() = java.io.File(".") }
