package com.david7ce.sleepcycles.ui

/** Strings for an ISO 639 language code ("pt-BR" and "ES" work); English for anything not translated. */
fun stringsFor(languageCode: String): Strings = when (languageCode.lowercase().take(2)) {
    "es" -> EsStrings
    "fr" -> FrStrings
    "de" -> DeStrings
    "pt" -> PtStrings
    "it" -> ItStrings
    else -> EnStrings
}

object EnStrings : Strings()

object EsStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Despertar a las"
    override val goToBedAt = "Dormir a las"
    override val sleepNow = "Dormir ahora"
    override val resultsWake = "Acuéstate a una de estas horas"
    override val resultsBed = "Despierta a una de estas horas"
    override fun hint(latency: Int) = "1 ciclo = 90 min · incluye $latency min para dormirse"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n ciclos") + " · $hours h"
    override val hourLabel = "Hora"
    override val minuteLabel = "Min"
    override val ratingGood = "Dentro del rango recomendado para tu edad"
    override val ratingOk = "Ligeramente fuera del rango recomendado"
    override val ratingLow = "Lejos del rango recomendado"
    override val settings = "Ajustes"
    override val ageRange = "Rango de edad"
    override val ageChild = "Niño (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Mayor (65+) · 7–8 h"
    override val fallAsleepTime = "Tiempo para dormirse (minutos)"
    override val timeFormat = "Formato de hora"
    override val format24 = "24 horas"
    override val format12 = "12 horas (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val language = "Idioma"
    override val languageSystem = "Predeterminado del sistema"
    override fun versionLabel(version: String) = "Versión $version"
    override val howItWorks = "¿Cómo funciona?"
    override val howItWorksParagraphs = listOf(
        "El sueño no es uniforme: pasa por **ciclos repetidos de unos 90 minutos**, cada uno con sueño ligero, " +
            "sueño profundo y REM (cuando se produce la mayoría de los sueños).",
        "Despertar **en mitad de un ciclo**, sobre todo durante el sueño profundo, provoca inercia del sueño: " +
            "esa sensación de aturdimiento y pesadez que puede durar un rato. Despertar **entre ciclos**, cuando " +
            "el sueño es más ligero, hace que levantarse sea mucho más fácil.",
        "Esta calculadora cuenta hacia atrás o hacia delante en pasos de 90 minutos, más el tiempo que tardas " +
            "en dormirte (unos **15 minutos** de media, ajustable en ajustes), para que la alarma caiga al final " +
            "de un ciclo y no en medio.",
        "Cuántos ciclos necesitas depende de la edad. La National Sleep Foundation recomienda **7–9 horas** " +
            "(unos 5–6 ciclos) para adultos, más para adolescentes y niños, y algo menos para mayores. Cada " +
            "tarjeta muestra una cara según lo cerca que está esa opción de tu rango recomendado.",
    )
    override val howItWorksNote =
        "90 minutos es una media: los ciclos reales van de unos 70 a 120 minutos y varían cada noche. Toma las " +
            "sugerencias como buenos objetivos, no como ciencia exacta."
    override val gotIt = "Entendido"
    override val done = "Hecho"
    override val alarmHint = "Toca una hora para guardarla como alarma en tu app de reloj"
    override val pickTime = "Elegir la hora con el reloj"
    override val noClockApp = "No se encontró ninguna app de reloj"
    override val back = "Atrás"
    override val close = "Cerrar"
    override val cancel = "Cancelar"
    override val delete = "Eliminar"
    override val selectAll = "Seleccionar todo"
}

object FrStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Me réveiller à"
    override val goToBedAt = "Me coucher à"
    override val sleepNow = "Dormir maintenant"
    override val resultsWake = "Couchez-vous à l’une de ces heures"
    override val resultsBed = "Réveillez-vous à l’une de ces heures"
    override fun hint(latency: Int) = "1 cycle = 90 min · inclut $latency min pour s’endormir"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 cycle" else "$n cycles") + " · $hours h"
    override val hourLabel = "Heure"
    override val minuteLabel = "Min"
    override val ratingGood = "Dans la plage recommandée pour votre âge"
    override val ratingOk = "Légèrement hors de la plage recommandée"
    override val ratingLow = "Loin de la plage recommandée"
    override val settings = "Réglages"
    override val ageRange = "Tranche d’âge"
    override val ageChild = "Enfant (6–13) · 9–11 h"
    override val ageTeen = "Ado (14–17) · 8–10 h"
    override val ageAdult = "Adulte (18–64) · 7–9 h"
    override val ageSenior = "Senior (65+) · 7–8 h"
    override val fallAsleepTime = "Temps pour s’endormir (minutes)"
    override val timeFormat = "Format de l’heure"
    override val format24 = "24 heures"
    override val format12 = "12 heures (AM/PM)"
    override val theme = "Thème"
    override val themeSystem = "Système"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val language = "Langue"
    override val languageSystem = "Par défaut du système"
    override fun versionLabel(version: String) = "Version $version"
    override val howItWorks = "Comment ça marche ?"
    override val howItWorksParagraphs = listOf(
        "Le sommeil n’est pas uniforme : il traverse des **cycles répétés d’environ 90 minutes**, chacun passant " +
            "par le sommeil léger, le sommeil profond et le sommeil paradoxal (où survient l’essentiel des rêves).",
        "Se réveiller **au milieu d’un cycle**, surtout pendant le sommeil profond, provoque une inertie du " +
            "sommeil : cette sensation de lourdeur et d’engourdissement qui peut durer un moment. Se réveiller " +
            "**entre deux cycles**, quand le sommeil est le plus léger, rend le lever bien plus facile.",
        "Ce calculateur compte à rebours ou en avant par tranches de 90 minutes, plus le temps qu’il vous faut " +
            "pour vous endormir (environ **15 minutes** en moyenne, réglable dans les réglages), pour que votre " +
            "alarme tombe à la fin d’un cycle et non au milieu.",
        "Le nombre de cycles dont vous avez besoin dépend de l’âge. La National Sleep Foundation recommande " +
            "**7 à 9 heures** (environ 5 à 6 cycles) pour les adultes, plus pour les adolescents et les enfants, " +
            "un peu moins pour les seniors. Chaque carte affiche un visage selon la proximité de cette option " +
            "avec votre plage recommandée.",
    )
    override val howItWorksNote =
        "90 minutes est une moyenne : les cycles réels vont d’environ 70 à 120 minutes et varient d’une nuit à " +
            "l’autre. Considérez les suggestions comme de bons objectifs, pas comme une science exacte."
    override val gotIt = "Compris"
    override val done = "Terminé"
    override val alarmHint = "Touchez une heure pour l’enregistrer comme alarme dans votre application Horloge"
    override val pickTime = "Choisir l’heure avec l’horloge"
    override val noClockApp = "Aucune application Horloge trouvée"
    override val back = "Retour"
    override val close = "Fermer"
    override val cancel = "Annuler"
    override val delete = "Supprimer"
    override val selectAll = "Tout sélectionner"
}

object DeStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Aufwachen um"
    override val goToBedAt = "Schlafen um"
    override val sleepNow = "Jetzt schlafen"
    override val resultsWake = "Geh zu einer dieser Zeiten schlafen"
    override val resultsBed = "Wach zu einer dieser Zeiten auf"
    override fun hint(latency: Int) = "1 Zyklus = 90 Min. · enthält $latency Min. zum Einschlafen"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 Zyklus" else "$n Zyklen") + " · $hours Std."
    override val hourLabel = "Stunde"
    override val minuteLabel = "Min."
    override val ratingGood = "Im empfohlenen Bereich für dein Alter"
    override val ratingOk = "Leicht außerhalb des empfohlenen Bereichs"
    override val ratingLow = "Weit vom empfohlenen Bereich entfernt"
    override val settings = "Einstellungen"
    override val ageRange = "Altersgruppe"
    override val ageChild = "Kind (6–13) · 9–11 Std."
    override val ageTeen = "Jugendlicher (14–17) · 8–10 Std."
    override val ageAdult = "Erwachsener (18–64) · 7–9 Std."
    override val ageSenior = "Senior (65+) · 7–8 Std."
    override val fallAsleepTime = "Zeit zum Einschlafen (Minuten)"
    override val timeFormat = "Zeitformat"
    override val format24 = "24-Stunden"
    override val format12 = "12-Stunden (AM/PM)"
    override val theme = "Design"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val language = "Sprache"
    override val languageSystem = "Systemstandard"
    override fun versionLabel(version: String) = "Version $version"
    override val howItWorks = "Wie funktioniert das?"
    override val howItWorksParagraphs = listOf(
        "Schlaf ist nicht gleichförmig: Er durchläuft sich wiederholende **Zyklen von etwa 90 Minuten**, jeder " +
            "mit leichtem Schlaf, Tiefschlaf und REM (wenn die meisten Träume entstehen).",
        "Wer **mitten in einem Zyklus** aufwacht, besonders im Tiefschlaf, erlebt Schlafträgheit: dieses " +
            "benommene, schwere Gefühl, das eine Weile anhalten kann. Wer **zwischen zwei Zyklen** aufwacht, " +
            "wenn der Schlaf am leichtesten ist, steht viel leichter auf.",
        "Dieser Rechner zählt in 90-Minuten-Schritten zurück oder vor und addiert die Zeit zum Einschlafen " +
            "(durchschnittlich etwa **15 Minuten**, in den Einstellungen anpassbar), damit dein Wecker am Ende " +
            "eines Zyklus klingelt und nicht mittendrin.",
        "Wie viele Zyklen du brauchst, hängt vom Alter ab. Die National Sleep Foundation empfiehlt für " +
            "Erwachsene **7–9 Stunden** (etwa 5–6 Zyklen), für Jugendliche und Kinder mehr, für Senioren etwas " +
            "weniger. Jede Karte zeigt ein Gesicht dafür, wie nah diese Option an deinem empfohlenen Bereich liegt.",
    )
    override val howItWorksNote =
        "90 Minuten sind ein Durchschnitt: echte Zyklen dauern etwa 70 bis 120 Minuten und schwanken von Nacht " +
            "zu Nacht. Sieh die Vorschläge als gute Richtwerte, nicht als exakte Wissenschaft."
    override val gotIt = "Verstanden"
    override val done = "Fertig"
    override val alarmHint = "Tippe auf eine Zeit, um sie als Wecker in deiner Uhr-App zu speichern"
    override val pickTime = "Zeit mit der Uhr wählen"
    override val noClockApp = "Keine Uhr-App gefunden"
    override val back = "Zurück"
    override val close = "Schließen"
    override val cancel = "Abbrechen"
    override val delete = "Löschen"
    override val selectAll = "Alle auswählen"
}

object PtStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Acordar às"
    override val goToBedAt = "Dormir às"
    override val sleepNow = "Dormir agora"
    override val resultsWake = "Durma em um destes horários"
    override val resultsBed = "Acorde em um destes horários"
    override fun hint(latency: Int) = "1 ciclo = 90 min · inclui $latency min para adormecer"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n ciclos") + " · $hours h"
    override val hourLabel = "Hora"
    override val minuteLabel = "Min"
    override val ratingGood = "Dentro da faixa recomendada para sua idade"
    override val ratingOk = "Um pouco fora da faixa recomendada"
    override val ratingLow = "Longe da faixa recomendada"
    override val settings = "Configurações"
    override val ageRange = "Faixa etária"
    override val ageChild = "Criança (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Idoso (65+) · 7–8 h"
    override val fallAsleepTime = "Tempo para adormecer (minutos)"
    override val timeFormat = "Formato da hora"
    override val format24 = "24 horas"
    override val format12 = "12 horas (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Escuro"
    override val language = "Idioma"
    override val languageSystem = "Padrão do sistema"
    override fun versionLabel(version: String) = "Versão $version"
    override val howItWorks = "Como funciona?"
    override val howItWorksParagraphs = listOf(
        "O sono não é uniforme: ele passa por **ciclos repetidos de cerca de 90 minutos**, cada um com sono " +
            "leve, sono profundo e REM (quando ocorre a maior parte dos sonhos).",
        "Acordar **no meio de um ciclo**, principalmente durante o sono profundo, causa inércia do sono: " +
            "aquela sensação de atordoamento e peso que pode durar um tempo. Acordar **entre ciclos**, quando " +
            "o sono está mais leve, torna levantar-se bem mais fácil.",
        "Esta calculadora conta para trás ou para frente em passos de 90 minutos, mais o tempo que você leva " +
            "para adormecer (cerca de **15 minutos** em média, ajustável nas configurações), para que o alarme " +
            "toque no fim de um ciclo e não no meio.",
        "Quantos ciclos você precisa depende da idade. A National Sleep Foundation recomenda **7–9 horas** " +
            "(cerca de 5–6 ciclos) para adultos, mais para adolescentes e crianças e um pouco menos para " +
            "idosos. Cada cartão mostra um rosto conforme a proximidade dessa opção da sua faixa recomendada.",
    )
    override val howItWorksNote =
        "90 minutos é uma média: os ciclos reais vão de cerca de 70 a 120 minutos e variam de noite para " +
            "noite. Use as sugestões como boas metas, não como ciência exata."
    override val gotIt = "Entendi"
    override val done = "Concluído"
    override val alarmHint = "Toque em um horário para salvá-lo como alarme no seu app de relógio"
    override val pickTime = "Escolher a hora com o relógio"
    override val noClockApp = "Nenhum app de relógio encontrado"
    override val back = "Voltar"
    override val close = "Fechar"
    override val cancel = "Cancelar"
    override val delete = "Excluir"
    override val selectAll = "Selecionar tudo"
}

