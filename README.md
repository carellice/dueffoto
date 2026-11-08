<div align="center">

<img src="docs/icon.png" width="128" alt="Icona di Dueffoto">

# Dueffoto

**Fotocamere che non esistono più, nel tuo telefono.**

Foto e video nello stile delle vecchie fotocamere a pellicola, oppure con un look da cinema moderno.

<a href="https://github.com/carellice/dueffoto/releases/latest"><img src="https://img.shields.io/github/v/release/carellice/dueffoto?label=Scarica%20APK&style=for-the-badge&color=E8642C&labelColor=14110E" alt="Scarica l'APK"></a>

<img src="https://img.shields.io/badge/Android-8.0%2B-C8A15A?style=flat-square&labelColor=14110E" alt="Android 8.0+">
<img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-C8A15A?style=flat-square&labelColor=14110E" alt="Kotlin e Jetpack Compose">

</div>

<p align="center">
  <img src="docs/camera.jpg" width="19%" alt="Fotocamera con Kodak Gold">
  <img src="docs/moody-camera.jpg" width="19%" alt="Modo Moody">
  <img src="docs/frigo.jpg" width="19%" alt="Il frigo dei rullini">
  <img src="docs/pellicola.jpg" width="19%" alt="Bordo pellicola con perforazioni">
  <img src="docs/camera-oscura.jpg" width="19%" alt="Camera oscura">
</p>

## Cosa fa

- **27 fotocamere analogiche**, ognuna con la sua pellicola: Olympus Trip 35 con Kodak Gold, Canon AE-1 con Portra 400, Nikon FM2 con Tri-X, Leica M6 con HP5, Nikon F3 con Delta 3200, Polaroid SX-70 e 600, Instax Mini, Instamatic, LOMO LC-A e LC-Wide con LomoChrome Purple, Holga, Yashica T4, Olympus µ-II, Ricoh GR1, Canon Sure Shot, Minolta Riva, Pentax K1000, Pentax 17 mezzo formato, Mamiya RZ67 6×7, Contax T2 con CineStill 800T, la usa e getta con la data arancione, Velvia, Ektachrome, una pellicola scaduta, Zenit-E e Kodak Brownie.
- **Modo Moody**: 6 look scuri e cinematografici (Dusk, Oak Shade, Canyon, Last Light, Teal Room, Coastline) calibrati su foto di riferimento: neri chiusi, luci color crema, cieli ottanio, verdi oliva.
- **Modo cinema** con 11 color grading (Golden Hour, Teal & Orange, Indie Print, Bleach Bypass, Noir, Day for Night, Codice Verde, Pastello, Neon Night, Tre Strisce, Super 8), rapporti 16:9, 1.85 e 2.39, dose regolabile e HUD da cinepresa con timecode.
- **Il frigo dei rullini**: tutte le pellicole sugli scaffali (colore, bianco e nero, diapositive, istantanee, sperimentali, moody, cinema), ognuna con la sua illustrazione e l'anteprima della tua inquadratura già sviluppata.
- **Scegli prima o dopo.** Scegli la pellicola e scatti: la stampa va subito in Galleria. Oppure scatti con "Scegli dopo" e decidi in **camera oscura**.
- **Il mirino è già sviluppato**: vedi la pellicola, la grana, la vignettatura e la cornice mentre inquadri.
- **Stampe vere**: bordo bianco delle istantanee, formato quadrato, perforazioni e scritte arancioni del bordo pellicola, data delle compatte anni '90, fughe di luce della Holga.
- **Video** con la stessa pellicola, applicata come LUT 3D a ogni fotogramma.
- **Rullino**: gli originali restano nell'app, puoi svilupparli di nuovo con un'altra pellicola quando vuoi. In camera oscura tieni premuto sulla foto per vedere l'originale.
- Esposizione, flash, zoom con due dita e messa a fuoco col tocco.
- **Aggiornamenti dall'app**: Dueffoto controlla l'ultima release di questa repository, scarica l'APK e lo installa.

## Moody, prima e dopo

<p align="center"><img src="docs/moody-prima-dopo.jpg" width="60%" alt="Prima e dopo con Dusk, Oak Shade e Coastline"></p>

A sinistra una foto "da telefono", a destra sviluppata con Dusk, Oak Shade e Coastline (foto di esempio generate). I preset sono calibrati con `tools/calibrate.py`, che replica il motore colore dell'app e ne confronta le statistiche con le foto di riferimento.

## Installazione

Scarica `Dueffoto-x.y.z.apk` dall'[ultima release](https://github.com/carellice/dueffoto/releases/latest) e aprilo sul telefono (Android 8.0 o successivo). Gli aggiornamenti successivi si installano dall'app: tocca **ⓘ** in alto a destra.

## Come funziona

| File | Contenuto |
|---|---|
| `Film.kt` | Le pellicole e la "chimica" del colore: esposizione, bilanciamento, saturazione, curva, viraggio di ombre e luci, neri slavati |
| `Picker.kt` | La scelta della pellicola: schede con anteprima dal vivo e il frigo dei rullini |
| `Darkroom.kt` | Lo sviluppo: grana, vignettatura, fughe di luce, cornici, data e la LUT 3D per i video |
| `Camera.kt` | CameraX: fotogrammi del mirino, scatto e registrazione |
| `VideoLab.kt` | Sviluppo dei video con Media3 Transformer |
| `Roll.kt` | Rullino degli originali e salvataggio in Galleria |

Le illustrazioni di rullini, scatole, latte e filtri sono state generate con ChatGPT (`branding/film/`) e ritagliate con `tools/slice_sheets.py`.

Le foto vengono salvate in `Immagini/Dueffoto`, i video in `Film/Dueffoto`.

I nomi delle fotocamere e delle pellicole servono solo a descrivere lo stile imitato: Dueffoto non è collegata ai rispettivi marchi.

## Licenza

MIT — Flavio Ceccarelli
