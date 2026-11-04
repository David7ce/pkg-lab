package com.david7ce.calistenia.logic

import com.david7ce.calistenia.model.Exercise

/**
 * How-to text for the built-in exercises in the five languages the UI translates (English is the
 * base stored in the catalog itself). Kept in code, not in the database: it is app content and a
 * later release can improve the wording without a migration. Lookup is by stable built-in id, so a
 * session or backup never needs to carry a translation.
 *
 * Custom exercises have a single description written by the user and are never translated.
 */
object BuiltinDescriptions {

    private val es: Map<String, String> = mapOf(
        "builtin:push-up" to
            "Empieza en plancha alta con las manos un poco más anchas que los hombros y el cuerpo en línea. " +
            "Baja el pecho entre las manos, con los codos a unos 45° del torso, y empuja para subir sin dejar " +
            "caer la cadera. Clave: aprieta los glúteos y activa el core para que pecho y cadera bajen juntos. " +
            "Hazlo más fácil con inclinación o rodillas; más difícil con un descenso lento o los pies elevados.",
        "builtin:knee-push-up" to
            "Arrodíllate con las manos bajo los hombros, las rodillas en el suelo y una línea recta de rodillas " +
            "a cabeza. Baja el pecho entre las manos y empuja sin doblarte por la cadera. Sirve para construir " +
            "el mismo patrón que la flexión completa; mantén el core activo para que la zona lumbar no se " +
            "arquee. Pasa a la flexión completa cuando puedas hacer varias series limpias.",
        "builtin:incline-push-up" to
            "Apoya las manos en un banco, mesa o escalón y aleja los pies hasta que el cuerpo quede recto. Baja " +
            "el pecho al borde y empuja. Cuanto más alta la superficie, más fácil; bájala a medida que ganes " +
            "fuerza. Mantén los codos hacia atrás unos 45° y el cuerpo en línea.",
        "builtin:diamond-push-up" to
            "Junta las manos bajo el pecho, con los índices y pulgares formando un diamante. Mantén los codos " +
            "cerca de las costillas al bajar y empuja para subir. Carga más el tríceps y la parte interna del " +
            "pecho. No dejes que la cadera suba ni que los hombros se acerquen a las orejas.",
        "builtin:archer-push-up" to
            "Coloca las manos muy abiertas y desplaza el peso sobre un brazo al bajar, manteniendo el otro casi " +
            "recto. Vuelve al centro y alterna lados. Es un paso hacia la flexión a una mano; mantén la cadera " +
            "cuadrada en vez de girar y deja que el codo que trabaja se quede cerca del cuerpo.",
        "builtin:pseudo-planche-push-up" to
            "Pon las manos a la altura de la cadera con los dedos hacia fuera e inclina los hombros hacia delante " +
            "sobre las manos. Baja y empuja manteniendo esa inclinación. Carga mucho los hombros y el core. " +
            "Mantén el cuerpo en línea y no dejes que la zona lumbar se hunda ni que los codos se abran.",
        "builtin:bench-dip" to
            "Siéntate en un banco, pon las manos a los lados de la cadera y desliza la cadera fuera del borde con " +
            "las piernas delante. Dobla los codos para bajar hasta que los brazos queden más o menos paralelos " +
            "al suelo y empuja. Mantén los hombros abajo y la espalda cerca del banco. No te hundas en los " +
            "hombros ni rebotes abajo.",
        "builtin:parallel-bar-dip" to
            "Apóyate en las barras paralelas con los brazos rectos, los hombros abajo y las piernas colgando o " +
            "dobladas. Baja hasta que los brazos queden más o menos paralelos al suelo y empuja. Inclínate un " +
            "poco hacia delante para el pecho o mantente recto para el tríceps. Sáltalo si te duelen los hombros " +
            "y progresa con fondos en banco.",
        "builtin:pike-push-up" to
            "Desde la posición de flexión, camina los pies hacia las manos y sube la cadera formando una V " +
            "invertida. Dobla los codos para bajar la cabeza al suelo entre las manos y empuja. Construye el " +
            "press de hombros para el pino. Mantén los codos hacia delante, sin abrirlos, y la cadera alta.",
        "builtin:handstand-push-up" to
            "Sube a un pino contra la pared con las manos un poco más anchas que los hombros. Baja la cabeza al " +
            "suelo con control y empuja para volver al pino recto. Inténtalo solo cuando aguantes el pino en " +
            "pared y hagas pike push-ups. Mantén el core firme y las costillas abajo en vez de arquear la espalda.",
        "builtin:dead-hang" to
            "Cuélgate de una barra con agarre prono, brazos rectos y los hombros ligeramente hacia abajo, lejos " +
            "de las orejas. Aguanta el tiempo que puedas respirando con normalidad. Construye fuerza de agarre y " +
            "resistencia de hombros. No te balances ni dejes que los hombros suban a las orejas; baja antes de " +
            "que el agarre falle del todo.",
        "builtin:scapular-pull-up" to
            "Cuélgate con los brazos rectos y lleva las escápulas hacia abajo y atrás para que el cuerpo suba unos " +
            "centímetros sin doblar los codos. Haz una pausa y baja con control. Enseña la primera parte de la " +
            "dominada. Mantén las costillas abajo y evita balancearte; piensa en mover las escápulas, no las manos.",
        "builtin:inverted-row" to
            "Coloca una barra a la altura de la cintura y cuélgate debajo con el cuerpo recto y los talones en el " +
            "suelo. Lleva el pecho a la barra apretando las escápulas y baja con control. Cuanto más baja la " +
            "barra, más difícil. Mantén la cadera arriba y el cuerpo en línea en vez de adelantar la barbilla.",
        "builtin:pull-up" to
            "Cuélgate de una barra con agarre prono un poco más ancho que los hombros. Lleva el pecho a la barra " +
            "llevando los codos hacia abajo y baja hasta el cuelgue completo con control. Evita balancearte o " +
            "hacer kipping. Si aún no puedes hacer una, usa negativas o remo invertido y sigue practicando el " +
            "recorrido completo.",
        "builtin:chin-up" to
            "Cuélgate con agarre supino a la anchura de los hombros. Sube hasta que la barbilla pase la barra y " +
            "baja del todo. El agarre supino usa más bíceps. Mantén los hombros activos y las costillas abajo en " +
            "vez de arquear la espalda para llegar arriba.",
        "builtin:negative-pull-up" to
            "Empieza arriba de la dominada, con la barbilla sobre la barra, saltando o subiendo con un escalón. " +
            "Baja lo más despacio posible, buscando de tres a cinco segundos, hasta estirar los brazos. Construye " +
            "fuerza para la primera dominada. Vuelve a subir con un escalón, nunca balanceándote.",
        "builtin:commando-pull-up" to
            "Cuélgate de una barra con agarre neutro, una mano delante de la otra. Sube hasta que un hombro pase " +
            "la barra, baja y alterna lados. Mantén el cuerpo cerca de la barra y evita girar. Es una variante " +
            "exigente; domina la dominada estándar primero.",
        "builtin:muscle-up" to
            "Cuélgate de la barra, tira con fuerza y pasa los codos por encima de la barra hasta un apoyo con los " +
            "brazos rectos. Baja con control o invierte el movimiento. Necesitas una dominada y un fondo fuertes. " +
            "Aprende la transición en una barra baja y con agarre falso antes de añadir altura o velocidad.",
        "builtin:squat" to
            "Ponte con los pies a la anchura de los hombros y las puntas ligeramente hacia fuera. Lleva la cadera " +
            "atrás y abajo, con el pecho arriba y las rodillas siguiendo la línea de los pies, y sube. Baja todo " +
            "lo que puedas sin que la zona lumbar se redondee. Empuja con todo el pie, no solo con las puntas.",
        "builtin:split-squat" to
            "Da un paso adelante y baja la rodilla de atrás hacia el suelo, con la espinilla delantera más o menos " +
            "vertical. Empuja con el pie delantero para subir. Mantén el torso erguido y la cadera cuadrada. La " +
            "rodilla de atrás debe acercarse al suelo sin golpearlo; apóyate en una pared o silla si el " +
            "equilibrio es el límite.",
        "builtin:bulgarian-split-squat" to
            "Apoya el empeine del pie de atrás en un banco y quédate de pie sobre la pierna delantera. Baja recto " +
            "hasta que el muslo delantero quede casi paralelo al suelo y empuja. Es un gran constructor de fuerza " +
            "a una pierna. Mantén la rodilla delantera sobre el pie y el torso alto; empieza con un banco bajo si " +
            "la posición es inestable.",
        "builtin:reverse-lunge" to
            "Ponte recto, da un paso atrás y baja las dos rodillas hasta que la de atrás quede justo por encima " +
            "del suelo. Empuja con el pie delantero para volver. Dar el paso atrás es más amable con las rodillas " +
            "que adelante. Mantén el torso erguido y la rodilla delantera sobre los dedos del pie.",
        "builtin:nordic-curl" to
            "Arrodíllate con los tobillos sujetos y el cuerpo en línea recta de rodillas a cabeza. Baja el torso " +
            "hacia delante lo más despacio posible usando los isquiotibiales y empuja con las manos si hace " +
            "falta. Es muy exigente. Empieza con poco recorrido y auméntalo con el tiempo; no dejes que la cadera " +
            "rompa la línea.",
        "builtin:glute-bridge" to
            "Túmbate boca arriba con las rodillas dobladas y los pies planos, cerca de la cadera. Empuja con los " +
            "talones y sube la cadera hasta que el cuerpo forme una línea de rodillas a hombros, y baja. Aprieta " +
            "los glúteos arriba. No arquees la zona lumbar para subir más.",
        "builtin:single-leg-glute-bridge" to
            "Túmbate boca arriba con un pie plano y la otra pierna estirada o la rodilla abrazada. Sube la cadera " +
            "con la pierna que trabaja, manteniendo la pelvis nivelada, y baja. Delata diferencias de fuerza " +
            "entre lados. No dejes que la cadera libre se caiga; reduce el recorrido antes de perder la alineación.",
        "builtin:calf-raise" to
            "Ponte con la parte delantera de los pies en un escalón y los talones fuera. Sube lo más alto posible " +
            "sobre las puntas, haz una pausa y baja los talones por debajo del escalón para estirar. Mantén las " +
            "rodillas rectas pero no bloqueadas. Muévete despacio en vez de rebotar y sujétate para mantener el " +
            "equilibrio.",
        "builtin:plank" to
            "Apoya los antebrazos en el suelo bajo los hombros y estira las piernas, con el cuerpo en una línea " +
            "recta. Activa el core, aprieta los glúteos y respira con normalidad. No dejes que la cadera se hunda " +
            "ni suba. Termina la serie cuando empiece a doler la zona lumbar, no cuando lo diga el reloj.",
        "builtin:side-plank" to
            "Túmbate de lado con el codo bajo el hombro y los pies apilados. Sube la cadera hasta que el cuerpo " +
            "forme una línea recta y aguanta. Mantén el hombro de arriba sobre el de abajo y no dejes que la " +
            "cadera se vaya atrás. Cambia de lado el mismo tiempo; apoya la rodilla de abajo si la cadera no se " +
            "mantiene.",
        "builtin:hollow-body-hold" to
            "Túmbate boca arriba, presiona la zona lumbar contra el suelo y levanta unos centímetros los " +
            "omóplatos y las piernas. Aguanta con las costillas abajo y los brazos junto a las orejas o cruzados " +
            "en el pecho. Si la zona lumbar se despega, dobla las rodillas o sube más las piernas hasta que se " +
            "quede pegada.",
        "builtin:leg-raise" to
            "Túmbate boca arriba con las piernas rectas y las manos al lado de la cadera o bajo los glúteos. Sube " +
            "las piernas hasta la vertical y bájalas despacio sin dejar que la zona lumbar se arquee. Dobla las " +
            "rodillas si la espalda empieza a levantarse. Un recorrido más corto con control vale más que uno " +
            "bajo con impulso.",
        "builtin:hanging-knee-raise" to
            "Cuélgate de una barra con los hombros activos. Lleva las rodillas al pecho, bascula un poco la pelvis " +
            "y baja con control. Evita balancearte. Si el cuerpo empieza a moverse, espera abajo a que se pare " +
            "antes de la siguiente repetición.",
        "builtin:hanging-leg-raise" to
            "Cuélgate de una barra y sube las piernas rectas a la altura de la cadera o más, llevando la pelvis " +
            "atrás arriba. Baja despacio sin balancearte. Es la versión difícil del levantamiento de rodillas. " +
            "Empieza con las rodillas dobladas y estíralas a medida que ganes control.",
        "builtin:dragon-flag" to
            "Túmbate en un banco y agárrate detrás de la cabeza; sube el cuerpo hasta los hombros y bájalo como una " +
            "línea recta con control. Mantén las costillas abajo y los glúteos apretados. Es un movimiento " +
            "avanzado. Empieza con versiones encogidas o negativas y nunca dejes que la zona lumbar se arquee.",
        "builtin:jumping-jack" to
            "De pie, erguido, con los brazos a los lados y los pies juntos. Salta abriendo los pies más que los " +
            "hombros mientras subes los brazos por encima de la cabeza, y vuelve al inicio en un solo ritmo. " +
            "Apoya en la punta de los pies, mantén las rodillas blandas y el core activo. Úsalo como " +
            "calentamiento: importan la frecuencia cardíaca y los hombros, no la velocidad. Si el impacto molesta " +
            "a las rodillas, abre los pies sin saltar.",
        "builtin:wall-handstand" to
            "Pon las manos a unos 15 o 20 cm de la pared, sube de una patada o caminando hasta que el pecho mire a " +
            "la pared y el cuerpo quede recto. Empuja con los hombros, mantén las costillas abajo y aguanta. Baja " +
            "con control. Cuando lo aguantes cómodo, practica de espaldas a la pared con los talones apoyados.",
        "builtin:handstand-hold" to
            "Sube a un pino libre con las manos a la anchura de los hombros, los dedos abiertos y los hombros " +
            "empujando hacia arriba. Encuentra el equilibrio con pequeños ajustes de dedos y muñecas, manteniendo " +
            "el cuerpo en línea. Practica cerca de una pared y aprende a salir hacia delante o de lado. Necesita " +
            "muchos intentos cortos, no uno largo.",
        "builtin:l-sit" to
            "Apóyate en paralelas o en el suelo con los brazos rectos, los hombros abajo y las piernas subidas a " +
            "90°. Aguanta con el core activo y los dedos de los pies estirados. Si las piernas no se mantienen, " +
            "dobla las rodillas o súbelas de una en una. No te encojas en los hombros; empuja el suelo.",
        "builtin:tuck-planche" to
            "Pon las manos a la altura de la cadera con los dedos hacia fuera, inclina los hombros sobre las manos " +
            "y sube las rodillas al pecho con la cadera alta. Aguanta con los brazos rectos y la parte alta de la " +
            "espalda redondeada. Es una progresión de plancha. Mantén la inclinación y no dejes que los pies " +
            "toquen el suelo entre intentos.",
        "builtin:tuck-front-lever" to
            "Cuélgate de una barra y lleva las escápulas abajo; después sube las rodillas al pecho e inclínate atrás " +
            "hasta que la espalda quede más o menos paralela al suelo. Aguanta con los brazos rectos y el cuerpo " +
            "bien encogido. Es una progresión de front lever. Evita que la cadera caiga y mantén los codos " +
            "bloqueados; abre el encogimiento solo cuando el agarre sea sólido.",
        "builtin:back-lever" to
            "Desde un German hang o una posición invertida en la barra, baja el cuerpo detrás de la barra con los " +
            "brazos rectos hasta que quede más o menos paralelo al suelo. Mantén los hombros activos y todo el " +
            "cuerpo firme. Es un movimiento avanzado. Progresa con versiones encogidas y straddle y calienta bien " +
            "los hombros.",
    )

