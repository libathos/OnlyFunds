# OnlyFunds 📈

**OnlyFunds** is a Compose Multiplatform stock-market playground that runs from a
single shared codebase on **Android, iOS, Desktop (JVM), and the Web (Wasm & JS)**.

It shows the most expensive stocks on the market in a live, self-refreshing list, lets you dive into the
price history of any of them, set price alerts, and answer the question every investor keeps asking:
*"how much would I have made if I had bought this back then?"* — and even *"would I have done better if a
robot had traded it for me?"*

> ⚠️ OnlyFunds is a demo/learning project. Nothing in it is financial advice, and no real orders are ever
> placed — the "trading" is a simulation over historical prices.

---

## ✨ What OnlyFunds does

### 💸 Live "Top 10 Most Expensive Stocks" list
- Fetches real-time quotes for a curated basket of 16 high-priced symbols (`BRK.A`, `NVR`, `SEB`, `BKNG`,
  `AAPL`, `NVDA`, `MSFT`, …) **in parallel**, then ranks them by price and shows the top 10.
- **Auto-refreshes every 15 seconds.** A thin progress bar animates down to the next refresh so you always
  know how fresh the numbers are, and a spinner marks the in-flight request.
- **Price-flash feedback:** each row remembers its previous price, so the price briefly flashes green when
  it ticked **up** and red when it ticked **down** between polls, then fades back — and the new number
  slides in from the direction the price moved.
- Polling is **lifecycle-aware**: it starts when the list is on screen and stops as soon as you navigate to
  the chart, so no requests (and no API quota) are wasted in the background.
- Partial failures are tolerated — if some symbols fail, the rest are still ranked and displayed; only a
  total failure shows an error state with a **Retry** action.

### 📊 Interactive price-history chart
- Tap any stock to open its history chart, drawn from scratch on a Compose `Canvas` (no chart library).
- **Six time frames**: `1D`, `1W`, `1M`, `3M`, `1Y`, `5Y` — each with its own candle resolution
  (5-minute, 30-minute, daily, weekly).
- Gradient-filled price line, plus the **period change in $ and %**, the latest price, and the **period
  low/high** labels.
- **SMA overlay:** a 20-period Simple Moving Average is drawn as a dashed amber line on top of the price
  line. On short series the period automatically shrinks so the overlay stays useful instead of disappearing.
- **Maximize mode:** a full-screen chart dialog that **locks the device to landscape** for a wide, detailed
  view, with the time-frame selector still available. The rest of the app is locked to portrait.

### 🔔 Price alerts
- From a stock's chart, set a target price with **"Alert"** — *"notify me when AAPL falls to or below X"*.
- Alerts live in a shared, in-memory `PriceAlertStore` keyed by symbol, so an alert created on the chart
  screen is **evaluated by the live poller** on the list screen.
- When the condition holds, a **snackbar** pops up on the list screen on every poll, so you keep being
  reminded until you change or clear the alert.
- The domain layer supports both `BELOW` and `ABOVE` alert directions.

### 🤔 "What-if" profit calculator
- Hit **"What-if"**, then **tap any point on the chart** to pick your imaginary buy date and buy price.
- Enter a quantity and OnlyFunds computes, against the *current* live quote:
  - amount invested, current value,
  - profit/loss in **$** and **%**, colour-coded green/red.
- Everything is derived from the candle you tapped, so you can compare different entry points in seconds.

### 🤖 Auto-trade on SMA cross (strategy backtest)
- In the what-if dialog, flip the **"Auto trade on SMA cross"** switch to run a **backtest** of the classic
  moving-average crossover strategy over the exact chart you are looking at:
  - close **above** the SMA → buy with all available cash,
  - close **below** the SMA → sell the whole position.
- The strategy starts with the **same money** as the buy-and-hold scenario, so results are directly comparable.
- You get the strategy's final value, P/L in $ and %, the **number of buys and sales**, whether it ended
  **"Still holding"** or **"Out of the market"**, and a plain-language **verdict**:
  *"Auto trading on SMA cross wins by $X"* vs *"Buy-and-hold wins by $X"*.

