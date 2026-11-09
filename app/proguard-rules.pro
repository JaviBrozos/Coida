# Reglas de ProGuard/R8 para la build de release.
# Compose y AndroidX ya incluyen sus propias reglas vía consumer rules,
# así que de momento aquí solo hace falta lo específico de Coida.

# Mantiene los nombres de fichero y líneas en los stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
