# Plan de Implementación — Proyecto 1: Sistemas Operativos

## Stack Tecnológico

**Lenguaje:** Java 21 (OpenJDK)
**GUI/Animación:** JavaFX 21 con CSS personalizado
**Build:** El proyecto se compila y ejecuta sobre Windows 11.

**Justificación del stack:**
JavaFX provee un motor de renderizado con soporte nativo para animaciones (`Timeline`, `TranslateTransition`, `FadeTransition`, `FillTransition`), binding reactivo de propiedades (ideal para reflejar estados de threads en la UI), y estilización por CSS — lo cual permite lograr un resultado visual pulido sin dependencias externas. Java 21 ofrece virtual threads y `ReentrantLock` mejorado, aunque usaremos primitivas clásicas de sincronización para alinearnos al contenido del curso.

---

## Arquitectura General

La aplicación es un único ejecutable con un **menú principal** que permite seleccionar entre los dos problemas. Cada problema vive en su propio módulo/paquete, pero comparten un shell visual común (la ventana principal, el tema CSS, utilidades de animación).

```
src/
├── Main.java                          // Entry point + menú principal
├── common/
│   ├── AnimationUtils.java            // Helpers de animación reutilizables
│   ├── AppTheme.css                   // Tema visual global
│   └── LogPanel.java                  // Panel de log de eventos en tiempo real
├── producerconsumer/
│   ├── PCController.java              // Orquestador de la simulación
│   ├── Producer.java                  // Thread productor
│   ├── Consumer.java                  // Thread consumidor (pares/impares/primos)
│   ├── SharedBuffer.java             // Buffer circular sincronizado
│   ├── NumberClassifier.java          // Clasificador par/impar/primo
│   ├── PCAnimationView.java           // Vista animada (JavaFX)
│   └── data/
│       └── numeros.txt                // Archivo de entrada de ejemplo
├── philosophers/
│   ├── DPController.java              // Orquestador de la simulación
│   ├── Philosopher.java               // Thread filósofo
│   ├── Fork.java                      // Recurso tenedor (sincronizado)
│   ├── DPAnimationView.java           // Vista animada (JavaFX)
│   └── PhilosopherConfig.java         // Configuración (N filósofos)
```

**Flujo de navegación:**
`Main` → Pantalla menú (2 botones: "Productor-Consumidor" / "Filósofos Comensales") → Escena del problema seleccionado → Botón de regreso al menú.

---

## Problema 1: Productor-Consumidor

### Modelado Formal

**Actores:**
- 1 Productor `P` — lee números de un archivo y los deposita en el buffer.
- 3 Consumidores `C₁, C₂, C₃` — retiran números del buffer según su tipo:
  - `C₁`: números pares (no primos)
  - `C₂`: números impares (no primos)
  - `C₃`: números primos (independientemente de su paridad)

**Recurso compartido:**
- Buffer circular `B` de tamaño finito `N`.

**Invariantes del sistema:**
```
0 ≤ |B| ≤ N                          // El buffer nunca excede su capacidad
Si |B| = N → P se bloquea            // Productor espera si buffer lleno
Si |B| = 0 → Cᵢ se bloquea          // Consumidor espera si buffer vacío
∀ elemento e ∈ B, e es consumido por exactamente un Cᵢ según su clasificación
```

**Prioridad de clasificación:**
Un número primo (ej: 7) es impar, pero la condición de primo domina. El clasificador evalúa en orden: ¿es primo? → va a `C₃`. Si no es primo: ¿es par? → `C₁`, ¿es impar? → `C₂`.

### Mecanismo de Sincronización: Semáforos + Mutex

**Elección:** Semáforos contadores (`Semaphore` de `java.util.concurrent`) + un mutex (`Semaphore(1)` o `ReentrantLock`).

