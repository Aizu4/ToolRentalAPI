# Sprawozdanie — Tool Rental

**Przedmiot:** Zaawansowane programowanie aplikacji bazodanowych
**Autorzy:** Oskar Dybowski, Franciszek Szczepaniak
**Repozytorium:** `tool_rental_web` (frontend) + `tool_rental_api` (backend)

---

## 1. Opis projektu i założenia

Aplikacja **Tool Rental** to webowy system wypożyczalni narzędzi budowlanych.
Niezalogowany użytkownik może przeglądać katalog narzędzi. Po założeniu konta
klient może wypożyczać narzędzia i zarządzać historią swoich wypożyczeń.
Administrator zarządza katalogiem, użytkownikami oraz wszystkimi wypożyczeniami
w systemie i ma wgląd w statystyki.

### Stos technologiczny

| Warstwa     | Technologia |
|-------------|-------------|
| Backend     | Java 26, Spring Boot 4.0.6, Spring Security, Spring Data JPA, Hibernate, QueryDSL 5.1, Lombok, JWT (jjwt 0.12), BCrypt, Datafaker (seeding) |
| Frontend    | React 19.2 + TypeScript 6, Vite 8, React Router 7.16, Axios 1.16 |
| Framework CSS / UI | **Ant Design 6.4** + `@ant-design/icons` 6.2 + **Tailwind CSS 4.3** |
| Wykresy     | `@ant-design/charts` 2.6 |
| Baza danych | **PostgreSQL** (`jdbc:postgresql://localhost:5432/tool_rental`) |

### Role aplikacyjne

System rozróżnia trzy role (`Role` enum: `ADMIN`, `CUSTOMER`, `SUSPENDED`).
Dwie główne role to **Administrator** i **Klient (Customer)**; `SUSPENDED`
jest dodatkowym stanem zablokowanego konta. Spring Security ma skonfigurowaną
hierarchię ról (`ROLE_ADMIN > ROLE_CUSTOMER`), więc administrator
dziedziczy wszystkie uprawnienia klienta i może dodatkowo korzystać
z funkcji administracyjnych.

| Funkcjonalność                                | Klient | Admin |
|-----------------------------------------------|:------:|:-----:|
| Rejestracja, logowanie                        | ✓      | ✓     |
| Przeglądanie katalogu narzędzi                | ✓      | ✓     |
| Wypożyczenie narzędzia                        | ✓      | ✓     |
| Anulowanie własnego wypożyczenia (PENDING)    | ✓      | ✓     |
| Historia własnych wypożyczeń                  | ✓      | ✓     |
| Edycja własnego profilu                       | ✓      | ✓     |
| Zarządzanie katalogiem (CRUD)                 | —      | ✓     |
| Lista i edycja wszystkich użytkowników        | —      | ✓     |
| Zawieszanie / odwieszanie kont                | —      | ✓     |
| Lista wszystkich wypożyczeń + zmiana statusu  | —      | ✓     |
| Statystyki (wykresy)                          | —      | ✓     |
| Reset bazy danych z danymi przykładowymi      | —      | ✓     |

## 2. Przypadki użycia (UML)

Diagramy w osobnych plikach (Mermaid):

- [`use-cases/use-case-admin.md`](use-cases/use-case-admin.md) — administrator
- [`use-cases/use-case-customer.md`](use-cases/use-case-customer.md) — klient
- [`use-cases/use-case-guest.md`](use-cases/use-case-guest.md) — gość (niezalogowany)

## 3. Diagram sekwencji

[`sequence-rental.md`](sequence-rental.md) — sekwencja
wypożyczenia narzędzia przez klienta (od kliknięcia w UI, przez filtr JWT,
kontroler i repozytorium, aż po zapis w bazie).

## 4. Struktura bazy danych

[`er-diagram.md`](er-diagram.md) — diagram ER z relacjami,
kluczami obcymi i ograniczeniami. Cztery tabele: `user`, `item`, `item_rental`,
`item_rental_update`. Adres klienta jest osadzony (`@Embeddable`) w wierszu
tabeli `user`. Pole `available_amount` narzędzia jest wyliczane formułą
Hibernate, a nie przechowywane w kolumnie.

## 5. Skrypt bazy danych

