FROM eclipse-temurin:21-jre-alpine

# Copie le fichier compilé Main.class dans l'image
COPY Main.class /app/

WORKDIR /app

# Commande pour exécuter le programme Java
CMD ["java", "Main"]