**Justificación:**
El problema Productor-Consumidor tiene una estructura de "counting" natural: necesitamos rastrear cuántos slots están vacíos y cuántos están llenos. Los semáforos contadores modelan esto directamente — `empty` inicia en `N` (slots vacíos disponibles) y `full` inicia en `0` (productos disponibles). Esto es más expresivo y eficiente que usar solo mutex + condition variables, porque los semáforos ya codifican la cuenta de recursos en su valor. Un mutex adicional protege la modificación atómica del buffer.

Alternativas descartadas:
- **Solo mutex + condiciones:** Requeriría mantener contadores manuales y señalización explícita, más propenso a errores.
- **Monitores (synchronized):** Funcional pero menos didáctico para demostrar el manejo explícito de la sección crítica que pide la rúbrica.
- **BlockingQueue de Java:** Abstrae todo el problema y no permite demostrar el manejo manual de sincronización.

**Pseudocódigo formal:**

```
Semáforos:
  empty  = Semaphore(N)     // slots vacíos disponibles
  full   = Semaphore(0)     // productos disponibles para consumir
  mutex  = Semaphore(1)     // exclusión mutua sobre el buffer

Productor P:
  while hay_numeros_en_archivo:
      numero = leer_siguiente()
      empty.acquire()           // esperar slot vacío (se bloquea si buffer lleno)
      mutex.acquire()           // INICIO sección crítica
        buffer.insertar(numero)
      mutex.release()           // FIN sección crítica
      full.release()            // señalar que hay un producto disponible

Consumidor Cᵢ:
  while simulación_activa:
      full.acquire()            // esperar producto disponible (se bloquea si vacío)
      mutex.acquire()           // INICIO sección crítica
        elemento = buffer.buscar_y_extraer(tipo_i)
      mutex.release()           // FIN sección crítica
      if elemento != null:
          suma_i += elemento
          actualizar_vista(suma_i)
      else:
          full.release()        // devolver el permiso si no había elemento de su tipo
```

> **Nota sobre la búsqueda selectiva:** Los consumidores no extraen el primer elemento del buffer — buscan el primer elemento que coincida con su tipo (par, impar, primo). Si no encuentran uno, liberan el semáforo `full` y vuelven a intentar. Esto requiere que el buffer permita lectura indexada bajo mutex, no solo FIFO puro.

### Análisis de Condiciones de Carrera

| Escenario | Riesgo | Mitigación |
|-----------|--------|------------|
| Dos consumidores acceden al buffer simultáneamente | Podrían leer/extraer el mismo elemento | `mutex` garantiza exclusión mutua en toda operación de lectura+extracción |
| Productor y consumidor acceden al buffer al mismo tiempo | Escritura y lectura concurrente corrompe índices | `mutex` serializa todos los accesos |
| Consumidor lee un elemento que otro ya marcó para extracción | Estado inconsistente del buffer | Búsqueda + extracción son una sola operación atómica dentro del mutex |
| Contador de suma se actualiza desde thread consumidor y se lee desde thread UI | Data race sobre la variable suma | Se usa `Platform.runLater()` para actualizar la UI desde el thread de JavaFX, y la suma se mantiene local al consumidor |

### Prevención de Deadlock

El deadlock en Productor-Consumidor clásico ocurre cuando el productor espera un slot vacío y el consumidor espera un producto, formando una espera circular. Nuestra solución lo evita porque:

1. **Orden de adquisición consistente:** Tanto productor como consumidores adquieren `empty`/`full` *antes* que `mutex`. Nunca se invierte el orden.
2. **No hay retención y espera cruzada:** Un thread nunca retiene `mutex` mientras espera `empty` o `full`. El semáforo de capacidad se adquiere *fuera* de la sección crítica.
3. **El mutex se libera siempre:** La sección crítica es corta y determinista (insertar o extraer). No hay operaciones bloqueantes dentro del mutex.
4. **Señal de terminación:** Cuando el productor termina de leer el archivo, envía un "elemento veneno" (poison pill) o señal de fin para que los consumidores sepan cuándo terminar limpiamente.