    private val fr: Map<String, String> = mapOf(
        "builtin:push-up" to
            "Commence en planche haute, mains un peu plus larges que les épaules et corps aligné. Descends la " +
            "poitrine entre les mains, coudes à environ 45° du torse, puis pousse pour remonter sans laisser les " +
            "hanches s'affaisser. Repère : serre les fessiers et gaine le tronc pour que poitrine et hanches " +
            "descendent ensemble. Facilite avec un plan incliné ou les genoux ; complique avec une descente lente " +
            "ou les pieds surélevés.",
        "builtin:knee-push-up" to
            "Mets-toi à genoux, mains sous les épaules, genoux au sol et ligne droite des genoux à la tête. " +
            "Descends la poitrine entre les mains et pousse sans casser au niveau des hanches. C'est le même " +
            "schéma que la pompe complète ; garde le tronc gainé pour que le bas du dos ne se creuse pas. Passe à " +
            "la pompe complète quand tu peux faire plusieurs séries propres.",
        "builtin:incline-push-up" to
            "Pose les mains sur un banc, une table ou une marche et recule les pieds jusqu'à ce que le corps soit " +
            "droit. Descends la poitrine au bord puis pousse. Plus la surface est haute, plus c'est facile ; " +
            "descends-la au fur et à mesure. Garde les coudes vers l'arrière à environ 45° et le corps aligné.",
        "builtin:diamond-push-up" to
            "Rapproche les mains sous la poitrine, index et pouces formant un losange. Garde les coudes près des " +
            "côtes en descendant puis pousse. Le travail se déplace vers les triceps et l'intérieur des " +
            "pectoraux. Ne laisse pas les hanches monter ni les épaules se rapprocher des oreilles.",
        "builtin:archer-push-up" to
            "Écarte largement les mains et transfère le poids sur un bras en descendant, l'autre bras presque " +
            "tendu. Reviens au centre et alterne. C'est une étape vers la pompe à un bras ; garde les hanches " +
            "dans l'axe au lieu de tourner, et le coude actif près du corps.",
        "builtin:pseudo-planche-push-up" to
            "Place les mains au niveau des hanches, doigts vers l'extérieur, et incline les épaules vers l'avant " +
            "au-dessus des mains. Descends et pousse en gardant cette inclinaison. Les épaules et le tronc " +
            "travaillent beaucoup. Garde le corps aligné, sans laisser le bas du dos s'affaisser ni les coudes " +
            "s'écarter.",
        "builtin:bench-dip" to
            "Assieds-toi sur un banc, mains à côté des hanches, puis fais glisser les hanches hors du bord, jambes " +
            "devant. Plie les coudes pour descendre jusqu'à ce que les bras soient à peu près parallèles au sol, " +
            "puis pousse. Garde les épaules basses et le dos près du banc. Ne t'enfonce pas dans les épaules et ne " +
            "rebondis pas en bas.",
        "builtin:parallel-bar-dip" to
            "Suspends-toi aux barres parallèles, bras tendus, épaules basses et jambes pendantes ou pliées. " +
            "Descends jusqu'à ce que les bras soient à peu près parallèles au sol, puis pousse. Penche-toi " +
            "légèrement en avant pour les pectoraux ou reste droit pour les triceps. Évite si les épaules font mal " +
            "et progresse avec les dips sur banc.",
        "builtin:pike-push-up" to
            "Depuis la position de pompe, avance les pieds vers les mains et monte les hanches en V inversé. Plie " +
            "les coudes pour descendre la tête vers le sol entre les mains, puis pousse. Cela construit le " +
            "développé au-dessus de la tête pour le handstand. Garde les coudes vers l'avant, sans les écarter, et " +
            "les hanches hautes.",
        "builtin:handstand-push-up" to
            "Monte en handstand contre un mur, mains un peu plus larges que les épaules. Descends la tête vers le " +
            "sol avec contrôle puis pousse pour revenir au handstand tendu. Ne tente l'exercice que si tu tiens un " +
            "handstand au mur et fais des pompes en piqué. Garde le tronc gainé et les côtes basses au lieu de " +
            "creuser le dos.",
        "builtin:dead-hang" to
            "Suspends-toi à une barre en pronation, bras tendus et épaules légèrement tirées vers le bas, loin des " +
            "oreilles. Tiens en respirant normalement. Cela construit la force de préhension et l'endurance des " +
            "épaules. Ne te balance pas et ne laisse pas les épaules monter vers les oreilles ; descends avant que " +
            "la prise lâche complètement.",
        "builtin:scapular-pull-up" to
            "Suspends-toi bras tendus, puis tire les omoplates vers le bas et l'arrière pour que le corps monte de " +
            "quelques centimètres sans plier les coudes. Marque une pause puis redescends avec contrôle. Cela " +
            "enseigne le début de la traction. Garde les côtes basses et évite de te balancer ; pense à bouger " +
            "les omoplates, pas les mains.",
        "builtin:inverted-row" to
            "Place une barre à hauteur de taille et suspends-toi dessous, corps droit et talons au sol. Tire la " +
            "poitrine vers la barre en serrant les omoplates, puis redescends avec contrôle. Plus la barre est " +
            "basse, plus c'est difficile. Garde les hanches hautes et le corps aligné au lieu d'avancer le menton.",
        "builtin:pull-up" to
            "Suspends-toi à une barre en pronation, mains un peu plus larges que les épaules. Amène la poitrine " +
            "vers la barre en tirant les coudes vers le bas, puis redescends jusqu'à la suspension complète avec " +
            "contrôle. Évite le kipping et le balancement. Si tu n'en fais pas encore une, utilise des négatives " +
            "ou du rowing inversé et continue le mouvement complet.",
        "builtin:chin-up" to
            "Suspends-toi en supination, mains à largeur d'épaules. Tire jusqu'à ce que le menton dépasse la " +
            "barre, puis redescends complètement. La prise en supination sollicite davantage les biceps. Garde les " +
            "épaules actives et les côtes basses au lieu de creuser le dos pour arriver en haut.",
        "builtin:negative-pull-up" to
            "Commence en haut de la traction, menton au-dessus de la barre, en sautant ou en montant sur un " +
            "support. Descends le plus lentement possible, vise trois à cinq secondes, jusqu'à tendre les bras. " +
            "Cela construit la force pour la première traction. Remonte en t'aidant du support, jamais en te " +
            "balançant.",
        "builtin:commando-pull-up" to
            "Suspends-toi à une barre en prise neutre, une main devant l'autre. Tire jusqu'à ce qu'une épaule " +
            "passe au-dessus de la barre, redescends et alterne. Garde le corps près de la barre et évite de " +
            "tourner. C'est une variante exigeante ; maîtrise d'abord la traction classique.",
        "builtin:muscle-up" to
            "Suspends-toi à la barre, tire fort et passe les coudes au-dessus de la barre jusqu'à un appui bras " +
            "tendus. Redescends avec contrôle ou inverse le mouvement. Il faut une traction et un dip solides. " +
            "Apprends la transition sur une barre basse et avec une fausse prise avant d'ajouter de la hauteur ou " +
            "de la vitesse.",
        "builtin:squat" to
            "Place les pieds à largeur d'épaules, pointes légèrement vers l'extérieur. Recule et descends les " +
            "hanches, poitrine haute et genoux dans l'axe des pieds, puis remonte. Descends aussi bas que possible " +
            "sans arrondir le bas du dos. Pousse sur tout le pied, pas seulement sur les orteils.",
        "builtin:split-squat" to
            "Avance un pied et descends le genou arrière vers le sol, tibia avant à peu près vertical. Pousse sur " +
            "le pied avant pour remonter. Garde le buste droit et les hanches dans l'axe. Le genou arrière doit " +
            "s'approcher du sol sans le frapper ; appuie-toi à un mur ou une chaise si l'équilibre limite.",
        "builtin:bulgarian-split-squat" to
            "Pose le dessus du pied arrière sur un banc et tiens-toi sur la jambe avant. Descends à la verticale " +
            "jusqu'à ce que la cuisse avant soit presque parallèle au sol, puis pousse. Excellent exercice de " +
            "force sur une jambe. Garde le genou avant au-dessus du pied et le buste haut ; commence avec un banc " +
            "bas si la position est instable.",
        "builtin:reverse-lunge" to
            "Tiens-toi droit, fais un pas en arrière et descends les deux genoux jusqu'à ce que celui de derrière " +
            "soit juste au-dessus du sol. Pousse sur le pied avant pour revenir. Le pas arrière est plus doux pour " +
            "les genoux que le pas avant. Garde le buste droit et le genou avant dans l'axe des orteils.",
        "builtin:nordic-curl" to
            "Mets-toi à genoux, chevilles bloquées et corps aligné des genoux à la tête. Descends le buste vers " +
            "l'avant le plus lentement possible avec les ischio-jambiers, puis pousse avec les mains si besoin. " +
            "C'est très exigeant. Commence sur une petite amplitude et augmente-la progressivement ; ne laisse pas " +
            "les hanches casser la ligne.",
        "builtin:glute-bridge" to
            "Allonge-toi sur le dos, genoux pliés et pieds à plat près des hanches. Pousse sur les talons et monte " +
            "les hanches jusqu'à former une ligne des genoux aux épaules, puis redescends. Serre les fessiers en " +
            "haut. Ne creuse pas le bas du dos pour monter plus haut.",
        "builtin:single-leg-glute-bridge" to
            "Allonge-toi sur le dos, un pied à plat et l'autre jambe tendue ou le genou ramené. Monte les hanches " +
            "avec la jambe active en gardant le bassin horizontal, puis redescends. Cela révèle les différences de " +
            "force entre les côtés. Ne laisse pas la hanche libre descendre ; réduis l'amplitude avant de perdre " +
            "l'alignement.",
        "builtin:calf-raise" to
            "Place l'avant des pieds sur une marche, talons dans le vide. Monte le plus haut possible sur la " +
            "pointe, marque une pause, puis descends les talons sous la marche pour étirer. Garde les genoux " +
            "tendus sans les bloquer. Bouge lentement au lieu de rebondir, et tiens-toi à un support pour " +
            "l'équilibre.",
        "builtin:plank" to
            "Pose les avant-bras au sol sous les épaules et tends les jambes, corps en ligne droite. Gaine le " +
            "tronc, serre les fessiers et respire normalement. Ne laisse pas les hanches s'affaisser ni monter. " +
            "Arrête la série quand le bas du dos commence à faire mal, pas quand la montre le dit.",
        "builtin:side-plank" to
            "Allonge-toi sur le côté, coude sous l'épaule et pieds empilés. Monte les hanches pour former une " +
            "ligne droite, puis tiens. Garde l'épaule du haut au-dessus de celle du bas et ne laisse pas les " +
            "hanches partir en arrière. Change de côté pour le même temps ; pose le genou du bas si les hanches " +
            "ne tiennent pas.",
        "builtin:hollow-body-hold" to
            "Allonge-toi sur le dos, plaque le bas du dos au sol et soulève de quelques centimètres les omoplates " +
            "et les jambes. Tiens avec les côtes basses et les bras près des oreilles ou croisés sur la poitrine. " +
            "Si le bas du dos se décolle, plie les genoux ou monte les jambes plus haut jusqu'à ce qu'il reste au " +
            "sol.",
        "builtin:leg-raise" to
            "Allonge-toi sur le dos, jambes tendues et mains à côté des hanches ou sous les fessiers. Monte les " +
            "jambes à la verticale puis redescends lentement sans laisser le bas du dos se creuser. Plie les " +
            "genoux si le dos commence à se soulever. Une amplitude plus courte et contrôlée vaut mieux qu'une " +
            "amplitude basse avec élan.",
        "builtin:hanging-knee-raise" to
            "Suspends-toi à une barre, épaules actives. Ramène les genoux vers la poitrine, bascule légèrement le " +
            "bassin et redescends avec contrôle. Évite de te balancer. Si le corps part en balancement, attends " +
            "en bas qu'il s'arrête avant la répétition suivante.",
        "builtin:hanging-leg-raise" to
            "Suspends-toi à une barre et monte les jambes tendues à hauteur de hanches ou plus haut, en basculant " +
            "le bassin en arrière en haut. Redescends lentement sans te balancer. C'est la version difficile du " +
            "relevé de genoux. Commence genoux pliés et tends-les au fur et à mesure que tu gagnes du contrôle.",
        "builtin:dragon-flag" to
            "Allonge-toi sur un banc et attrape derrière la tête, puis monte le corps jusqu'aux épaules et " +
            "redescends-le comme une ligne droite avec contrôle. Garde les côtes basses et les fessiers serrés. " +
            "C'est un mouvement avancé. Commence par des versions groupées ou négatives et ne laisse jamais le bas " +
            "du dos se creuser.",
        "builtin:jumping-jack" to
            "Debout, bras le long du corps et pieds joints. Saute en écartant les pieds au-delà de la largeur " +
            "des épaules pendant que les bras montent au-dessus de la tête, puis reviens au départ dans le même " +
            "rythme. Reste sur l'avant des pieds, genoux souples et gaine active. Sers-t'en comme échauffement : " +
            "ce qui compte est le rythme cardiaque et les épaules, pas la vitesse. Si l'impact gêne les genoux, " +
            "écarte les pieds sans sauter.",
        "builtin:wall-handstand" to
            "Place les mains à 15 ou 20 cm du mur, monte d'un coup de pied ou en marchant jusqu'à ce que la " +
            "poitrine fasse face au mur et le corps soit droit. Pousse dans les épaules, garde les côtes basses " +
            "et tiens. Redescends avec contrôle. Quand tu tiens confortablement, entraîne-toi dos au mur, talons " +
            "appuyés.",
        "builtin:handstand-hold" to
            "Monte en handstand libre, mains à largeur d'épaules, doigts écartés et épaules poussées vers le haut. " +
            "Trouve l'équilibre avec de petits ajustements des doigts et des poignets, corps aligné. Entraîne-toi " +
            "près d'un mur et apprends à sortir vers l'avant ou sur le côté. Il faut beaucoup de tentatives " +
            "courtes, pas une longue.",
        "builtin:l-sit" to
            "Suspends-toi sur des parallettes ou au sol, bras tendus, épaules basses et jambes levées à 90°. " +
            "Tiens avec le tronc gainé et les pointes de pieds tendues. Si les jambes ne restent pas en place, " +
            "plie les genoux ou monte-les une par une. Ne monte pas les épaules ; pousse le sol.",
        "builtin:tuck-planche" to
            "Place les mains au niveau des hanches, doigts vers l'extérieur, incline les épaules au-dessus des " +
            "mains et monte les genoux vers la poitrine, hanches hautes. Tiens avec les bras tendus et le haut du " +
            "dos arrondi. C'est une progression de planche. Garde l'inclinaison et ne laisse pas les pieds " +
            "toucher le sol entre les essais.",
        "builtin:tuck-front-lever" to
            "Suspends-toi à une barre et tire les omoplates vers le bas, puis monte les genoux vers la poitrine et " +
            "penche-toi en arrière jusqu'à ce que le dos soit à peu près parallèle au sol. Tiens avec les bras " +
            "tendus et le corps bien groupé. C'est une progression de front lever. Empêche les hanches de tomber " +
            "et garde les coudes verrouillés ; ouvre le groupe seulement quand la tenue est solide.",
        "builtin:back-lever" to
            "Depuis un German hang ou une position inversée à la barre, descends le corps derrière la barre, bras " +
            "tendus, jusqu'à ce qu'il soit à peu près parallèle au sol. Garde les épaules actives et tout le corps " +
            "gainé. C'est un mouvement avancé. Progresse avec des versions groupées et écartées et échauffe bien " +
            "les épaules.",
    )

