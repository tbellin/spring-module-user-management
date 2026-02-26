# Rate Limit Options


````
rate-limit-options
    requests_per_minute: 20–30
    max_concurrent_requests: 1
    retry_backoff: true
`````

Certo. Te lo spiego **riga per riga**, in modo pratico, pensando **esattamente** a
**get-shit-done-cc + Claude Pro**.

Immagina di dare istruzioni a una persona molto brava… ma che si stanca se la pressi troppo.

---

## `/rate-limit-options`

È il **freno a mano** dell’agente.
Dice a get-shit-done-cc **come comportarsi** quando parla con Claude.

---

## `requests_per_minute: 20–30`

### Cosa significa

Numero massimo di **richieste totali** che l’agente può inviare a Claude **in un minuto**.

### Perché 20–30

* Claude Pro **non ama raffiche**
* 1 richiesta ogni **2–3 secondi** ≈ ritmo umano veloce
* sotto questa soglia:

  * meno blocchi
  * risposte più complete
  * contesto più stabile

### Cosa succede se alzi troppo

* Claude inizia a:

  * tagliare le risposte
  * diventare vago
  * rallentare “di nascosto”
* nei casi peggiori: stop temporaneo

💡 **Regola pratica**
Refactor serio? → 20
Task brevi? → 30
Loop automatici? → 15–20

---

## `max_concurrent_requests: 1`

### Cosa significa

Claude riceve **una sola richiesta alla volta**.

### Perché è cruciale

Claude Pro:

* mantiene contesto **per conversazione**
* se riceve richieste parallele:

  * confonde i task
  * risponde fuori ordine
  * perde coerenza

Con `1`:

* ogni risposta arriva
* viene letta
* solo dopo parte la successiva

È come dire:

> “Finisci di parlare, poi ti faccio la prossima domanda.”

---

## `retry_backoff: true`

### Cosa significa

Se Claude dice:

* “sono sovraccarico”
* risponde male
* non risponde

👉 l’agente **NON insiste subito**, ma aspetta.

### Perché serve

Senza backoff:

* l’agente martella
* Claude si irrigidisce
* vieni bloccato più a lungo

Con backoff:

* Claude “respira”
* la richiesta successiva passa
* meno errori a catena

---

## `retry_after: 2–4s`

### Cosa significa

Quando c’è un errore o throttle:

* aspetta **2–4 secondi**
* poi riprova

### Perché questo intervallo

* < 2s → troppo aggressivo
* > 5s → rallenta inutilmente

2–4s è il **tempo giusto** per:

* uscire dal throttle
* non perdere il flusso di lavoro

---

## Tutto insieme, cosa ottieni

Con questa configurazione:

* l’agente lavora **più lentamente**
* MA:

  * meno errori
  * meno risposte troncate
  * meno reset di contesto
  * più task completati

È il classico:

> *meno gas → arrivi più lontano*

---

## Segnale che sei settato bene

Se noti che:

* Claude risponde in modo coerente
* le risposte non si accorciano col tempo
* l’agente completa i task senza “impazzire”

👉 sei nel **sweet spot**.

Se vuoi, posso:

* adattare questi valori **al tuo repo**
* dirti **quando alzarli o abbassarli**
* spiegarti **come capire dai log se stai throttlando**

Dimmi tu 😊