### Diseño de Clases

**`SharedBuffer`**
```java
public class SharedBuffer {
    private final int[] buffer;
    private final int capacity;
    private int count;
    private int inIndex, outIndex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    // insertar(int numero) — llamado por el productor
    // buscarYExtraer(TipoNumero tipo) — llamado por consumidores
    // getEstado() — snapshot inmutable para la animación
}
```

**`NumberClassifier`**
```java
public class NumberClassifier {
    public static TipoNumero clasificar(int n) {
        if (esPrimo(n)) return TipoNumero.PRIMO;
        if (n % 2 == 0)  return TipoNumero.PAR;
        return TipoNumero.IMPAR;
    }

    private static boolean esPrimo(int n) {
        if (n < 2) return false;
        for (int i = 2; i <= Math.sqrt(n); i++)
            if (n % i == 0) return false;
        return true;
    }
}
```

**`Producer` y `Consumer`** extienden `Thread`. Cada uno recibe referencia al `SharedBuffer` y a un callback para notificar cambios de estado a la vista.

### Diseño de la Animación

**Temática visual:** Línea de producción industrial / conveyor belt.

**Layout de la pantalla:**

```
┌──────────────────────────────────────────────────────────────────┐
│  [← Menú]              PRODUCTOR - CONSUMIDOR           [⏯ ⏹]  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────┐     ┌─────────────────────────┐    ┌──────────┐  │
│   │          │     │ ░░ █ █ ░░ █ ░░ ░░ █ ░░  │    │ C₁ PARES │  │
│   │ PRODUCTOR│ ──▶ │    BUFFER (8/12)         │ ──▶│ Σ = 142  │  │
│   │  Leyendo │     │ ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░  │    │──────────│  │
│   │  archivo │     └─────────────────────────┘    │ C₂ IMPAR │  │
│   └──────────┘                                     │ Σ = 87   │  │
│                                                    │──────────│  │
│                                                    │ C₃ PRIMO │  │
│                                                    │ Σ = 58   │  │
│                                                    └──────────┘  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│  LOG: [12:00:01] Productor insertó 17 (primo) en slot 3         │
│       [12:00:01] C₃ extrajo 17 — Σ primos = 58                  │
│       [12:00:02] Productor BLOQUEADO — buffer lleno              │
└──────────────────────────────────────────────────────────────────┘
```

**Elementos visuales clave:**

| Componente | Representación | Tecnología JavaFX |
|------------|---------------|-------------------|
| Buffer | Fila horizontal de celdas (rectángulos). Celda vacía = gris tenue, celda con par = azul, impar = verde, primo = dorado | `Rectangle` + `FillTransition` al cambiar estado |
| Productor | Caja/avatar a la izquierda con ícono de engranaje. Muestra el número que acaba de leer | `StackPane` con animación de "pulso" al insertar |
| Consumidores | 3 cajas a la derecha, cada una con su color y suma acumulada | `VBox` con `Label` bindeado a `IntegerProperty` |
| Conveyor belt | Línea punteada animada entre productor → buffer → consumidores | `StrokeDashOffset` animado con `Timeline` |
| Estado bloqueado | Ícono de candado rojo + borde parpadeante sobre el actor bloqueado | `FadeTransition` loop + overlay de candado |
| Sección crítica activa | Borde brillante/glow alrededor del buffer cuando un thread está dentro del mutex | `DropShadow` effect con `Timeline` |
| Barra de progreso del buffer | Barra debajo del buffer que muestra ocupación actual vs capacidad | `ProgressBar` bindeada a `count/capacity` |

**Indicadores de estado de cada thread:**
- 🟢 Activo (produciendo/consumiendo)
- 🔴 Bloqueado (esperando semáforo)
- 🟡 En sección crítica
- ⚪ Idle/Terminado