    private val de: Map<String, String> = mapOf(
        "builtin:push-up" to
            "Beginne im hohen Unterarmstütz, die Hände etwas breiter als die Schultern, der Körper in einer " +
            "Linie. Senke die Brust zwischen den Händen, die Ellbogen etwa 45° vom Rumpf entfernt, und drücke " +
            "dich hoch, ohne dass die Hüfte durchhängt. Merke: Gesäß anspannen und den Rumpf stabilisieren, damit " +
            "Brust und Hüfte gemeinsam sinken. Erleichtere mit einer Erhöhung oder den Knien; erschwere mit " +
            "langsamem Absenken oder erhöhten Füßen.",
        "builtin:knee-push-up" to
            "Knie dich hin, die Hände unter den Schultern, die Knie am Boden und eine gerade Linie von den Knien " +
            "bis zum Kopf. Senke die Brust zwischen den Händen und drücke dich hoch, ohne in der Hüfte " +
            "abzuknicken. Es ist dasselbe Muster wie der vollständige Liegestütz; halte den Rumpf stabil, damit " +
            "das Kreuz nicht ins Hohlkreuz geht. Wechsle zum vollständigen Liegestütz, wenn mehrere saubere Sätze " +
            "gelingen.",
        "builtin:incline-push-up" to
            "Lege die Hände auf eine Bank, einen Tisch oder eine Stufe und gehe mit den Füßen zurück, bis der " +
            "Körper gerade ist. Senke die Brust bis zur Kante und drücke weg. Je höher die Fläche, desto leichter; " +
            "senke sie mit zunehmender Kraft. Halte die Ellbogen etwa 45° nach hinten und den Körper in einer " +
            "Linie.",
        "builtin:diamond-push-up" to
            "Setze die Hände eng unter der Brust, Zeigefinger und Daumen bilden ein Dreieck. Halte die Ellbogen " +
            "nah am Rumpf, senke dich und drücke hoch. Die Arbeit geht mehr auf die Trizeps und die innere Brust. " +
            "Lass die Hüfte nicht nach oben kippen und die Schultern nicht zu den Ohren wandern.",
        "builtin:archer-push-up" to
            "Nimm eine sehr breite Handposition und verlagere beim Absenken das Gewicht auf einen Arm, während " +
            "der andere fast gestreckt bleibt. Komm zurück zur Mitte und wechsle die Seiten. Es ist ein Schritt " +
            "zum einarmigen Liegestütz; halte die Hüfte gerade, statt zu drehen, und den arbeitenden Ellbogen nah " +
            "am Körper.",
        "builtin:pseudo-planche-push-up" to
            "Setze die Hände auf Hüfthöhe, die Finger nach außen, und lehne die Schultern über die Hände nach " +
            "vorn. Senke und drücke, während du diese Vorlage hältst. Das belastet Schultern und Rumpf stark. " +
            "Halte den Körper in einer Linie und lass weder das Kreuz durchhängen noch die Ellbogen weit " +
            "ausbrechen.",
        "builtin:bench-dip" to
            "Setz dich auf eine Bank, die Hände neben der Hüfte, und rutsche mit der Hüfte über die Kante, die " +
            "Beine nach vorn. Beuge die Ellbogen, bis die Oberarme etwa parallel zum Boden sind, und drücke hoch. " +
            "Halte die Schultern unten und den Rücken nah an der Bank. Sinke nicht in die Schultern und federe " +
            "unten nicht.",
        "builtin:parallel-bar-dip" to
            "Stütze dich auf Parallelholmen ab, Arme gestreckt, Schultern unten, Beine hängend oder angewinkelt. " +
            "Senke dich, bis die Oberarme etwa parallel zum Boden sind, und drücke hoch. Lehne dich leicht nach " +
            "vorn für die Brust oder bleibe aufrecht für die Trizeps. Lass es weg, wenn die Schultern schmerzen, " +
            "und steigere dich mit Bankdips.",
        "builtin:pike-push-up" to
            "Gehe aus dem Liegestütz mit den Füßen zu den Händen und hebe die Hüfte in ein umgekehrtes V. Beuge " +
            "die Ellbogen, bis der Kopf zwischen den Händen zum Boden sinkt, und drücke zurück. Das baut den " +
            "Überkopfdrück für den Handstand auf. Halte die Ellbogen nach vorn gerichtet, nicht weit ausgestellt, " +
            "und die Hüfte hoch.",
        "builtin:handstand-push-up" to
            "Kicke dich in einen Handstand an der Wand, die Hände etwas breiter als die Schultern. Senke den Kopf " +
            "kontrolliert zum Boden und drücke dich zurück in den gestreckten Handstand. Versuche es erst, wenn du " +
            "den Wandhandstand hältst und Pike-Liegestütze schaffst. Halte den Rumpf fest und die Rippen unten, " +
            "statt das Kreuz zu überstrecken.",
        "builtin:dead-hang" to
            "Hänge an einer Stange im Obergriff, Arme gestreckt und die Schultern leicht nach unten, weg von den " +
            "Ohren, gezogen. Halte die Zeit und atme normal. Das baut Griffkraft und Schulterausdauer auf. " +
            "Schwinge nicht und lass die Schultern nicht zu den Ohren hochkommen; komm herunter, bevor der Griff " +
            "ganz versagt.",
        "builtin:scapular-pull-up" to
            "Hänge mit gestreckten Armen und ziehe die Schulterblätter nach unten und hinten, sodass der Körper " +
            "einige Zentimeter steigt, ohne die Ellbogen zu beugen. Halte kurz und senke kontrolliert. Das lehrt " +
            "den ersten Teil des Klimmzugs. Halte die Rippen unten und vermeide Schwingen; denke daran, die " +
            "Schulterblätter zu bewegen, nicht die Hände.",
        "builtin:inverted-row" to
            "Lege eine Stange auf Hüfthöhe und hänge dich mit geradem Körper und Fersen am Boden darunter. Ziehe " +
            "die Brust zur Stange und drücke die Schulterblätter zusammen, dann senke kontrolliert. Je tiefer die " +
            "Stange, desto schwerer. Halte die Hüfte oben und den Körper in einer Linie, statt mit dem Kinn " +
            "vorzugehen.",
        "builtin:pull-up" to
            "Hänge im Obergriff etwas breiter als schulterbreit an einer Stange. Ziehe die Brust zur Stange, " +
            "indem du die Ellbogen nach unten ziehst, und senke dich kontrolliert bis zum vollen Hang. Vermeide " +
            "Kipping und Schwingen. Wenn noch keiner gelingt, nutze Negative oder invertiertes Rudern und übe die " +
            "volle Bewegung.",
        "builtin:chin-up" to
            "Hänge im Untergriff etwa schulterbreit an einer Stange. Ziehe dich hoch, bis das Kinn über der " +
            "Stange ist, und senke dich ganz ab. Der Untergriff beansprucht mehr den Bizeps. Halte die Schultern " +
            "aktiv und die Rippen unten, statt das Kreuz zu überstrecken, um oben anzukommen.",
        "builtin:negative-pull-up" to
            "Beginne oben am Klimmzug, Kinn über der Stange, indem du springst oder aufsteigst. Senke dich so " +
            "langsam wie möglich, etwa drei bis fünf Sekunden, bis die Arme gestreckt sind. Das baut Kraft für " +
            "den ersten Klimmzug auf. Steige zum Zurücksetzen wieder auf, niemals durch Schwingen.",
        "builtin:commando-pull-up" to
            "Hänge im neutralen Griff an einer Stange, eine Hand vor der anderen. Ziehe dich hoch, bis eine " +
            "Schulter über die Stange kommt, senke dich und wechsle die Seiten. Halte den Körper nah an der Stange " +
            "und drehe nicht. Es ist eine anspruchsvolle Klimmzugvariante; beherrsche zuerst den normalen " +
            "Klimmzug.",
        "builtin:muscle-up" to
            "Hänge an der Stange, ziehe explosiv und bringe die Ellbogen über die Stange in einen Stütz mit " +
            "gestreckten Armen. Senke dich kontrolliert oder kehre die Bewegung um. Du brauchst einen starken " +
            "Klimmzug und Dip. Lerne den Übergang an einer niedrigen Stange und mit falschem Griff, bevor du Höhe " +
            "oder Tempo erhöhst.",
        "builtin:squat" to
            "Stelle die Füße etwa schulterbreit, die Zehen leicht nach außen. Schiebe die Hüfte nach hinten und " +
            "unten, Brust hoch und Knie über den Zehen, dann steh auf. Gehe so tief, wie du kannst, ohne dass das " +
            "Kreuz rund wird. Drücke über den ganzen Fuß, nicht nur über die Zehen.",
        "builtin:split-squat" to
            "Tritt mit einem Fuß vor und senke das hintere Knie zum Boden, das vordere Schienbein etwa senkrecht. " +
            "Drücke über den vorderen Fuß hoch. Halte den Oberkörper aufrecht und die Hüfte gerade. Das hintere " +
            "Knie soll sich dem Boden nähern, ohne aufzuschlagen; halte dich an einer Wand oder einem Stuhl fest, " +
            "wenn das Gleichgewicht limitiert.",
        "builtin:bulgarian-split-squat" to
            "Lege den Fußrücken des hinteren Fußes auf eine Bank und stehe auf dem vorderen Bein. Senke dich " +
            "gerade nach unten, bis der vordere Oberschenkel fast parallel zum Boden ist, und drücke hoch. Ein " +
            "starker einbeiniger Kraftaufbau. Halte das vordere Knie über dem Fuß und den Oberkörper aufrecht; " +
            "beginne mit einer niedrigen Bank, wenn die Position instabil ist.",
        "builtin:reverse-lunge" to
            "Stehe aufrecht, tritt mit einem Fuß nach hinten und senke beide Knie, bis das hintere Knie knapp über " +
            "dem Boden ist. Drücke über den vorderen Fuß zurück. Der Schritt nach hinten ist knieschonender als " +
            "nach vorn. Halte den Oberkörper aufrecht und das vordere Knie über den Zehen.",
        "builtin:nordic-curl" to
            "Knie dich hin, die Fußknöchel fixiert, und halte den Körper von den Knien bis zum Kopf in einer " +
            "Linie. Senke den Oberkörper so langsam wie möglich mit den Beinbeugern nach vorn und drücke dich bei " +
            "Bedarf mit den Händen zurück. Es ist sehr anspruchsvoll. Beginne mit kleinem Bewegungsradius und " +
            "steigere ihn; lass die Hüfte die Linie nicht brechen.",
        "builtin:glute-bridge" to
            "Lege dich auf den Rücken, Knie gebeugt und Füße flach nahe der Hüfte. Drücke über die Fersen und hebe " +
            "die Hüfte, bis der Körper von den Knien bis zu den Schultern eine Linie bildet, dann senke. Gesäß " +
            "oben anspannen. Überstrecke das Kreuz nicht, um höher zu kommen.",
        "builtin:single-leg-glute-bridge" to
            "Lege dich auf den Rücken, ein Fuß flach, das andere Bein gestreckt oder das Knie herangezogen. Hebe " +
            "die Hüfte mit dem Arbeitsbein und halte das Becken waagerecht, dann senke. Das zeigt Kraftunterschiede " +
            "zwischen den Seiten. Lass die freie Hüfte nicht absinken; verkleinere den Radius, bevor die " +
            "Ausrichtung verloren geht.",
        "builtin:calf-raise" to
            "Stelle die Fußballen auf eine Stufe, die Fersen hängen über die Kante. Geh so hoch wie möglich auf " +
            "die Zehen, halte kurz und senke die Fersen unter die Stufe für eine Dehnung. Halte die Knie " +
            "gestreckt, aber nicht blockiert. Bewege dich langsam statt zu federn, und halte dich für das " +
            "Gleichgewicht fest.",
        "builtin:plank" to
            "Lege die Unterarme unter den Schultern auf den Boden und strecke die Beine, der Körper in einer " +
            "geraden Linie. Rumpf stabilisieren, Gesäß anspannen und normal atmen. Lass die Hüfte weder " +
            "durchhängen noch nach oben kippen. Beende den Satz, wenn das Kreuz zu schmerzen beginnt, nicht wenn " +
            "die Uhr es sagt.",
        "builtin:side-plank" to
            "Lege dich auf die Seite, den Ellbogen unter der Schulter und die Füße übereinander. Hebe die Hüfte, " +
            "bis der Körper eine gerade Linie bildet, und halte. Halte die obere Schulter über der unteren und " +
            "lass die Hüfte nicht nach hinten rollen. Wechsle die Seite für die gleiche Zeit; setze das untere " +
            "Knie ab, wenn die Hüfte nicht oben bleibt.",
        "builtin:hollow-body-hold" to
            "Lege dich auf den Rücken, drücke das Kreuz in den Boden und hebe Schulterblätter und Beine einige " +
            "Zentimeter. Halte mit den Rippen unten und den Armen neben den Ohren oder vor der Brust gekreuzt. " +
            "Wenn das Kreuz abhebt, beuge die Knie oder hebe die Beine höher, bis es unten bleibt.",
        "builtin:leg-raise" to
            "Lege dich auf den Rücken, die Beine gestreckt und die Hände neben der Hüfte oder unter dem Gesäß. " +
            "Hebe die Beine senkrecht und senke sie langsam, ohne dass das Kreuz ins Hohlkreuz geht. Beuge die " +
            "Knie, wenn der Rücken abhebt. Ein kleinerer, kontrollierter Radius ist besser als ein tiefer mit " +
            "Schwung.",
        "builtin:hanging-knee-raise" to
            "Hänge mit aktiven Schultern an einer Stange. Ziehe die Knie zur Brust, kippe das Becken leicht und " +
            "senke kontrolliert. Vermeide Schwingen. Wenn der Körper zu schwingen beginnt, warte unten, bis er " +
            "stoppt, bevor die nächste Wiederholung kommt.",
        "builtin:hanging-leg-raise" to
            "Hänge an einer Stange und hebe die gestreckten Beine auf Hüfthöhe oder höher, wobei das Becken oben " +
            "nach hinten kippt. Senke langsam ohne Schwingen. Es ist die schwere Version des Kniehebens. Beginne " +
            "mit gebeugten Knien und strecke sie mit zunehmender Kontrolle.",
        "builtin:dragon-flag" to
            "Lege dich auf eine Bank und greife hinter den Kopf, dann hebe den Körper bis zu den Schultern und " +
            "senke ihn als eine gerade Linie kontrolliert. Halte die Rippen unten und das Gesäß fest. Es ist eine " +
            "fortgeschrittene Übung. Beginne mit eingerollten oder negativen Versionen und lass das Kreuz nie ins " +
            "Hohlkreuz gehen.",
        "builtin:jumping-jack" to
            "Aufrecht stehen, Arme seitlich am Körper, Füße zusammen. Springe mit den Füßen weiter als " +
            "schulterbreit auseinander, während die Arme über den Kopf schwingen, und komm im gleichen Rhythmus " +
            "zurück. Bleib auf dem Fußballen, halte die Knie weich und die Körpermitte aktiv. Als Aufwärmen " +
            "gedacht: Es geht um Herzfrequenz und Schultern, nicht um Tempo. Wenn der Aufprall den Knien weh tut, " +
            "stell die Füße ohne Sprung auseinander.",
        "builtin:wall-handstand" to
            "Setze die Hände etwa 15 bis 20 cm von der Wand entfernt, kicke oder laufe hoch, bis die Brust zur " +
            "Wand zeigt und der Körper gerade ist. Drücke in den Schultern, halte die Rippen unten und halte. " +
            "Komm kontrolliert herunter. Wenn es bequem klappt, übe mit dem Rücken zur Wand und den Fersen daran.",
        "builtin:handstand-hold" to
            "Kicke dich in einen freien Handstand, die Hände schulterbreit, die Finger gespreizt und die " +
            "Schultern nach oben gedrückt. Finde das Gleichgewicht mit kleinen Finger- und " +
            "Handgelenkskorrekturen und halte den Körper in einer Linie. Übe nahe einer Wand und lerne, nach vorn " +
            "oder seitlich auszusteigen. Es braucht viele kurze Versuche, nicht einen langen.",
        "builtin:l-sit" to
            "Stütze dich auf Parallettes oder am Boden ab, Arme gestreckt, Schultern unten und die Beine im " +
            "90°-Winkel gehoben. Halte mit festem Rumpf und gestreckten Zehen. Wenn die Beine nicht oben bleiben, " +
            "beuge die Knie oder hebe sie nacheinander. Ziehe die Schultern nicht hoch; drücke den Boden weg.",
        "builtin:tuck-planche" to
            "Setze die Hände auf Hüfthöhe, die Finger nach außen, lehne die Schultern über die Hände und ziehe die " +
            "Knie zur Brust, die Hüfte hoch. Halte mit gestreckten Armen und rundem oberen Rücken. Es ist eine " +
            "Planche-Progression. Halte die Vorlage und lass die Füße zwischen den Versuchen nicht absetzen.",
        "builtin:tuck-front-lever" to
            "Hänge an einer Stange und ziehe die Schulterblätter nach unten, dann ziehe die Knie zur Brust und " +
            "lehne dich zurück, bis der Rücken etwa parallel zum Boden ist. Halte mit gestreckten Armen und " +
            "festem Tuck. Es ist eine Front-Lever-Progression. Lass die Hüfte nicht absinken und die Ellbogen " +
            "verriegelt; öffne den Tuck erst, wenn der Halt sicher ist.",
        "builtin:back-lever" to
            "Senke dich aus einem German Hang oder einer umgekehrten Position an der Stange mit gestreckten Armen " +
            "hinter die Stange, bis der Körper etwa parallel zum Boden ist. Halte die Schultern aktiv und den " +
            "ganzen Körper fest. Es ist eine fortgeschrittene Übung. Steigere dich mit eingerollten und " +
            "gegrätschten Versionen und wärme die Schultern gründlich auf.",
    )