object ItStrings : Strings() {
    override val decimalSeparator = ','
    override val wakeUpAt = "Svegliarmi alle"
    override val goToBedAt = "Dormire alle"
    override val sleepNow = "Dormi ora"
    override val resultsWake = "Vai a letto a uno di questi orari"
    override val resultsBed = "Svegliati a uno di questi orari"
    override fun hint(latency: Int) = "1 ciclo = 90 min · include $latency min per addormentarsi"
    override fun cycles(n: Int, hours: String) = (if (n == 1) "1 ciclo" else "$n cicli") + " · $hours h"
    override val hourLabel = "Ora"
    override val minuteLabel = "Min"
    override val ratingGood = "Entro l’intervallo consigliato per la tua età"
    override val ratingOk = "Leggermente fuori dall’intervallo consigliato"
    override val ratingLow = "Lontano dall’intervallo consigliato"
    override val settings = "Impostazioni"
    override val ageRange = "Fascia d’età"
    override val ageChild = "Bambino (6–13) · 9–11 h"
    override val ageTeen = "Adolescente (14–17) · 8–10 h"
    override val ageAdult = "Adulto (18–64) · 7–9 h"
    override val ageSenior = "Anziano (65+) · 7–8 h"
    override val fallAsleepTime = "Tempo per addormentarsi (minuti)"
    override val timeFormat = "Formato dell’ora"
    override val format24 = "24 ore"
    override val format12 = "12 ore (AM/PM)"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Chiaro"
    override val themeDark = "Scuro"
    override val language = "Lingua"
    override val languageSystem = "Predefinita di sistema"
    override fun versionLabel(version: String) = "Versione $version"
    override val howItWorks = "Come funziona?"
    override val howItWorksParagraphs = listOf(
        "Il sonno non è uniforme: attraversa **cicli ripetuti di circa 90 minuti**, ciascuno con sonno " +
            "leggero, sonno profondo e REM (quando avviene la maggior parte dei sogni).",
        "Svegliarsi **a metà di un ciclo**, soprattutto durante il sonno profondo, provoca inerzia del sonno: " +
            "quella sensazione di stordimento e pesantezza che può durare a lungo. Svegliarsi **tra due cicli**, " +
            "quando il sonno è più leggero, rende l’alzarsi molto più facile.",
        "Questo calcolatore conta all’indietro o in avanti a passi di 90 minuti, più il tempo che serve per " +
            "addormentarsi (circa **15 minuti** in media, regolabile nelle impostazioni), così la sveglia suona " +
            "alla fine di un ciclo e non nel mezzo.",
        "Quanti cicli servono dipende dall’età. La National Sleep Foundation raccomanda **7–9 ore** (circa " +
            "5–6 cicli) per gli adulti, di più per adolescenti e bambini, un po’ meno per gli anziani. Ogni " +
            "scheda mostra un volto in base a quanto l’opzione è vicina al tuo intervallo consigliato.",
    )
    override val howItWorksNote =
        "90 minuti è una media: i cicli reali vanno da circa 70 a 120 minuti e variano da notte a notte. " +
            "Considera i suggerimenti come buoni obiettivi, non come scienza esatta."
    override val gotIt = "Capito"
    override val done = "Fatto"
    override val alarmHint = "Tocca un orario per salvarlo come sveglia nella tua app Orologio"
    override val pickTime = "Scegli l’ora con l’orologio"
    override val noClockApp = "Nessuna app Orologio trovata"
    override val back = "Indietro"
    override val close = "Chiudi"
    override val cancel = "Annulla"
    override val delete = "Elimina"
    override val selectAll = "Seleziona tutto"
}
