# Documento de Visión - Valoración de Mercado de Jugadores de Fútbol

## 1. Descripción General
Se deberá realizar un desarrollo **backend** con una suite de **APIs REST** expuesta que integre información de jugadores de fútbol de 5 ligas principales [cite: 3]:
* Premier League (Inglaterra) [cite: 3]
* Bundesliga (Alemania) [cite: 3]
* La Liga (España) [cite: 3]
* Serie A (Italia) [cite: 3]
* Ligue 1 (Francia) [cite: 3]

El sistema deberá calcular una **cotización periódica** basada en criterios definidos y permitir a los usuarios operar comprando y vendiendo **tokens** [cite: 4]. Asimismo, deberá gestionar el **portfolio** de cada usuario, mostrando su posición actual y rentabilidad [cite: 5]. El sistema contemplará procesos de lectura, operaciones transaccionales e integración con APIs externas [cite: 6].

### Fuentes de Datos
* **WhoScored:** Se utilizará scraping para extraer datos detallados de rendimiento (pases, tiros, intercepciones, calificaciones, entre otros) [cite: 9, 10].
* **Football-Data.org:** Se utilizará la API oficial para obtener resultados de partidos, alineaciones y fixtures [cite: 11].

### Funcionalidades Requeridas
* Definir la estructura de datos a utilizar [cite: 13].
* Implementar un scrapper de jugadores [cite: 14].
* Simular una cotización de jugadores según estrategia [cite: 15].
* Obtener la cotización actual e histórica de un jugador [cite: 16, 17].

---

## 2. Dominio del Problema
El sistema representa un mercado de jugadores basado en criterios de valuación [cite: 19]:
* Cada jugador tiene una cotización que varía en el tiempo [cite: 20].
* Los usuarios pueden invertir comprando tokens de jugadores [cite: 21].
* El valor de la inversión cambia según la cotización actual [cite: 22].

> **Ejemplo:** Un usuario compra 10 tokens de un jugador a una cotización de 100 (inversión de 1000) [cite: 24, 25]. Si la cotización sube a 120, la posición vale 1200 (ganancia de 200) [cite: 27]. Si baja a 90, vale 900 (pérdida de 100) [cite: 28].

---

## 3. Funcionalidades Obligatorias

### 3.1 Catálogo de Jugadores [cite: 30]
* Integrar jugadores de las 5 ligas [cite: 33].
* Obtener datos desde APIs externas y persistirlos localmente o en BD cacheada [cite: 34, 35].

### 3.2 Sistema de Cotización [cite: 36]
La cotización debe recalcularse automáticamente (por ejemplo, semanalmente) y almacenarse en un historial consultable [cite: 39, 40].

**Criterio de Valuación:**
Se deben implementar al menos **dos estrategias configurables** de ponderación basadas en métricas de performance (minutos, goles, asistencias, tiros, pases, intercepciones, tarjetas, posición, rating, etc.) [cite: 43, 44]. Las métricas pueden tener impacto positivo o negativo [cite: 45].

**Ejemplo de Fórmula de Score (normalizado):** [cite: 52]
$$score = 0.25 \cdot goals + 0.15 \cdot assists + 0.10 \cdot shots + 0.10 \cdot keyPasses + 0.10 \cdot dribbles + 0.10 \cdot tackles + 0.20 \cdot rating$$ [cite: 55, 56]

**Conversión a Precio:**
$$valor = valorBase + (score \cdot factorEscala)$$ [cite: 58]

### 3.3 Mercado de Tokens [cite: 63]
* **Emisión:** Cada jugador tiene 100 tokens. Valor inicial: 1 crédito [cite: 65].
* **Dueño Inicial:** Un superusuario posee todos los tokens inicialmente [cite: 64, 69].
* **Operaciones:** El sistema debe validar disponibilidad de tokens (compra) o posesión de los mismos (venta) y actualizar saldos y posiciones [cite: 67, 68].

### 3.4 Portfolio del Usuario [cite: 70, 71]
Debe incluir: cantidad de tokens por jugador, precio promedio de compra, valor actual, ganancia/pérdida e historial de operaciones [cite: 74, 75, 76, 77, 78].

---

## 4. Requerimientos Funcionales (APIs) [cite: 79]

| Método | Endpoint | Descripción | Contexto |
| :--- | :--- | :--- | :--- |
| **GET** | `/players` | Listado de jugadores (filtros por liga, equipo, posición) | Catálogo |
| **GET** | `/players/:id` | Detalle de un jugador | Catálogo |
| **GET** | `/players/:id/quotes` | Historial de cotizaciones de un jugador | Cotización |
| **GET** | `/players/ranking` | Ranking de jugadores según estrategia activa | Cotización |
| **POST** | `/quotes/recalculate` | Recalcular cotizaciones (job manual) | Cotización |
| **POST** | `/orders/buy` | Comprar tokens de un jugador | Mercado |
| **POST** | `/orders/sell` | Vender tokens de un jugador | Mercado |
| **GET** | `/users/:id/portfolio` | Portfolio del usuario | Mercado |
| **GET** | `/users/:id/transactions` | Historial de operaciones del usuario | Mercado |

[cite: 81]

---

## 5. Requisitos No Funcionales [cite: 82]
* **Consistencia:** Transaccionalidad (operaciones atómicas), manejo de concurrencia e idempotencia [cite: 83, 85, 86, 88].
* **Persistencia y Cache:** No depender directamente de APIs externas en cada request; implementar cache [cite: 90, 91].
* **Scheduler:** Procesos automáticos para recalcular cotizaciones y sincronizar datos [cite: 92, 93, 94, 95].
* **Manejo de Errores y Observabilidad:** Gestionar fallas de negocio/integración y registrar logs de operaciones y auditoría [cite: 96, 97, 98, 100, 103].

---

## 6. Arquitectura Esperada [cite: 104]
El sistema debe organizarse en capas: **Controllers, Services, Repositories y Adapters** [cite: 105, 106, 107, 108, 109].

## 7. Integración con APIs Externas [cite: 110]
Consumir al menos una API externa de fútbol y tolerar fallas de la misma funcionando con datos locales [cite: 111, 112].

## 8. Escenarios de Prueba [cite: 113]
1. Construcción de la base de datos desde fuentes externas [cite: 115].
2. Evolución histórica de la cotización de un jugador por liga [cite: 116].
3. Simulación con 4 usuarios comprando 5 jugadores y visualización de la valoración actual [cite: 117].