    private val pt: Map<String, String> = mapOf(
        "builtin:push-up" to
            "Começa em prancha alta, com as mãos um pouco mais afastadas que os ombros e o corpo em linha. Desce " +
            "o peito entre as mãos, com os cotovelos a cerca de 45° do tronco, e empurra para subir sem deixar o " +
            "quadril cair. Dica: aperta os glúteos e ativa o core para que peito e quadril desçam juntos. Facilita " +
            "com inclinação ou joelhos; dificulta com uma descida lenta ou os pés elevados.",
        "builtin:knee-push-up" to
            "Ajoelha-te com as mãos sob os ombros, os joelhos no chão e uma linha reta dos joelhos à cabeça. " +
            "Desce o peito entre as mãos e empurra sem dobrar na anca. Serve para construir o mesmo padrão da " +
            "flexão completa; mantém o core ativo para a zona lombar não arquear. Passa à flexão completa quando " +
            "conseguires várias séries limpas.",
        "builtin:incline-push-up" to
            "Apoia as mãos num banco, mesa ou degrau e afasta os pés até o corpo ficar direito. Desce o peito até " +
            "ao bordo e empurra. Quanto mais alta a superfície, mais fácil; baixa-a à medida que ganhas força. " +
            "Mantém os cotovelos para trás a cerca de 45° e o corpo em linha.",
        "builtin:diamond-push-up" to
            "Junta as mãos sob o peito, com os indicadores e polegares a formar um diamante. Mantém os cotovelos " +
            "junto às costelas ao descer e empurra para subir. Passa mais trabalho para o tríceps e para a parte " +
            "interna do peito. Não deixes o quadril subir nem os ombros aproximarem-se das orelhas.",
        "builtin:archer-push-up" to
            "Abre bem as mãos e desloca o peso para um braço ao descer, mantendo o outro quase esticado. Volta ao " +
            "centro e alterna os lados. É um passo para a flexão a um braço; mantém o quadril quadrado em vez de " +
            "rodar e deixa o cotovelo ativo junto ao corpo.",
        "builtin:pseudo-planche-push-up" to
            "Coloca as mãos à altura da anca com os dedos virados para fora e inclina os ombros para a frente " +
            "sobre as mãos. Desce e empurra mantendo essa inclinação. Carrega muito os ombros e o core. Mantém o " +
            "corpo em linha e não deixes a zona lombar ceder nem os cotovelos abrirem.",
        "builtin:bench-dip" to
            "Senta-te num banco, coloca as mãos ao lado da anca e desliza a anca para fora do bordo com as pernas " +
            "à frente. Dobra os cotovelos para descer até os braços ficarem mais ou menos paralelos ao chão e " +
            "empurra. Mantém os ombros em baixo e as costas junto ao banco. Não te afundes nos ombros nem saltes " +
            "em baixo.",
        "builtin:parallel-bar-dip" to
            "Apoia-te nas barras paralelas com os braços esticados, ombros em baixo e pernas penduradas ou " +
            "dobradas. Desce até os braços ficarem mais ou menos paralelos ao chão e empurra. Inclina-te " +
            "ligeiramente para a frente para o peito ou mantém-te direito para o tríceps. Evita se os ombros " +
            "doerem e progride com dips no banco.",
        "builtin:pike-push-up" to
            "A partir da posição de flexão, caminha os pés para as mãos e sobe a anca formando um V invertido. " +
            "Dobra os cotovelos para baixar a cabeça ao chão entre as mãos e empurra. Constrói o empurrão por cima " +
            "da cabeça para o pino. Mantém os cotovelos para a frente, sem abrir muito, e a anca alta.",
        "builtin:handstand-push-up" to
            "Sobe para um pino contra a parede com as mãos um pouco mais afastadas que os ombros. Desce a cabeça " +
            "ao chão com controlo e empurra para voltar ao pino direito. Só tentes quando aguentares o pino na " +
            "parede e fizeres pike push-ups. Mantém o core firme e as costelas em baixo em vez de arquear as " +
            "costas.",
        "builtin:dead-hang" to
            "Pendura-te numa barra com pegada pronada, braços esticados e ombros ligeiramente puxados para baixo, " +
            "longe das orelhas. Aguenta o tempo que conseguires a respirar normalmente. Constrói força de " +
            "preensão e resistência dos ombros. Não balances nem deixes os ombros subir às orelhas; desce antes de " +
            "a pega falhar por completo.",
        "builtin:scapular-pull-up" to
            "Pendura-te com os braços esticados e puxa as escápulas para baixo e para trás para o corpo subir " +
            "alguns centímetros sem dobrar os cotovelos. Faz uma pausa e desce com controlo. Ensina a primeira " +
            "parte da barra fixa. Mantém as costelas em baixo e evita balançar; pensa em mover as escápulas, não " +
            "as mãos.",
        "builtin:inverted-row" to
            "Coloca uma barra à altura da cintura e pendura-te por baixo com o corpo direito e os calcanhares no " +
            "chão. Puxa o peito à barra apertando as escápulas e desce com controlo. Quanto mais baixa a barra, " +
            "mais difícil. Mantém a anca em cima e o corpo em linha em vez de avançar o queixo.",
        "builtin:pull-up" to
            "Pendura-te numa barra com pegada pronada um pouco mais larga que os ombros. Leva o peito à barra " +
            "puxando os cotovelos para baixo e desce até à pendura completa com controlo. Evita kipping ou " +
            "balançar. Se ainda não fizeres uma, usa negativas ou remo invertido e continua a praticar o movimento " +
            "completo.",
        "builtin:chin-up" to
            "Pendura-te com pegada supinada à largura dos ombros. Sobe até o queixo passar a barra e desce por " +
            "completo. A pegada supinada usa mais bíceps. Mantém os ombros ativos e as costelas em baixo em vez de " +
            "arquear as costas para chegar acima.",
        "builtin:negative-pull-up" to
            "Começa no topo da barra fixa, com o queixo acima da barra, saltando ou subindo com um apoio. Desce o " +
            "mais devagar possível, procurando três a cinco segundos, até esticares os braços. Constrói força para " +
            "a primeira barra fixa. Volta a subir com o apoio, nunca balançando.",
        "builtin:commando-pull-up" to
            "Pendura-te numa barra com pegada neutra, uma mão à frente da outra. Sobe até um ombro passar a barra, " +
            "desce e alterna os lados. Mantém o corpo junto à barra e evita rodar. É uma variante exigente; " +
            "domina primeiro a barra fixa normal.",
        "builtin:muscle-up" to
            "Pendura-te na barra, puxa com força e passa os cotovelos por cima da barra para um apoio com os " +
            "braços esticados. Desce com controlo ou inverte o movimento. Precisas de uma barra fixa e de um dip " +
            "fortes. Aprende a transição numa barra baixa e com pegada falsa antes de acrescentar altura ou " +
            "velocidade.",
        "builtin:squat" to
            "Põe os pés à largura dos ombros e as pontas ligeiramente para fora. Leva a anca para trás e para " +
            "baixo, com o peito em cima e os joelhos a seguir a linha dos pés, e sobe. Desce o mais que " +
            "conseguires sem a zona lombar arredondar. Empurra com o pé todo, não só com as pontas.",
        "builtin:split-squat" to
            "Dá um passo em frente e desce o joelho de trás em direção ao chão, com a canela da frente mais ou " +
            "menos vertical. Empurra com o pé da frente para subir. Mantém o tronco direito e a anca quadrada. O " +
            "joelho de trás deve aproximar-se do chão sem bater; apoia-te numa parede ou cadeira se o equilíbrio " +
            "for o limite.",
        "builtin:bulgarian-split-squat" to
            "Apoia o peito do pé de trás num banco e fica de pé sobre a perna da frente. Desce a direito até a " +
            "coxa da frente ficar quase paralela ao chão e empurra. É um grande construtor de força a uma perna. " +
            "Mantém o joelho da frente sobre o pé e o tronco alto; começa com um banco baixo se a posição for " +
            "instável.",
        "builtin:reverse-lunge" to
            "Fica direito, dá um passo atrás e desce os dois joelhos até o de trás ficar logo acima do chão. " +
            "Empurra com o pé da frente para voltar. Dar o passo atrás é mais amigo dos joelhos do que à frente. " +
            "Mantém o tronco direito e o joelho da frente sobre os dedos do pé.",
        "builtin:nordic-curl" to
            "Ajoelha-te com os tornozelos presos e o corpo em linha reta dos joelhos à cabeça. Desce o tronco para " +
            "a frente o mais devagar possível com os isquiotibiais e empurra com as mãos se precisares. É muito " +
            "exigente. Começa com uma amplitude curta e aumenta-a com o tempo; não deixes a anca quebrar a linha.",
        "builtin:glute-bridge" to
            "Deita-te de barriga para cima com os joelhos dobrados e os pés assentes, perto da anca. Empurra com " +
            "os calcanhares e sobe a anca até o corpo formar uma linha dos joelhos aos ombros, depois desce. " +
            "Aperta os glúteos em cima. Não arqueies a zona lombar para subir mais.",
        "builtin:single-leg-glute-bridge" to
            "Deita-te de barriga para cima com um pé assente e a outra perna esticada ou o joelho abraçado. Sobe " +
            "a anca com a perna ativa, mantendo a bacia nivelada, e desce. Revela diferenças de força entre lados. " +
            "Não deixes a anca livre cair; reduz a amplitude antes de perderes o alinhamento.",
        "builtin:calf-raise" to
            "Põe a parte da frente dos pés num degrau e os calcanhares de fora. Sobe o mais alto que conseguires " +
            "na ponta, faz uma pausa e desce os calcanhares abaixo do degrau para alongar. Mantém os joelhos " +
            "esticados mas não bloqueados. Move-te devagar em vez de saltar e segura-te para o equilíbrio.",
        "builtin:plank" to
            "Apoia os antebraços no chão sob os ombros e estica as pernas, com o corpo em linha reta. Ativa o " +
            "core, aperta os glúteos e respira normalmente. Não deixes a anca ceder nem subir. Termina a série " +
            "quando a zona lombar começar a doer, não quando o relógio disser.",
        "builtin:side-plank" to
            "Deita-te de lado com o cotovelo sob o ombro e os pés empilhados. Sobe a anca até o corpo formar uma " +
            "linha reta e aguenta. Mantém o ombro de cima sobre o de baixo e não deixes a anca rolar para trás. " +
            "Troca de lado pelo mesmo tempo; apoia o joelho de baixo se a anca não ficar em cima.",
        "builtin:hollow-body-hold" to
            "Deita-te de barriga para cima, pressiona a zona lombar contra o chão e levanta alguns centímetros as " +
            "omoplatas e as pernas. Aguenta com as costelas em baixo e os braços junto às orelhas ou cruzados no " +
            "peito. Se a zona lombar se levantar, dobra os joelhos ou sobe mais as pernas até ficar assente.",
        "builtin:leg-raise" to
            "Deita-te de barriga para cima com as pernas esticadas e as mãos ao lado da anca ou sob os glúteos. " +
            "Sobe as pernas até à vertical e desce-as devagar sem deixar a zona lombar arquear. Dobra os joelhos " +
            "se as costas começarem a levantar. Uma amplitude mais curta e controlada vale mais do que uma baixa " +
            "com balanço.",
        "builtin:hanging-knee-raise" to
            "Pendura-te numa barra com os ombros ativos. Puxa os joelhos ao peito, roda ligeiramente a bacia e " +
            "desce com controlo. Evita balançar. Se o corpo começar a balançar, espera em baixo até parar antes da " +
            "repetição seguinte.",
        "builtin:hanging-leg-raise" to
            "Pendura-te numa barra e sobe as pernas esticadas até à altura da anca ou mais, rodando a bacia para " +
            "trás em cima. Desce devagar sem balançar. É a versão difícil da elevação de joelhos. Começa com os " +
            "joelhos dobrados e estica-os à medida que ganhas controlo.",
        "builtin:dragon-flag" to
            "Deita-te num banco e agarra atrás da cabeça; sobe o corpo até aos ombros e desce-o como uma linha " +
            "reta com controlo. Mantém as costelas em baixo e os glúteos apertados. É um movimento avançado. " +
            "Começa com versões encolhidas ou negativas e nunca deixes a zona lombar arquear.",
        "builtin:jumping-jack" to
            "De pé, ereto, com os braços ao lado do corpo e os pés juntos. Salta abrindo os pés além da largura " +
            "dos ombros enquanto os braços sobem acima da cabeça, e volta ao início no mesmo ritmo. Fica na ponta " +
            "dos pés, mantém os joelhos soltos e o core ativo. Usa como aquecimento: o que importa é a frequência " +
            "cardíaca e os ombros, não a velocidade. Se o impacto incomodar os joelhos, abre os pés sem saltar.",
        "builtin:wall-handstand" to
            "Põe as mãos a cerca de 15 a 20 cm da parede, sobe com um pontapé ou caminhando até o peito ficar " +
            "virado para a parede e o corpo direito. Empurra nos ombros, mantém as costelas em baixo e aguenta. " +
            "Desce com controlo. Quando aguentares confortavelmente, pratica de costas para a parede com os " +
            "calcanhares apoiados.",
        "builtin:handstand-hold" to
            "Sobe para um pino livre com as mãos à largura dos ombros, os dedos abertos e os ombros a empurrar " +
            "para cima. Encontra o equilíbrio com pequenos ajustes dos dedos e pulsos, mantendo o corpo em linha. " +
            "Pratica perto de uma parede e aprende a sair para a frente ou para o lado. Precisa de muitas " +
            "tentativas curtas, não de uma longa.",
        "builtin:l-sit" to
            "Apoia-te em parallettes ou no chão com os braços esticados, ombros em baixo e as pernas levantadas a " +
            "90°. Aguenta com o core ativo e os dedos dos pés esticados. Se as pernas não ficarem em cima, dobra " +
            "os joelhos ou sobe-as uma de cada vez. Não encolhas os ombros; empurra o chão.",
        "builtin:tuck-planche" to
            "Coloca as mãos à altura da anca com os dedos virados para fora, inclina os ombros sobre as mãos e " +
            "sobe os joelhos ao peito com a anca alta. Aguenta com os braços esticados e a parte de cima das " +
            "costas arredondada. É uma progressão de planche. Mantém a inclinação e não deixes os pés tocar no " +
            "chão entre tentativas.",
        "builtin:tuck-front-lever" to
            "Pendura-te numa barra e puxa as escápulas para baixo; depois sobe os joelhos ao peito e inclina-te " +
            "para trás até as costas ficarem mais ou menos paralelas ao chão. Aguenta com os braços esticados e o " +
            "corpo bem encolhido. É uma progressão de front lever. Evita que a anca caia e mantém os cotovelos " +
            "bloqueados; abre o encolhimento só quando o apoio for sólido.",
        "builtin:back-lever" to
            "A partir de um German hang ou de uma posição invertida na barra, desce o corpo atrás da barra com os " +
            "braços esticados até ficar mais ou menos paralelo ao chão. Mantém os ombros ativos e o corpo todo " +
            "firme. É um movimento avançado. Progride com versões encolhidas e straddle e aquece bem os ombros.",
    )

