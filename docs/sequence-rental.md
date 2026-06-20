# Diagram sekwencji — Wypożyczenie narzędzia

Scenariusz: zalogowany klient klika *Rent* na stronie szczegółów narzędzia,
podaje datę i liczbę sztuk, a aplikacja zapisuje wypożyczenie w bazie i zwraca
potwierdzenie.

```mermaid
sequenceDiagram
    actor Customer as Klient (przeglądarka)
    participant React as Frontend React
    participant API as API (Spring Boot)
    participant Controller as ItemRentalController
    participant Repo as Repo (JPA)
    participant DB as PostgreSQL

    Customer->>React: Klik "Rent" + formularz<br/>(itemId, startDate, amount)
    React->>API: POST /rentals<br/>Authorization: Bearer <JWT>
    API->>API: JwtAuthFilter — dekoduj JWT,<br/>ustaw SecurityContext
    API->>Controller: @CustomerOnly check ok

    alt brak wymaganych pól lub amount < 1
        Controller-->>API: 400 Bad Request
        API-->>React: 400
        React-->>Customer: Komunikat o błędzie
    else dane wejściowe poprawne
        Controller->>Repo: itemRepository.findByIdForUpdate(itemId)
        Repo->>DB: SELECT ... FOR UPDATE
        DB-->>Repo: Item (z availableAmount)
        Repo-->>Controller: Item

        alt item nie istnieje lub amount > availableAmount
            Controller-->>API: 400 Bad Request
            API-->>React: 400
            React-->>Customer: Komunikat o błędzie
        else stan magazynowy ok
            Controller->>Repo: itemRentalRepository.save(rental)
            Repo->>DB: INSERT INTO item_rental ...
            DB-->>Repo: ItemRental
            Repo-->>Controller: saved
            Controller->>Repo: itemRentalUpdateRepository.save(update)
            Repo->>DB: INSERT INTO item_rental_update ...
            Controller-->>API: 201 Created + ItemRental
            API-->>React: 201 + JSON
            React-->>Customer: Przekierowanie na /rentals/{id}<br/>+ powiadomienie sukcesu
        end
    end
```