### 🎨 Polish & platform behaviour
- Custom dark **navy/mint** Material 3 theme with dedicated positive/negative/SMA colours.
- Animated content transitions, snackbars, loading and error states everywhere.
- Screen-orientation locking implemented per platform via `expect`/`actual` (portrait for the app,
  landscape for the maximized chart), with sensible no-ops on Desktop and Web.

---

## 🧱 Architecture

OnlyFunds is a three-module, unidirectional-data-flow (**MVI**) Kotlin Multiplatform project:

```
:composeApp   UI — Compose Multiplatform screens, ViewModels, UiProviders (reducers), UiStates
     │
     ▼
:domain       Business logic — use cases, domain models, mappers, in-memory stores
     │
     ▼
:network      Data — Ktor clients, DTOs, Finnhub & Yahoo services, NetworkResponse
```

**`:composeApp`** — one screen per package (`topStocksScreen`, `stockChartScreen`), each with:
- an `Action` (user intent) → `ViewModel` → `Mutation` → `UiProvider.reduce()` → immutable `UiState` loop,
- pure, testable `UiProvider` reducers that also do all formatting, so composables only render strings,
- stateless `...Content` composables with `@Preview`s, wrapped by thin stateful screen composables.

**`:domain`** — framework-free logic exposed as `operator fun invoke` use cases:
`GetQuoteUseCase`, `GetStockCandlesUseCase`, `CalculateSmaUseCase`, `BacktestSmaCrossUseCase`,
`CalculateWhatIfUseCase`, `EvaluatePriceAlertUseCase`, plus `PriceAlertStore` and the domain models.
Time is always passed in (`nowEpochSeconds`), never read from a clock inside a use case, which keeps
everything deterministic and unit-testable.

**`:network`** — Ktor 3 clients with `ContentNegotiation`/kotlinx-serialization and logging, one HTTP engine
per platform (OkHttp / Darwin / CIO / JS), and a `NetworkResponse<T>` sealed result type so no exception
ever escapes the data layer.

### Data sources
| Data | Provider | Notes |
|---|---|---|
| Live quotes | **Finnhub** `/quote` | Requires **your API key** (see below). |
| Candles / history | **Finnhub** `/stock/candle`, falling back to **Yahoo Finance** `v8/finance/chart` | Finnhub's candle endpoint is premium-gated on free plans, so `GetStockCandlesUseCase` automatically falls back to Yahoo's free, key-less chart endpoint whenever Finnhub errors or returns no data. Symbols are translated (`BRK.A` → `BRK-A`). |

---

## 🔑 Getting and storing your Finnhub API key