    private val it: Map<String, String> = mapOf(
        "builtin:push-up" to
            "Parti in plank alta, mani poco più larghe delle spalle e corpo in linea. Abbassa il petto tra le " +
            "mani, gomiti a circa 45° dal busto, poi spingi per risalire senza far cadere i fianchi. Cue: stringi " +
            "i glutei e attiva il core così petto e fianchi scendono insieme. Facilita con un piano inclinato o le " +
            "ginocchia; complica con una discesa lenta o i piedi sollevati.",
        "builtin:knee-push-up" to
            "Mettiti in ginocchio con le mani sotto le spalle, le ginocchia a terra e una linea retta dalle " +
            "ginocchia alla testa. Abbassa il petto tra le mani e spingi senza piegarti sui fianchi. Serve a " +
            "costruire lo stesso schema della flessione completa; mantieni il core attivo per non inarcare la zona " +
            "lombare. Passa alla flessione completa quando riesci a fare più serie pulite.",
        "builtin:incline-push-up" to
            "Appoggia le mani su una panchina, un tavolo o un gradino e allontana i piedi finché il corpo è " +
            "dritto. Abbassa il petto al bordo e spingi. Più alta è la superficie, più è facile; abbassala man " +
            "mano che acquisti forza. Tieni i gomiti indietro a circa 45° e il corpo in linea.",
        "builtin:diamond-push-up" to
            "Avvicina le mani sotto il petto, indici e pollici a formare un diamante. Tieni i gomiti vicini alle " +
            "costole mentre scendi, poi spingi. Il lavoro si sposta su tricipiti e parte interna del petto. Non " +
            "lasciare che i fianchi salgano o che le spalle si avvicinino alle orecchie.",
        "builtin:archer-push-up" to
            "Apri bene le mani e sposta il peso su un braccio mentre scendi, tenendo l'altro quasi disteso. Torna " +
            "al centro e alterna i lati. È un passo verso la flessione a un braccio; mantieni i fianchi quadrati " +
            "invece di ruotare e lascia il gomito attivo vicino al corpo.",
        "builtin:pseudo-planche-push-up" to
            "Metti le mani all'altezza dei fianchi con le dita verso l'esterno e inclina le spalle in avanti sopra " +
            "le mani. Scendi e spingi mantenendo quell'inclinazione. Carica molto spalle e core. Tieni il corpo in " +
            "linea e non lasciare che la zona lombare ceda o che i gomiti si aprano.",
        "builtin:bench-dip" to
            "Siediti su una panchina, metti le mani ai lati dei fianchi e fai scivolare i fianchi fuori dal bordo " +
            "con le gambe davanti. Piega i gomiti per scendere finché le braccia sono più o meno parallele al " +
            "pavimento, poi spingi. Tieni le spalle basse e la schiena vicina alla panchina. Non sprofondare nelle " +
            "spalle e non rimbalzare in basso.",
        "builtin:parallel-bar-dip" to
            "Appoggiati alle parallele con le braccia distese, le spalle basse e le gambe pendenti o piegate. " +
            "Scendi finché le braccia sono più o meno parallele al pavimento, poi spingi. Inclina leggermente il " +
            "busto in avanti per il petto o resta dritto per i tricipiti. Evita se le spalle fanno male e " +
            "progredisci con i dip sulla panchina.",
        "builtin:pike-push-up" to
            "Dalla posizione di flessione, cammina con i piedi verso le mani e solleva i fianchi a V rovesciata. " +
            "Piega i gomiti per abbassare la testa verso il pavimento tra le mani, poi spingi. Costruisce la " +
            "spinta sopra la testa per l'handstand. Tieni i gomiti in avanti, senza aprirli troppo, e i fianchi " +
            "alti.",
        "builtin:handstand-push-up" to
            "Sali in handstand contro il muro con le mani poco più larghe delle spalle. Abbassa la testa verso il " +
            "pavimento con controllo e spingi per tornare all'handstand dritto. Provaci solo quando tieni " +
            "l'handstand al muro e fai i pike push-up. Tieni il core fermo e le costole basse invece di inarcare " +
            "la schiena.",
        "builtin:dead-hang" to
            "Appenditi a una sbarra con presa prona, braccia distese e spalle leggermente tirate in basso, lontane " +
            "dalle orecchie. Resisti il tempo che riesci respirando normalmente. Costruisce forza di presa e " +
            "resistenza delle spalle. Non dondolare e non lasciare che le spalle salgano alle orecchie; scendi " +
            "prima che la presa ceda del tutto.",
        "builtin:scapular-pull-up" to
            "Appenditi con le braccia distese, poi tira le scapole in basso e indietro così il corpo sale di " +
            "qualche centimetro senza piegare i gomiti. Fermati un attimo e scendi con controllo. Insegna la prima " +
            "parte della trazione. Tieni le costole basse ed evita di dondolare; pensa a muovere le scapole, non " +
            "le mani.",
        "builtin:inverted-row" to
            "Metti una sbarra all'altezza della vita e appenditi sotto con il corpo dritto e i talloni a terra. " +
            "Tira il petto alla sbarra stringendo le scapole, poi scendi con controllo. Più bassa è la sbarra, più " +
            "è difficile. Tieni i fianchi alti e il corpo in linea invece di portare avanti il mento.",
        "builtin:pull-up" to
            "Appenditi a una sbarra con presa prona poco più larga delle spalle. Porta il petto alla sbarra " +
            "tirando i gomiti in basso, poi scendi fino alla sospensione completa con controllo. Evita kipping e " +
            "dondolii. Se non ne fai ancora una, usa le negative o il rematore invertito e continua a provare il " +
            "movimento completo.",
        "builtin:chin-up" to
            "Appenditi con presa supina alla larghezza delle spalle. Tira finché il mento supera la sbarra, poi " +
            "scendi completamente. La presa supina usa più bicipiti. Tieni le spalle attive e le costole basse " +
            "invece di inarcare la schiena per arrivare in alto.",
        "builtin:negative-pull-up" to
            "Parti in cima alla trazione, con il mento sopra la sbarra, saltando o salendo su un appoggio. Scendi " +
            "il più lentamente possibile, cercando tre-cinque secondi, finché le braccia sono distese. Costruisce " +
            "forza per la prima trazione. Risali con l'appoggio, mai dondolando.",
        "builtin:commando-pull-up" to
            "Appenditi a una sbarra con presa neutra, una mano davanti all'altra. Tira finché una spalla supera la " +
            "sbarra, scendi e alterna i lati. Tieni il corpo vicino alla sbarra ed evita di ruotare. È una " +
            "variante impegnativa; padroneggia prima la trazione normale.",
        "builtin:muscle-up" to
            "Appenditi alla sbarra, tira con forza e porta i gomiti sopra la sbarra in un appoggio a braccia " +
            "distese. Scendi con controllo o inverti il movimento. Servono una trazione e un dip forti. Impara la " +
            "transizione su una sbarra bassa e con la presa falsa prima di aggiungere altezza o velocità.",
        "builtin:squat" to
            "Metti i piedi alla larghezza delle spalle e le punte leggermente verso l'esterno. Porta i fianchi " +
            "indietro e in basso, petto alto e ginocchia in linea con i piedi, poi risali. Scendi il più possibile " +
            "senza arrotondare la zona lombare. Spingi con tutto il piede, non solo con le punte.",
        "builtin:split-squat" to
            "Fai un passo avanti e abbassa il ginocchio dietro verso il pavimento, con lo stinco davanti più o " +
            "meno verticale. Spingi con il piede davanti per risalire. Tieni il busto dritto e i fianchi " +
            "quadrati. Il ginocchio dietro deve avvicinarsi al pavimento senza sbattere; appoggiati a un muro o a " +
            "una sedia se l'equilibrio è il limite.",
        "builtin:bulgarian-split-squat" to
            "Appoggia il collo del piede dietro su una panchina e resta in piedi sulla gamba davanti. Scendi " +
            "dritto finché la coscia davanti è quasi parallela al pavimento, poi spingi. È un ottimo costruttore " +
            "di forza su una gamba. Tieni il ginocchio davanti sopra il piede e il busto alto; inizia con una " +
            "panchina bassa se la posizione è instabile.",
        "builtin:reverse-lunge" to
            "Stai dritto, fai un passo indietro e abbassa entrambe le ginocchia finché quella dietro è appena " +
            "sopra il pavimento. Spingi con il piede davanti per tornare. Il passo indietro è più gentile con le " +
            "ginocchia di quello in avanti. Tieni il busto dritto e il ginocchio davanti in linea con le dita del " +
            "piede.",
        "builtin:nordic-curl" to
            "Mettiti in ginocchio con le caviglie bloccate e il corpo in linea retta dalle ginocchia alla testa. " +
            "Abbassa il busto in avanti il più lentamente possibile usando i femorali, poi spingi con le mani se " +
            "serve. È molto impegnativo. Inizia con un'ampiezza breve e aumentala nel tempo; non lasciare che i " +
            "fianchi rompano la linea.",
        "builtin:glute-bridge" to
            "Sdraiati sulla schiena con le ginocchia piegate e i piedi piatti, vicini ai fianchi. Spingi con i " +
            "talloni e solleva i fianchi finché il corpo forma una linea dalle ginocchia alle spalle, poi scendi. " +
            "Stringi i glutei in alto. Non inarcare la zona lombare per salire di più.",
        "builtin:single-leg-glute-bridge" to
            "Sdraiati sulla schiena con un piede piatto e l'altra gamba distesa o il ginocchio abbracciato. Solleva " +
            "i fianchi con la gamba attiva, tenendo il bacino livellato, poi scendi. Rivela differenze di forza " +
            "tra i lati. Non lasciare che l'anca libera scenda; riduci l'ampiezza prima di perdere l'allineamento.",
        "builtin:calf-raise" to
            "Metti l'avampiede su un gradino e i talloni fuori. Sali il più possibile sulle punte, fermati un " +
            "attimo e abbassa i talloni sotto il gradino per allungare. Tieni le ginocchia distese ma non " +
            "bloccate. Muoviti lentamente invece di rimbalzare e tieniti a qualcosa per l'equilibrio.",
        "builtin:plank" to
            "Appoggia gli avambracci a terra sotto le spalle e distendi le gambe, con il corpo in linea retta. " +
            "Attiva il core, stringi i glutei e respira normalmente. Non lasciare che i fianchi cedano o salgano. " +
            "Termina la serie quando la zona lombare inizia a dolere, non quando lo dice l'orologio.",
        "builtin:side-plank" to
            "Sdraiati su un fianco con il gomito sotto la spalla e i piedi sovrapposti. Solleva i fianchi finché " +
            "il corpo forma una linea retta, poi resisti. Tieni la spalla in alto sopra quella in basso e non " +
            "lasciare che i fianchi ruotino indietro. Cambia lato per lo stesso tempo; appoggia il ginocchio in " +
            "basso se i fianchi non restano su.",
        "builtin:hollow-body-hold" to
            "Sdraiati sulla schiena, premi la zona lombare a terra e solleva di qualche centimetro le scapole e le " +
            "gambe. Resisti con le costole basse e le braccia vicino alle orecchie o incrociate sul petto. Se la " +
            "zona lombare si stacca, piega le ginocchia o alza di più le gambe finché resta a terra.",
        "builtin:leg-raise" to
            "Sdraiati sulla schiena con le gambe distese e le mani ai lati dei fianchi o sotto i glutei. Solleva " +
            "le gambe in verticale e abbassale lentamente senza lasciare che la zona lombare si inarchi. Piega le " +
            "ginocchia se la schiena inizia a sollevarsi. Un'ampiezza più corta e controllata vale più di una " +
            "bassa con slancio.",
        "builtin:hanging-knee-raise" to
            "Appenditi a una sbarra con le spalle attive. Porta le ginocchia al petto, ruota leggermente il " +
            "bacino e scendi con controllo. Evita di dondolare. Se il corpo inizia a dondolare, fermati in basso " +
            "finché non si ferma prima della ripetizione successiva.",
        "builtin:hanging-leg-raise" to
            "Appenditi a una sbarra e solleva le gambe distese all'altezza dei fianchi o più in alto, ruotando il " +
            "bacino indietro in alto. Scendi lentamente senza dondolare. È la versione difficile del sollevamento " +
            "ginocchia. Inizia con le ginocchia piegate e distendile man mano che acquisti controllo.",
        "builtin:dragon-flag" to
            "Sdraiati su una panchina e afferra dietro la testa, poi solleva il corpo fino alle spalle e abbassalo " +
            "come una linea retta con controllo. Tieni le costole basse e i glutei stretti. È un movimento " +
            "avanzato. Inizia con versioni raccolte o negative e non lasciare mai che la zona lombare si inarchi.",
        "builtin:jumping-jack" to
            "In piedi, braccia lungo i fianchi e piedi uniti. Salta aprendo i piedi oltre la larghezza delle " +
            "spalle mentre le braccia salgono sopra la testa, poi torna al via con lo stesso ritmo. Resta " +
            "sull'avampiede, ginocchia morbide e core attivo. Usalo come riscaldamento: contano frequenza " +
            "cardiaca e spalle, non la velocità. Se l'impatto dà fastidio alle ginocchia, apri i piedi senza " +
            "saltare.",
        "builtin:wall-handstand" to
            "Metti le mani a 15-20 cm dal muro, sali con un calcio o camminando finché il petto è rivolto al muro " +
            "e il corpo è dritto. Spingi nelle spalle, tieni le costole basse e resisti. Scendi con controllo. " +
            "Quando lo tieni comodamente, esercitati con le spalle al muro e i talloni appoggiati.",
        "builtin:handstand-hold" to
            "Sali in handstand libero con le mani alla larghezza delle spalle, le dita aperte e le spalle che " +
            "spingono in alto. Trova l'equilibrio con piccoli aggiustamenti di dita e polsi, mantenendo il corpo " +
            "in linea. Esercitati vicino a un muro e impara a uscire in avanti o di lato. Servono molti tentativi " +
            "brevi, non uno lungo.",
        "builtin:l-sit" to
            "Appoggiati su parallettes o a terra con le braccia distese, le spalle basse e le gambe sollevate a " +
            "90°. Resisti con il core attivo e le punte dei piedi distese. Se le gambe non restano su, piega le " +
            "ginocchia o sollevale una alla volta. Non alzare le spalle; spingi il pavimento.",
        "builtin:tuck-planche" to
            "Metti le mani all'altezza dei fianchi con le dita verso l'esterno, inclina le spalle sopra le mani e " +
            "solleva le ginocchia al petto con i fianchi alti. Resisti con le braccia distese e la parte alta " +
            "della schiena arrotondata. È una progressione della planche. Mantieni l'inclinazione e non lasciare " +
            "che i piedi tocchino terra tra i tentativi.",
        "builtin:tuck-front-lever" to
            "Appenditi a una sbarra e tira le scapole in basso, poi solleva le ginocchia al petto e inclinati " +
            "indietro finché la schiena è più o meno parallela al pavimento. Resisti con le braccia distese e il " +
            "corpo ben raccolto. È una progressione del front lever. Evita che i fianchi scendano e tieni i " +
            "gomiti bloccati; apri la raccolta solo quando la tenuta è solida.",
        "builtin:back-lever" to
            "Da un German hang o da una posizione invertita alla sbarra, abbassa il corpo dietro la sbarra con le " +
            "braccia distese finché è più o meno parallelo al pavimento. Tieni le spalle attive e tutto il corpo " +
            "fermo. È un movimento avanzato. Progredisci con versioni raccolte e a gambe divaricate e riscalda " +
            "bene le spalle.",
    )

    /** The translated text, or null for English/unknown languages (the English description is the fallback). */
    fun text(exerciseId: String, languageCode: String): String? = when (languageCode.lowercase().take(2)) {
        "es" -> es[exerciseId]
        "fr" -> fr[exerciseId]
        "de" -> de[exerciseId]
        "pt" -> pt[exerciseId]
        "it" -> it[exerciseId]
        else -> null
    }
}

/**
 * The how-to text for the current language: the built-in translation when there is one, otherwise the
 * exercise's own description (English for built-ins, whatever the user wrote for a custom exercise).
 */
fun Exercise.descriptionFor(languageCode: String): String =
    if (builtin) BuiltinDescriptions.text(id, languageCode) ?: description else description
