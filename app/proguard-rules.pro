# R8: ofusca y reduce el código para dificultar que se quite la licencia.
# Las clases del manifiesto (Activity y servicios) ya se conservan solas.
# ML Kit trae sus propias reglas.

# Quita los nombres de archivo y líneas de los errores.
-renamesourcefileattribute x

# Credential Manager (inicio de sesión con Google): la implementación se carga por reflexión.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