**Velocidad de simulación:** Un slider permite controlar el delay entre operaciones (50ms - 2000ms) para poder observar la animación en detalle o acelerarla.

---

## Problema 2: Filósofos Comensales

### Modelado Formal

**Actores:**
- `N` filósofos `F₀, F₁, ..., Fₙ₋₁` (configurable, por defecto 5).
- `N` tenedores `T₀, T₁, ..., Tₙ₋₁`, donde `Tᵢ` está entre `Fᵢ` y `F₍ᵢ₊₁₎ₘₒ𝒹 ₙ`.

**Estados de cada filósofo:**
```
Fᵢ ∈ { PENSANDO, ESPERANDO, COMIENDO }
```

**Reglas:**
```
Para que Fᵢ pase a COMIENDO:
  Fᵢ debe poseer Tᵢ (tenedor izquierdo) Y T₍ᵢ₊₁₎ₘₒ𝒹 ₙ (tenedor derecho)

Para Tⱼ:
  En todo momento, a lo sumo un filósofo posee Tⱼ  (exclusión mutua)
```

**Invariantes del sistema:**
```
∀ Tⱼ : |{Fᵢ : Fᵢ posee Tⱼ}| ≤ 1
∀ Fᵢ : Fᵢ.estado = COMIENDO → Fᵢ posee Tᵢ ∧ Fᵢ posee T₍ᵢ₊₁₎ₘₒ𝒹 ₙ
A lo sumo ⌊N/2⌋ filósofos pueden comer simultáneamente
```

### Mecanismo de Sincronización: Solución de Dijkstra con Estados + Mutex

**Elección:** La solución basada en un arreglo de estados por filósofo, un mutex global, y un semáforo por filósofo (la solución clásica de Dijkstra/Tanenbaum).

**Justificación:**
Esta solución permite que un filósofo tome ambos tenedores atómicamente (o ninguno), eliminando la posibilidad de deadlock sin sacrificar concurrencia. Es superior a las alternativas porque:

- **vs. "Tomar izquierdo, luego derecho":** Esa aproximación naïve causa deadlock si todos toman el izquierdo simultáneamente.
- **vs. Orden global de recursos (numeración de tenedores):** Funciona pero es menos expresivo didácticamente y puede causar inanición.
- **vs. Limitar comensales (N-1 máximo):** Rompe el deadlock pero de forma artificial. No modela la solución elegante con estados.

La solución elegida modela explícitamente los 3 estados del filósofo y usa `test()` para verificar si los vecinos no están comiendo antes de conceder acceso. Esto demuestra exclusión mutua, ausencia de deadlock, y ausencia de inanición de forma clara.

**Pseudocódigo formal:**

```
estado[N]        = {PENSANDO, ...}     // estado de cada filósofo
mutex            = Semaphore(1)         // protege el arreglo de estados
sem[N]           = {Semaphore(0), ...}  // un semáforo por filósofo

función test(i):
    if estado[i] == ESPERANDO
       AND estado[izq(i)] != COMIENDO
       AND estado[der(i)] != COMIENDO:
        estado[i] = COMIENDO
        sem[i].release()          // despertar al filósofo i

función tomar_tenedores(i):
    mutex.acquire()
      estado[i] = ESPERANDO
      test(i)                     // intentar comer de inmediato
    mutex.release()
    sem[i].acquire()              // si test no lo liberó, se bloquea aquí

función soltar_tenedores(i):
    mutex.acquire()
      estado[i] = PENSANDO
      test(izq(i))                // verificar si vecino izquierdo puede comer
      test(der(i))                // verificar si vecino derecho puede comer
    mutex.release()

Filósofo Fᵢ:
    while true:
        pensar()                  // tiempo aleatorio
        tomar_tenedores(i)
        comer()                   // tiempo aleatorio
        soltar_tenedores(i)
```

