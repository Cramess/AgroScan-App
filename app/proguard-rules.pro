# Reglas de R8 para el build release de AgroScan.

# --- Gson: modelos que se (de)serializan por reflexión ---
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Respuestas de OpenWeather y Roboflow, y payloads de la plataforma web
-keep class com.tecsup.agroscan.Services.** { *; }
-keep class com.tecsup.agroscan.network.** { *; }
# Cuentas locales guardadas como JSON en SharedPreferences
-keep class com.tecsup.agroscan.viewmodel.CuentaLocal { *; }
# Vértices de las parcelas guardados como JSON en Room
-keep class com.google.android.gms.maps.model.LatLng { *; }

# --- Retrofit con funciones suspend (R8 en modo completo) ---
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- LiteRT (TFLite): clases usadas desde código nativo ---
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# --- Glance: las acciones del widget se instancian por reflexión ---
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