Live quotes come from [finnhub.io](https://finnhub.io), which requires a personal API token. **The key is
never committed to this repository** — the build reads it from your machine and generates it into code at
compile time.

### 1. Create a free Finnhub API key
1. Go to **https://finnhub.io/register** and create a free account (email + password).
2. Confirm your email and log in — you land on the **Dashboard**.
3. Your **API key** (token) is shown right there under *"API Key"*. Copy it.
   You can always find or rotate it at **https://finnhub.io/dashboard**.

The free tier is enough for OnlyFunds (`/quote` is included; `/stock/candle` is premium, which is exactly
why the app falls back to Yahoo for history).

### 2. Store the key — option A: `local.properties` (recommended)
Open (or create) the **`local.properties`** file in the project root and add:

```properties
finnhub.api.key=YOUR_FINNHUB_API_KEY_HERE
```

`local.properties` is listed in `.gitignore`, so your key stays on your machine and can never be pushed by
accident. This is also where the Android SDK path lives, so the file most likely already exists:

```properties
sdk.dir=/Users/you/Library/Android/sdk
finnhub.api.key=YOUR_FINNHUB_API_KEY_HERE
```

### 3. Store the key — option B: environment variable (CI-friendly)
If you would rather not keep the key in a file (e.g. on a build server), export it instead:

```shell
# macOS / Linux
export FINNHUB_API_KEY=YOUR_FINNHUB_API_KEY_HERE
```

```shell
# Windows (PowerShell)
$env:FINNHUB_API_KEY = "YOUR_FINNHUB_API_KEY_HERE"
```

`local.properties` wins if both are present; the environment variable is the fallback.

### 4. How it is wired (and why it is safe)
- `network/build.gradle.kts` reads `finnhub.api.key` from `local.properties`, else the `FINNHUB_API_KEY`
  environment variable, else falls back to an empty string.
- The `generateFinnhubSecrets` Gradle task generates
  `network/build/generated/finnhub/kotlin/io/onlyfunds/network/FinnhubSecrets.kt` containing an
  `internal object FinnhubSecrets { const val API_KEY }`, and registers it as a `commonMain` source dir.
- `FinnhubApiClient` appends that value as the `token` query parameter on every request, so no service or
  use case ever has to know about the secret.
- The generated file lives under `build/` (git-ignored) and `FinnhubSecrets` is `internal`, so the key never
  reaches version control.

> 🔁 **After changing the key**, re-sync/rebuild the project (the task is cached on the key value, so a
> Gradle sync is enough) — e.g. `./gradlew :network:generateFinnhubSecrets`.

> ❗ **No key?** The app still builds and runs: history charts keep working through the Yahoo fallback, but
> the live quote list will show an error/`401` state because `/quote` requires a token.

---

## 🚀 Build and run

Requirements: **JDK 17+**, Android SDK (compileSdk 36, minSdk 29), and **Xcode** for the iOS target.
Gradle 8.14.3 comes with the wrapper. Kotlin 2.3.20 · Compose Multiplatform 1.10.3 · Ktor 3.2.3.

### Android
```shell
./gradlew :composeApp:assembleDebug          # macOS/Linux
.\gradlew.bat :composeApp:assembleDebug      # Windows
```
Or just pick the `composeApp` run configuration in Android Studio.

### Desktop (JVM)
```shell
./gradlew :composeApp:run                    # macOS/Linux
.\gradlew.bat :composeApp:run                # Windows
```
Native installers (`.dmg`, `.msi`, `.deb`) can be built with `./gradlew :composeApp:packageDistributionForCurrentOS`.

### Web
```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Wasm — faster, modern browsers
./gradlew :composeApp:jsBrowserDevelopmentRun       # JS  — supports older browsers
```

### iOS
Open the [`/iosApp`](./iosApp) directory in Xcode and run, or use the iOS run configuration in
Android Studio / Fleet.

### Tests
```shell
./gradlew :composeApp:jvmTest
```
The shared tests in [`composeApp/src/commonTest`](./composeApp/src/commonTest) cover the SMA overlay
(`SmaOverlayTest`), the SMA-cross backtest incl. edge cases (`SmaCrossBacktestTest`), and the orientation
contract (`ScreenOrientationTest`).

---

## 🗂 Project layout

```
composeApp/
  src/commonMain/kotlin/compose/demo/onlyfunds/
    application/        App entry composable, formatting helpers, orientation expect/actual
    topStocksScreen/    Live top-10 list: composables + MVI (ViewModel, UiProvider, UiState)
    stockChartScreen/   Chart, maximized chart, alert & what-if dialogs + MVI
    theme/              OnlyFunds Material 3 dark theme and custom colours
  src/{androidMain,iosMain,jvmMain,webMain}/   Platform entry points & actuals
  src/commonTest/       Shared unit tests
domain/
  src/commonMain/kotlin/io/onlyfunds/domain/
    model/  usecases/  mapper/  store/
network/
  src/commonMain/kotlin/io/onlyfunds/network/
    FinnhubApiClient / FinnhubConfig / QuoteService / CandleService
    YahooApiClient   / YahooConfig   / YahooChartService
    NetworkResponse and DTOs
iosApp/                 SwiftUI host for the shared Compose UI
```