Donde `izq(i) = (i + N - 1) % N` y `der(i) = (i + 1) % N`.

### Análisis de Condiciones de Carrera

| Escenario | Riesgo | Mitigación |
|-----------|--------|------------|
| Dos filósofos adyacentes intentan comer al mismo tiempo | Ambos leen que el tenedor compartido está libre | `mutex` serializa toda lectura/escritura del arreglo de estados. `test()` es atómico dentro del mutex |
| Un filósofo cambia su estado a ESPERANDO mientras su vecino ejecuta `test()` | Estado inconsistente, podría otorgar tenedor a ambos | Ambas operaciones ocurren dentro del mismo `mutex` |
| La UI lee el estado de un filósofo mientras un thread lo modifica | Data race visual | Se usa snapshot inmutable del estado dentro del mutex, y `Platform.runLater()` para actualizar la UI |

### Prevención de Deadlock

La solución previene deadlock porque:

1. **No hay retención y espera (hold-and-wait):** Un filósofo nunca toma un solo tenedor y luego espera por el otro. La función `test()` verifica *ambos* vecinos antes de conceder acceso. Si no puede comer, no retiene ningún recurso.
2. **No hay espera circular:** Dado que los tenedores se asignan atómicamente (ambos o ninguno) mediante `test()`, no puede formarse una cadena circular de esperas.
3. **Progreso garantizado:** Cuando un filósofo termina de comer y llama a `soltar_tenedores()`, verifica si sus vecinos pueden comer (`test(izq)`, `test(der)`). Esto garantiza que si hay un filósofo esperando y los recursos están disponibles, será despertado.

**Sobre inanición:** En la implementación pura de Dijkstra, la inanición es teóricamente posible (un filósofo podría esperar indefinidamente si sus vecinos se turnan para comer). Para mitigarla en la práctica, los tiempos de pensar y comer son aleatorios, lo cual distribuye el acceso de forma equitativa estadísticamente.

### Diseño de Clases

**`Fork`**
```java
public class Fork {
    private final int id;
    private boolean taken = false;
    private int heldBy = -1;
    // Nota: el acceso a Fork no se sincroniza individualmente,
    // se controla mediante el mutex global + estado[] en DPController
}
```

**`Philosopher` extiende `Thread`**
```java
public class Philosopher extends Thread {
    private final int id;
    private final DPController controller;
    // run() implementa el ciclo pensar-comer
    // callbacks para notificar cambio de estado a la vista
}
```

**`DPController`**
```java
public class DPController {
    private final int N;
    private final EstadoFilosofo[] estado;
    private final Semaphore mutex;
    private final Semaphore[] sem;

    // tomarTenedores(int i)
    // soltarTenedores(int i)
    // test(int i)
    // getSnapshot() → copia inmutable para la UI
}
```

**`PhilosopherConfig`**
```java
public class PhilosopherConfig {
    private int numFilosofos = 5;    // mínimo 2, máximo ~10 para que la UI sea legible
    private int tiempoMinPensar = 500;
    private int tiempoMaxPensar = 3000;
    private int tiempoMinComer = 500;
    private int tiempoMaxComer = 2000;
}
```

### Diseño de la Animación

**Temática visual:** Mesa de restaurante elegante vista cenital, combinada con indicadores técnicos de estado de threads.

**Layout de la pantalla:**