W katalogu `tool_rental_api/src/main/resources/db/` znajduje się skrypt
`schema.sql` — `CREATE TABLE` dla wszystkich tabel z ograniczeniami
(PK, FK, NOT NULL, UNIQUE, CHECK, indeksy), w składni PostgreSQL.
Hibernate normalnie zarządza schematem przez `ddl-auto=update`, ale
skrypt jest dostępny jako referencja i do ręcznej inicjalizacji bazy.

Dane przykładowe nie są dostarczane jako statyczny plik SQL — zamiast
tego wstrzykuje je `SeedingService` (opisany niżej), wywoływany z poziomu
aplikacji przez administratora lub automatycznie przy pierwszym starcie
pustej bazy. Pozwala to uzyskać znacznie bogatszy zestaw danych (300
narzędzi, 240 wypożyczeń z realistyczną historią statusów) niż dałoby
się wygodnie zapisać w skrypcie SQL.

### Reset bazy z poziomu aplikacji

Administrator może odtworzyć całą bazę z danymi przykładowymi
z poziomu aplikacji. Realizuje to:

- Endpoint **`POST /admin/db/reset`** (chroniony `@AdminOnly`) — wywołuje
  `SeedingService.wipeAndReseed()`, który przez Hibernate `SchemaManager`
  najpierw dropuje (`dropMappedObjects(true)`) a potem ponownie tworzy
  (`exportMappedObjects(true)`) wszystkie tabele i sekwencje, a następnie
  wywołuje `seedAll()` z domyślną konfiguracją: **5 użytkowników**
  (1 admin + 3 klientów + 1 zawieszony), **300 narzędzi** i
  **240 wypożyczeń** (80 na klienta) z losowymi datami z ostatniego roku
  — żeby wykresy miały sens. Każde wypożyczenie ma też wygenerowaną
  historię statusów (`ItemRentalUpdate`) symulującą realny cykl życia
  (PENDING → SENT → DELIVERED → RETURNED/LOST, lub PENDING → CANCELLED).
- Przycisk **„Reset DB with sample data"** na stronie `/admin/statistics` w
  panelu administratora (z potwierdzeniem `Popconfirm`).

## 6. Moduły aplikacji

### 6.1 Moduł logowania

- `POST /auth/register` — rejestracja klienta.
- `POST /auth/login` — zwraca JWT (ważny 24h, sekret w `app.jwt.secret`).
- Frontend: `LoginPage`, `RegisterPage`. Token przechowywany w
  `tool_rental_web/src/api/tokenStore.ts` (`localStorage`) i automatycznie
  dołączany przez interceptor Axios w
  `tool_rental_web/src/api/client.ts` (`Authorization: Bearer <JWT>`).
- Backend: `JwtAuthFilter` (`src/main/java/.../filters/JwtAuthFilter.java`)
  dekoduje token, `UserDetailsServiceImpl` ładuje użytkownika z bazy,
  `SecurityConfig` rejestruje filtr w łańcuchu Spring Security i włącza
  bezstanową obsługę sesji.

### 6.2 Moduły dla ról

Adnotacje `@AdminOnly`, `@CustomerOnly`, `@RequireAuth` opakowują Spring
Security `@PreAuthorize`. We frontendzie ten sam podział realizuje komponent
`ProtectedRoute` z `AuthContext`, a `MenuSider` ukrywa nieosiągalne pozycje.

### 6.3 Wybieranie, sortowanie i przeglądanie danych

Komponent `PagedTable` (`tool_rental_web/src/components/table/PagedTable.tsx`)
łączy `Ant Table` z `Input.Search` i wewnętrznie obsługuje paginację, sortowanie
(z `SorterResult` z `antd`) oraz wyszukiwanie po frazie. Każda zmiana w UI jest
zamieniana na parametry zapytania `page`, `size`, `sort`, `s` i wysyłana do
backendu. Spring wystawia te parametry na endpointach listujących
(`GET /items`, `GET /rentals`, `GET /users`); filtrowanie po frazie tekstowej
jest realizowane przez predykaty QueryDSL łączone `or`-em po wybranych polach
(np. nazwa producenta, model narzędzia, dane użytkownika).

### 6.4 Moduł statystyk (graficzny, czasowy)

Strona `/admin/statistics` (`tool_rental_web/src/pages/admin/StatisticsPage.tsx`)
prezentuje dwa wykresy zbudowane na `@ant-design/charts`:

- **Wypożyczenia w miesiącu** (`Column`, stack po statusie) — liczba
  zakończonych wypożyczeń pogrupowana po miesiącu i statusie końcowym
  (`RETURNED`, `CANCELLED`, `LOST`), domyślnie ostatnie 12 miesięcy.
  Słupki są stackowane i kolorowane po statusie. Dane z
  `GET /stats/rentals-per-month?months=12`.
- **Aktywne wypożyczenia w czasie** (`Area`, stack po statusie) — liczba
  aktywnych wypożyczeń (statusy `PENDING`, `SENT`, `DELIVERED`) w 180
  punktach czasu w wybranym oknie (domyślnie 10080 minut = 7 dni, czyli
  ≈ co 1 godzinę). Backend rekonstruuje status każdego wypożyczenia na
  podstawie historii w `ItemRentalUpdate`. Dane z
  `GET /stats/active-rentals-over-time?minutes=10080`.

Oba endpointy są zabezpieczone `@AdminOnly`.

### 6.5 Tryb połączenia z bazą danych

Plik `tool_rental_api/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tool_rental
spring.datasource.username=tool_rental
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Aplikacja używa JDBC + dialektu PostgreSQL dla Hibernate, schemat synchronizowany
przy starcie (`ddl-auto=update`); skrypt referencyjny w `db/schema.sql`.

### 6.6 Kodowanie hasła

Hasła użytkowników są hashowane algorytmem **BCrypt**
(`org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`) zanim
trafią do bazy. Implementacja jest umieszczona w setterze
`User.setPassword(rawPassword)` (encja sama hashuje przed zapisem), dzięki
czemu kod biznesowy nie może przypadkowo zapisać hasła w postaci jawnej.
W bazie kolumna `password` zawiera ciąg postaci
`$2a$10$<22-byte-salt><31-char-hash>`. Weryfikacja przy logowaniu odbywa
się przez `BCryptPasswordEncoder.matches`.

### 6.7 Baza danych

PostgreSQL — serwer dostępny pod `localhost:5432`, baza `tool_rental`. Wybór
podyktowany pełną zgodnością z JPA/Hibernate przez dialekt `PostgreSQLDialect`,
obsługą sekwencji, oraz powszechnym użyciem w środowiskach produkcyjnych.

### 6.8 Struktura frontendu

Frontend w `tool_rental_web/` jest SPA-ką opartą na React Router 7. Główne
elementy architektury:

- **Routing** (`src/App.tsx`) — top-level ścieżki: `/` (redirect → `/items`),
  `/login`, `/register`, katalog narzędzi (`/items`, `/items/:id`,
  `/items/:id/rent`), wypożyczenia (`/rentals`, `/rentals/:id`), profil
  (`/profile`, `/profile/edit`) oraz sekcja administratora (`/admin/items`,
  `/admin/items/new`, `/admin/items/:id/edit`, `/admin/rentals`,
  `/admin/users`, `/admin/users/:id`, `/admin/statistics`). Fallback
  `*` → `NotFoundPage`.
- **Katalog `src/pages/`** — podzielony funkcjonalnie: `auth/`
  (`LoginPage`, `RegisterPage`), `items/`, `rentals/`, `users/`, `admin/`
  (`StatisticsPage` z resetem bazy), `fallback/` (`NotFoundPage`).
- **`AuthContext`** (`src/context/AuthContext.tsx`) — przechowuje
  zalogowanego użytkownika, dekoduje payload JWT i wystawia flagi
  `isAdmin` / `isCustomer`, używane przez `ProtectedRoute` oraz
  `MenuSider` do warunkowego renderowania pozycji menu i całych tras.
- **Motyw** — ciemny motyw na sztywno przez
  `ConfigProvider theme={{ algorithm: theme.darkAlgorithm }}` w `App.tsx`.
  Tailwind jest używany do dodatkowego layoutu (flex, spacing),
  podstawowe komponenty pochodzą z Ant Design.
- **Walidacja formularzy** — przez `Form.Item rules` z Ant Design (`required`,
  `type: 'email'`, własne walidatory dla dat startu i ilości sztuk).
- **Powiadomienia** — `NotificationBridge.tsx` wpina API powiadomień Ant
  (`App.useApp().notification`) w globalnego klienta Axios, dzięki czemu
  błędy z backendu są pokazywane jednolitymi toastami w całej aplikacji.
- **Kolorowanie statusów** — wypożyczenia są kolorowane spójnie w tabelach,
  szczegółach i wykresach (`PENDING`, `SENT`, `DELIVERED`, `RETURNED`,
  `CANCELLED`, `LOST`).

## 7. Procedura instalacji i uruchamiania

Oba repozytoria mają własne pliki `Dockerfile` i `docker-compose.yml`,
więc całe uruchomienie sprowadza się do `docker compose up` w każdym
z nich.

### Wymagania

- **Docker 20+** oraz **Docker Compose v2**
- **Git** do sklonowania obu repozytoriów

### Krok 1 — backend (PostgreSQL + API)

```bash
cd tool_rental_api
docker compose up -d --build
```

`docker-compose.yml` uruchamia dwie usługi:

- **`postgres`** — `postgres:17-alpine`, baza `tool_rental`, na porcie
  `5432`. Dane trzymane w wolumenie `postgres_data`.
- **`api`** — obraz budowany z `Dockerfile` (multi-stage build na
  `eclipse-temurin:26-jdk`, `./gradlew bootJar`), wystawiony na porcie
  `8080`. Czeka na healthcheck Postgresa przed startem.

Po starcie API `DatabaseSeeder` zauważy, że baza jest pusta, i zaseeduje
dane przykładowe (5 użytkowników, 300 narzędzi, 240 wypożyczeń).

Domyślne zmienne (`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`,
`JWT_SECRET`) można nadpisać przez plik `.env` lub eksport w shellu —
patrz wartości domyślne w `docker-compose.yml`.

### Krok 2 — frontend

W osobnym terminalu:

```bash
cd tool_rental_web
docker compose up -d --build
```

`docker-compose.yml` buduje obraz z `Dockerfile` (build w `node:22-alpine`
→ statyczny bundle w `nginx:alpine`) i wystawia frontend na porcie `80`.
Adres backendu jest wpiekany w bundle w czasie buildu z argumentu
`VITE_API_BASE_URL` (domyślnie `http://localhost:8080`).

