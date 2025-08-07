# Usar imagem oficial OpenJDK 21 com JDK completo
FROM openjdk:21-jdk-slim

# Diretório de trabalho dentro do container
WORKDIR /app

# Copiar o jar gerado para dentro do container
COPY target/admissao-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta padrão da aplicação
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