```
┌──────────────────────────────────────────────────────────────────┐
│  [← Menú]           FILÓSOFOS COMENSALES  (N=5)        [⏯ ⏹]  │
│                                                    [Config ⚙]   │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│                        F₀ 🤔                                     │
│                    T₄ /    \ T₀                                  │
│                      /      \                                    │
│               F₄ 🍽 ── mesa ── F₁ ⏳                             │
│                      \      /                                    │
│                    T₃ \    / T₁                                  │
│                     F₃ 🤔─T₂─ F₂ 🍽                              │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  F₀: PENSANDO   F₁: ESPERANDO   F₂: COMIENDO              │ │
│  │  F₃: PENSANDO   F₄: COMIENDO                               │ │
│  │  T₀: libre  T₁: F₂  T₂: F₂  T₃: F₄  T₄: F₄              │ │
│  └─────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────┤
│  LOG: [12:00:05] F₂ tomó tenedores T₁ y T₂ — comiendo          │
│       [12:00:06] F₁ intentó tomar tenedores — BLOQUEADO         │
│       [12:00:08] F₄ soltó tenedores T₃ y T₄ — pensando         │
└──────────────────────────────────────────────────────────────────┘
```

**Elementos visuales clave:**

| Componente | Representación | Tecnología JavaFX |
|------------|---------------|-------------------|
| Mesa circular | Círculo central café/madera con textura | `Circle` + `RadialGradient` |
| Filósofos | Círculos posicionados alrededor de la mesa con ícono/emoji de estado | `StackPane` (Circle + Label) posicionados con trigonometría |
| Tenedores | Líneas/íconos entre cada par de filósofos | `Line` o `ImageView`. Color cambia según estado (gris=libre, rojo=ocupado) |
| Estado PENSANDO | Filósofo color azul tenue + ícono 💭 | `FillTransition` a azul |
| Estado ESPERANDO | Filósofo color amarillo parpadeante + ícono ⏳ | `FadeTransition` loop |
| Estado COMIENDO | Filósofo color verde brillante + ícono 🍽 + tenedores adyacentes se "inclinan" hacia él | `FillTransition` a verde + `RotateTransition` en tenedores |
| Panel de estados | Tabla compacta debajo de la mesa con el estado de cada filósofo y tenedor en texto | `GridPane` con `Label` bindeados a propiedades observables |
| Indicador de sección crítica | Borde glow alrededor del filósofo cuando ejecuta `test()` o modifica estado | `DropShadow` + `Timeline` |

**Configuración dinámica:**
Un panel lateral o diálogo de configuración permite ajustar `N` (número de filósofos), velocidad de simulación, y tiempos de pensar/comer antes de iniciar. Al cambiar `N`, la vista se recalcula (posiciones con trigonometría: `x = cx + r·cos(2πi/N)`, `y = cy + r·sin(2πi/N)`).

---

## Elementos Compartidos de UI

### Tema Visual Global (CSS)

```css
/* AppTheme.css — extracto de referencia */
.root {
    -fx-background-color: #1a1a2e;       /* Fondo oscuro elegante */
    -fx-font-family: "Segoe UI", sans-serif;
}

.card {
    -fx-background-color: #16213e;
    -fx-background-radius: 12;
    -fx-padding: 16;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);
}

.status-active    { -fx-fill: #00e676; }   /* Verde — activo/comiendo */
.status-blocked   { -fx-fill: #ff1744; }   /* Rojo — bloqueado */
.status-critical  { -fx-fill: #ffea00; }   /* Amarillo — sección crítica */
.status-idle      { -fx-fill: #546e7a; }   /* Gris — idle */

.buffer-slot-empty    { -fx-fill: #37474f; }
.buffer-slot-par      { -fx-fill: #42a5f5; }   /* Azul */
.buffer-slot-impar    { -fx-fill: #66bb6a; }   /* Verde */
.buffer-slot-primo    { -fx-fill: #ffd54f; }   /* Dorado */

.log-panel {
    -fx-background-color: #0d1117;
    -fx-text-fill: #c9d1d9;
    -fx-font-family: "Consolas", monospace;
    -fx-font-size: 11;
}
```

### Panel de Log de Eventos

Ambos problemas comparten un `LogPanel` en la parte inferior que muestra eventos en tiempo real con timestamp, color-coded por tipo de evento (inserción, extracción, bloqueo, desbloqueo). El log es scrollable y se auto-scroll al último evento.

### Controles de Simulación

