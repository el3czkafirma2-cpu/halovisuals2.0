# Halo Visuals (HV) 2.0.0 — Fabric 1.21.4, client-side

Wszystkie funkcje są darmowe i odblokowane. ClickGUI: domyślnie `I` (zmiana: ClickGUI → Client → Click GUI → Key
albo komendą `/halovs keybind clickgui insert`).
Lewy klik = włącz/wyłącz, prawy klik lub strzałka = ustawienia, kółko myszy = przewijanie.
Konfiguracja: `.minecraft/config/halovisuals.json`.

## Jak dostać gotowy .jar (bez IDE)
**GitHub Actions:** utwórz repozytorium, wgraj całą zawartość folderu (razem z ukrytym `.github`), zakładka **Actions** →
workflow `build` → po zielonym znaczku pobierz artefakt `HaloVisuals-jar` (`halovisuals-2.0.0.jar`).
**Lokalnie:** JDK 21 + Gradle 8.12+ → `gradle build` → `build/libs/halovisuals-2.0.0.jar`.
Do folderu `mods` wrzuć też Fabric API dla 1.21.4.

## Komendy czatu (/halovs)
- `/halovs config load <kod|nazwa>` – wczytuje config z kodu `HALO-1234` (zapis lokalny), z długiego kodu `HALO-...` albo z zapisanej nazwy
- `/halovs config export` – generuje kody (ID + długi kod, kopiuje do schowka)
- `/halovs config save <nazwa>` / `/halovs config list`
- `/halovs keybind clickgui <klawisz>` – np. `insert`, `i`, `shift`, `rshift`, `ctrl`, `alt`, `f6`, `home`

### Kody configów — jak to działa
Nie ma serwera w chmurze, więc kody działają bez niego:
- **ID** `HALO-7892` – zapis na Twoim komputerze (`config/halovisuals/codes`).
- **Długi kod** `HALO-eJx...` – to sam, skompresowany config; wklej go komuś na Discordzie i wczyta go komendą lub w zakładce **Configs**.
Zakładka **Configs** w ClickGUI (obok wyszukiwarki): generowanie, kopiowanie, wczytywanie, zapis pod nazwą, lista, reset.

## Co nowego w 2.0
- Gładkie (antyaliasowane) zaokrąglone krawędzie całego UI, płynne animacje ClickGUI.
- Przesuwanie myszką (przy otwartym czacie) + suwak Scale: Watermark, Potions, Target HUD.
- Custom Sky (kopuła: gwiazdy + mgławica), Atmosphere z mgłą (gęstość/kolor), Color Saturation (Brightness + Saturation/Tint), Motion Blur.
- Halo Nametag nad głową, Damage Numbers (dokładne wartości), Swing Animations (6 stylów, szybkość), więcej Hit Sounds i wybór Kill Sound.
- Kill Effects: Burst, Lightning, Angel Abduction, Ragdoll.
- Cosmetics: nowe skrzydła 3D (pióra, kości, machanie) i animowana peleryna Halo Cloak.
- Discord Rich Presence: wklej Application ID do `config/halovisuals.json` (`_discordAppId`), w Discord Developer Portal wgraj Art Assets `halo_logo` i `halo_small`.

## Ograniczenia (uczciwie)
- Color Saturation / Motion Blur to nakładki ekranowe (rozjaśnienie, tint, smugi), nie prawdziwe shadery.
- Ragdoll i Angel Abduction to stylizowane bryły (kończyny/duch), nie prawdziwy model gracza.
- Halo Nametag pokazuje się nad Tobą (opcjonalnie nad wszystkimi) i jako `[H]` w liście graczy.

## Jeśli gra wywala się przy starcie lub build się nie kompiluje
Najbardziej zależne od wersji elementy są w osobnych plikach. W `src/main/resources/halovisuals.mixins.json` usuń z listy `client`
mixin wskazany w logu (np. `FogMixin`, `HeldItemRendererMixin`, `LivingEntityMixin`) — reszta działa bez niego.
Jeśli kompilacja zgłasza błąd w `FogMixin.java`, możesz go po prostu skasować razem z wpisem w JSON (stracisz tylko mgłę).
