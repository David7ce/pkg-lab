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
    override val howItWorksBody =
        "El sueño pasa por ciclos de unos 90 minutos. Despertar en mitad de uno deja una sensación de " +
            "aturdimiento; despertar entre dos es mucho más fácil. La app cuenta hacia atrás (o hacia delante) " +
            "en pasos de 90 minutos y suma el tiempo que sueles tardar en dormirte. La duración del ciclo varía " +
            "de una persona a otra, así que toma las horas como una guía. Las caras indican cómo se compara cada " +
            "opción con el sueño recomendado para tu edad."
    override val gotIt = "Entendido"
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
    override val howItWorksBody =
        "Le sommeil se déroule en cycles d’environ 90 minutes. Se réveiller au milieu d’un cycle laisse une " +
            "sensation de lourdeur ; se réveiller entre deux est bien plus facile. L’application compte à rebours " +
            "(ou en avant) par tranches de 90 minutes et ajoute le temps dont vous avez besoin pour vous endormir. " +
            "La durée d’un cycle varie d’une personne à l’autre : considérez ces heures comme un repère. Les visages " +
            "indiquent comment chaque option se situe par rapport au sommeil recommandé pour votre âge."
    override val gotIt = "Compris"
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
    override val howItWorksBody =
        "Schlaf verläuft in Zyklen von etwa 90 Minuten. Mitten in einem Zyklus aufzuwachen macht benommen; " +
            "zwischen zwei Zyklen aufzuwachen fällt viel leichter. Die App rechnet in 90-Minuten-Schritten zurück " +
            "(oder vor) und addiert die Zeit, die du zum Einschlafen brauchst. Die Zykluslänge ist von Mensch zu " +
            "Mensch verschieden, die Zeiten sind daher ein Richtwert. Die Gesichter zeigen, wie jede Option im " +
            "Vergleich zum empfohlenen Schlaf für dein Alter abschneidet."
    override val gotIt = "Verstanden"
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
    override val howItWorksBody =
        "O sono passa por ciclos de cerca de 90 minutos. Acordar no meio de um deixa a sensação de " +
            "atordoamento; acordar entre dois é bem mais fácil. O app conta para trás (ou para frente) em passos " +
            "de 90 minutos e soma o tempo que você costuma levar para adormecer. A duração do ciclo varia de " +
            "pessoa para pessoa, então use os horários como referência. Os rostos mostram como cada opção se " +
            "compara ao sono recomendado para a sua idade."
    override val gotIt = "Entendi"
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
    override val howItWorksBody =
        "Il sonno attraversa cicli di circa 90 minuti. Svegliarsi nel mezzo di uno lascia storditi; " +
            "svegliarsi tra due è molto più facile. L’app conta all’indietro (o in avanti) a passi di 90 minuti " +
            "e aggiunge il tempo che di solito serve per addormentarsi. La durata del ciclo varia da persona a " +
            "persona, quindi considera gli orari come un’indicazione. I volti mostrano come ogni opzione si " +
            "confronta con il sonno consigliato per la tua età."
    override val gotIt = "Capito"
    override val back = "Indietro"
    override val close = "Chiudi"
    override val cancel = "Annulla"
    override val delete = "Elimina"
    override val selectAll = "Seleziona tutto"
}