Ambas vistas incluyen:
- **Play/Pause** — pausa todos los threads sin perder estado.
- **Stop** — termina la simulación y permite reiniciar.
- **Slider de velocidad** — controla el delay base entre operaciones.

---

## Archivo de Datos de Ejemplo

`numeros.txt` — contiene una mezcla balanceada de pares, impares y primos:

```
12
7
15
2
23
44
9
31
18
5
27
50
13
36
21
3
40
17
8
29
33
46
11
25
42
19
6
37
14
41
22
55
4
47
30
39
10
43
20
53
```

**Distribución del archivo (40 números):**
- Pares (no primos): 12, 44, 18, 50, 36, 40, 8, 46, 42, 6, 14, 22, 4, 30, 10, 20 → **16 números**
- Impares (no primos): 15, 9, 27, 21, 33, 25, 39, 55 → **8 números**
- Primos: 7, 2, 23, 31, 5, 13, 3, 17, 29, 11, 19, 37, 41, 47, 43, 53 → **16 números**

> Nota: El número 2 es par Y primo. Según las reglas del proyecto, la condición de primo domina, así que 2 va al consumidor de primos `C₃`.

---

## Flujo de Ejecución Esperado

### Productor-Consumidor
1. Usuario selecciona "Productor-Consumidor" en el menú.
2. Se muestra la vista con el buffer vacío, productor y 3 consumidores idle.
3. Usuario presiona Play.
4. El productor comienza a leer `numeros.txt` y depositar números en el buffer.
5. Los consumidores despiertan y comienzan a buscar números de su tipo.
6. La animación muestra en tiempo real: números entrando al buffer (animación de slide-in), números siendo extraídos (animación de slide-out con color del consumidor), estados de bloqueo cuando el buffer está lleno/vacío, y las sumas acumuladas incrementándose.
7. Cuando el productor termina de leer el archivo, envía señal de terminación.
8. Los consumidores procesan los elementos restantes y terminan.
9. Se muestra un resumen final: suma de pares, suma de impares, suma de primos.

### Filósofos Comensales
1. Usuario selecciona "Filósofos Comensales" en el menú.
2. Se muestra diálogo de configuración (N filósofos, velocidades). Por defecto N=5.
3. Se renderiza la mesa circular con N filósofos y N tenedores.
4. Usuario presiona Play.
5. Los filósofos comienzan a alternar entre pensar y comer con tiempos aleatorios.
6. La animación muestra cambios de estado (colores), movimiento de tenedores, y eventos de bloqueo/desbloqueo.
7. El log muestra la secuencia temporal de todos los eventos.
8. La simulación corre indefinidamente hasta que el usuario presiona Stop.

---

## Consideraciones Técnicas para la Implementación

**Thread safety con JavaFX:** JavaFX requiere que toda modificación de la UI ocurra en el Application Thread. Todos los callbacks de los threads de simulación deben usar `Platform.runLater()` para actualizar la vista. Usar propiedades observables (`SimpleIntegerProperty`, `SimpleObjectProperty<Estado>`) con bindings permite que la UI se actualice automáticamente.

**Terminación limpia de threads:** Al presionar Stop o regresar al menú, todos los threads deben terminarse limpiamente. Usar una bandera `volatile boolean running` que los threads verifican en cada iteración. Adicionalmente, llamar `thread.interrupt()` para despertar threads bloqueados en `acquire()`.

**Snapshots inmutables para la UI:** En vez de que la UI lea directamente las estructuras compartidas (lo cual requeriría sincronización adicional), cada ciclo de animación solicita un snapshot inmutable del estado actual (copia del buffer, estados de filósofos, etc.) dentro del mutex, y lo pasa a la UI.

**Responsividad:** La simulación corre en threads separados. La UI nunca se bloquea. El `Timeline` de JavaFX que actualiza la animación corre a ~30-60 FPS independientemente de la velocidad de la simulación.