Aplikacja dostępna pod `http://localhost`.

### Krok 3 — logowanie

Konto administratora jest hardcodowane:

| Login        | Hasło      | Rola      |
|--------------|------------|-----------|
| `admin`      | `admin`    | ADMIN     |

Konta klientów i konto zawieszone generuje `SeedingService` z użyciem
Datafakera (deterministyczny seed, więc loginy są powtarzalne).
Wszystkie wygenerowane konta mają hasło `password`; konkretne loginy
można podejrzeć po zalogowaniu jako `admin` w widoku **Users**.

### Krok 4 — reset bazy z aplikacji

Po zalogowaniu jako `admin` przejść do **Statistics** w menu bocznym i kliknąć
**Reset DB with sample data** → potwierdzić. Baza zostanie wyczyszczona i
ponownie zaseedowana w jednej transakcji.

### Krok 5 — ręczne odtworzenie schematu (opcjonalnie)

```bash
docker compose exec -T postgres \
    psql -U tool_rental -d tool_rental \
    < src/main/resources/db/schema.sql
```

Po wykonaniu skryptu kolejny start API (lub kliknięcie **Reset DB with
sample data**) wypełni puste tabele danymi przez `SeedingService`.

### Zatrzymanie i restart

```bash
docker compose down            # zatrzymuje kontenery, wolumen zostaje
docker compose down -v         # zatrzymuje + usuwa wolumen Postgresa
```

## 8. Źródła

- Spring Boot — <https://docs.spring.io/spring-boot/index.html>
- Spring Security — <https://docs.spring.io/spring-security/reference/index.html>
- Hibernate ORM — <https://hibernate.org/orm/documentation/>
- BCrypt (`spring-security-crypto`) —
  <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- JJWT (jsonwebtoken/jjwt) — <https://github.com/jwtk/jjwt>
- PostgreSQL — <https://www.postgresql.org/docs/>
- React — <https://react.dev/>
- Vite — <https://vitejs.dev/>
- Ant Design — <https://ant.design/>
- Ant Design Charts — <https://charts.ant.design/>
- Tailwind CSS — <https://tailwindcss.com/>
- Mermaid (diagramy) — <https://mermaid.js.org/>
