# Slutprojekt - <i>Webbaserat Meddelandesystem</i>

Ett Java-baserat meddelandesystem med ett enkelt användargränssnitt som fokuserar på säkerhet och implementerar moderna tekniker för kryptering, autentisering och loggning. Systemet är role-based och erbjuder två användartyper: user och admin. Det erbjuder säker lösenordshantering, kryptering av meddelanden, rate limiting och loggning av viktiga händelser.

## Funktioner
1. **Användarhantering**: Skapa konto, logga in och hantera profil
2. **Meddelandehantering**: Skicka och ta emot meddelanden, med säker kryptering
3. **Admin-funktioner**: Blockera användare, radera meddelanden, ladda ner loggfil

## Säkerhetsåtgärder
- **Lösenordshantering:** PBKDF2 med HMAC-SHA256 och 100 000 iterationer
- **Meddelande kryptering:** AES-GCM för kryptering och autentisering av meddelanden
- **Rate Limiting:** Skyddar alla API-endpoints mot överbelastning
- **JWT-skydd:** Använder HS256 med utgivningsdatum, utgångsdatum och digital signatur
- **Loggning:** Viktiga händelser loggas för att spåra aktivitet i systemet
- **Säker konfiguration:** Känsliga nycklar sparas utanför källkoden

## Systemkrav
- Java 21
- Spring Boot 2.x
- H2 Database (in-memory)

## Installationsguide
#### 1. Klona projektet
Git Bash:<br>
git clone https://github.com/fridastephanie/IT_SAKERHET_JAVA_24_Frida_Hassel_Slutuppgift
#### 2. Bygg och kör programmet
1. Öppna projektet i din IDE (t.ex. IntelliJ IDEA)
2. Konfigurera application.properties om det behövs
3. Kör huvudklassen MessageServerApplication.java för att starta applikationen
#### 3. Kör programmet
När programmet startas kommer en admin, två användare och några meddelanden att initialiseras för testning <br>*(se inloggningsinformation i huvudklassen MessageServerApplication).* <br>För att komma åt applikationen, gå till:
- Registrering: http://localhost:8080/register
- Inloggning: http://localhost:8080/login

## Övrigt
- Utskrifterna i applikationen är på engelska
