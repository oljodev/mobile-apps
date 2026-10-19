# 🍚 Rise – plan for første versjon

Rise er mini-appen for vekking og påminnelser i Salat. Senere kommer en kalender.

## Oversikt

- Alarmene ligger i **grupper**. Rise starter med «Vekking», «Trening» og «Annet».
- Gruppene kan få nytt navn og slettes, og man kan lage nye.
- Hver alarm kan skrus av og på.

## To typer

| Type | Hva skjer |
| --- | --- |
| **Alarm** | Ringer gjennom lydløs og Fokus, som Klokke-appen (AlarmKit). |
| **Varsel** | Et vanlig varsel med lyd. Kan også ha et forvarsel, f.eks. 15 minutter eller 1 time før. |

## Hver alarm har

- Navn, klokkeslett, gruppe og type.
- **Lyd:** valgt fra noen innebygde lyder.
- **Slumretid:** 5, 10 eller 15 minutter, eller ingen slumring.
- **«Sjekk at jeg er oppe»:** en bryter som står på som standard i Vekking-gruppen. Når den er på, ringer Rise igjen 5 minutter etter at alarmen er stoppet, helt til man åpner Rise og trykker «Jeg er oppe».

## Gjentakelse

- **Én gang:** dato og klokkeslett. Ikke «om 30 minutter»; til det brukes Klokke-appen.
- **Ukedager:** kryss av for mandag–søndag.
- **Avansert:**
  - annenhver uke
  - månedlig, f.eks. «første mandag» eller «den 15.»
  - start- og sluttdato
  - hopp over neste gang

## Senere

- Egne lyder på opptil 60 sekunder.
- Kalender.

## Tekniske valg

- **Begrensning på antall planlagte alarmer og varsler.** iOS begrenser hvor mange en app kan ha planlagt (64 varsler). Rise legger derfor inn de neste forekomstene som enkeltdatoer, og fyller på når appen åpnes.
- **«Sjekk at jeg er oppe»** er en reservealarm 5 minutter etter vekkealarmen, som avlyses når man trykker «Jeg er oppe». Stoppknappen på låseskjermen kan ikke fjernes.
- **Alt lagres lokalt** på telefonen.
