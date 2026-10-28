package com.cict.tirukkural.data

import android.net.Uri

/** Recitations hosted on the Internet Archive: one MP3 per அதிகாரம். */
const val AUDIO_BASE_URL = "https://archive.org/download/thiru-kkural/"

/** Approximate size of all 133 recordings, used for the "download all" prompt. */
const val TOTAL_AUDIO_MB = 536

enum class Paal(val tamil: String, val english: String) {
    ARAM("அறத்துப்பால்", "Virtue"),
    PORUL("பொருட்பால்", "Wealth"),
    KAMAM("காமத்துப்பால்", "Love"),
}

data class Iyal(val tamil: String, val paal: Paal, val chapters: IntRange)

data class Chapter(
    val number: Int,
    val title: String,
    val fileName: String,
    val durationSec: Int,
    val iyal: Iyal,
) {
    val paal: Paal get() = iyal.paal
    val firstKural: Int get() = (number - 1) * 10 + 1
    val lastKural: Int get() = number * 10
    val kuralRange: String get() = "$firstKural–$lastKural"
    val mediaId: String get() = mediaIdFor(number)
    val audioUrl: String get() = AUDIO_BASE_URL + Uri.encode("$fileName.mp3")

    /** Latin transliteration taken from the archive file name, e.g. "Kadavul Vazhthu". */
    val transliteration: String
        get() = fileName.substringAfter(' ')
            .lowercase()
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    companion object {
        private const val MEDIA_ID_PREFIX = "adhikaram-"
        fun mediaIdFor(number: Int) = "$MEDIA_ID_PREFIX$number"
        fun numberFromMediaId(mediaId: String?): Int? =
            mediaId?.removePrefix(MEDIA_ID_PREFIX)?.toIntOrNull()?.takeIf { it in 1..Tirukkural.CHAPTER_COUNT }
    }
}

object Tirukkural {
    const val CHAPTER_COUNT = 133
    const val KURAL_COUNT = 1330

    val iyals: List<Iyal> = listOf(
        Iyal("பாயிரம்", Paal.ARAM, 1..4),
        Iyal("இல்லறவியல்", Paal.ARAM, 5..24),
        Iyal("துறவறவியல்", Paal.ARAM, 25..37),
        Iyal("ஊழியல்", Paal.ARAM, 38..38),
        Iyal("அரசியல்", Paal.PORUL, 39..63),
        Iyal("அமைச்சியல்", Paal.PORUL, 64..73),
        Iyal("அரணியல்", Paal.PORUL, 74..75),
        Iyal("கூழியல்", Paal.PORUL, 76..76),
        Iyal("படையியல்", Paal.PORUL, 77..78),
        Iyal("நட்பியல்", Paal.PORUL, 79..95),
        Iyal("குடியியல்", Paal.PORUL, 96..108),
        Iyal("களவியல்", Paal.KAMAM, 109..115),
        Iyal("கற்பியல்", Paal.KAMAM, 116..133),
    )

    private val titles = arrayOf(
        // அறத்துப்பால் — பாயிரம்
        "கடவுள் வாழ்த்து", "வான்சிறப்பு", "நீத்தார் பெருமை", "அறன் வலியுறுத்தல்",
        // இல்லறவியல்
        "இல்வாழ்க்கை", "வாழ்க்கைத் துணைநலம்", "மக்கட்பேறு", "அன்புடைமை", "விருந்தோம்பல்",
        "இனியவை கூறல்", "செய்ந்நன்றி அறிதல்", "நடுவு நிலைமை", "அடக்கம் உடைமை", "ஒழுக்கம் உடைமை",
        "பிறனில் விழையாமை", "பொறையுடைமை", "அழுக்காறாமை", "வெஃகாமை", "புறங்கூறாமை",
        "பயனில சொல்லாமை", "தீவினையச்சம்", "ஒப்புரவறிதல்", "ஈகை", "புகழ்",
        // துறவறவியல்
        "அருளுடைமை", "புலால் மறுத்தல்", "தவம்", "கூடாவொழுக்கம்", "கள்ளாமை", "வாய்மை",
        "வெகுளாமை", "இன்னா செய்யாமை", "கொல்லாமை", "நிலையாமை", "துறவு", "மெய்யுணர்தல்",
        "அவா அறுத்தல்",
        // ஊழியல்
        "ஊழ்",
        // பொருட்பால் — அரசியல்
        "இறைமாட்சி", "கல்வி", "கல்லாமை", "கேள்வி", "அறிவுடைமை", "குற்றங்கடிதல்",
        "பெரியாரைத் துணைக்கோடல்", "சிற்றினம் சேராமை", "தெரிந்து செயல்வகை", "வலியறிதல்",
        "காலம் அறிதல்", "இடன் அறிதல்", "தெரிந்து தெளிதல்", "தெரிந்து வினையாடல்", "சுற்றந் தழால்",
        "பொச்சாவாமை", "செங்கோன்மை", "கொடுங்கோன்மை", "வெருவந்த செய்யாமை", "கண்ணோட்டம்",
        "ஒற்றாடல்", "ஊக்கம் உடைமை", "மடியின்மை", "ஆள்வினை உடைமை", "இடுக்கண் அழியாமை",
        // அமைச்சியல்
        "அமைச்சு", "சொல்வன்மை", "வினைத்தூய்மை", "வினைத்திட்பம்", "வினைசெயல்வகை", "தூது",
        "மன்னரைச் சேர்ந்தொழுதல்", "குறிப்பறிதல்", "அவையறிதல்", "அவை அஞ்சாமை",
        // அரணியல், கூழியல், படையியல்
        "நாடு", "அரண்", "பொருள்செயல்வகை", "படைமாட்சி", "படைச்செருக்கு",
        // நட்பியல்
        "நட்பு", "நட்பாராய்தல்", "பழைமை", "தீ நட்பு", "கூடா நட்பு", "பேதைமை", "புல்லறிவாண்மை",
        "இகல்", "பகைமாட்சி", "பகைத்திறந் தெரிதல்", "உட்பகை", "பெரியாரைப் பிழையாமை",
        "பெண்வழிச் சேறல்", "வரைவின் மகளிர்", "கள்ளுண்ணாமை", "சூது", "மருந்து",
        // குடியியல்
        "குடிமை", "மானம்", "பெருமை", "சான்றாண்மை", "பண்புடைமை", "நன்றியில் செல்வம்",
        "நாணுடைமை", "குடிசெயல்வகை", "உழவு", "நல்குரவு", "இரவு", "இரவச்சம்", "கயமை",
        // காமத்துப்பால் — களவியல்
        "தகையணங்குறுத்தல்", "குறிப்பறிதல்", "புணர்ச்சி மகிழ்தல்", "நலம் புனைந்துரைத்தல்",
        "காதற் சிறப்புரைத்தல்", "நாணுத் துறவுரைத்தல்", "அலர் அறிவுறுத்தல்",
        // கற்பியல்
        "பிரிவாற்றாமை", "படர்மெலிந்திரங்கல்", "கண்விதுப்பழிதல்", "பசப்புறு பருவரல்",
        "தனிப்படர் மிகுதி", "நினைந்தவர் புலம்பல்", "கனவுநிலை உரைத்தல்", "பொழுதுகண்டு இரங்கல்",
        "உறுப்புநலன் அழிதல்", "நெஞ்சொடு கிளத்தல்", "நிறையழிதல்", "அவர்வயின் விதும்பல்",
        "குறிப்பறிவுறுத்தல்", "புணர்ச்சி விதும்பல்", "நெஞ்சொடு புலத்தல்", "புலவி",
        "புலவி நுணுக்கம்", "ஊடலுவகை",
    )

    /** Archive file name (without ".mp3") and duration in seconds, in chapter order. */
    private val audio = arrayOf(
        "001 KADAVUL VAZHTHU" to 251,
        "002 VAAN SIRAPPU" to 231,
        "003 NEETHAR PERUMAI" to 235,
        "004 ARAN VALIYURUTHAL" to 251,
        "005 ILL VAZHKKAI" to 255,
        "006 VAZHKKAI THUNAINALAM" to 242,
        "007 MAKKATPERU" to 243,
        "008 ANBUDAIMAI" to 246,
        "009 VIRUNDHOMBAL" to 243,
        "010 INIYAVAI KOORAL" to 242,
        "011 SEINANDRI ARIDHAL" to 267,
        "012 NADUVU NILAIMAI" to 258,
        "013 ADAKKAM UDAIMAI" to 275,
        "014 OZHUKKAM UDAIMAI" to 244,
        "015 PIRAN ILL VIZHAIYAMAI" to 260,
        "016 PORAI UDAIMAI" to 241,
        "017 AZHUKKARAMAI" to 241,
        "018 VEKKAMAI" to 247,
        "019 PURANG KOORAMAI" to 257,
        "020 PAYAN ILA SOLLAMAI" to 229,
        "021 THEEVINAI ACHCHAM" to 245,
        "022 OPPURAVU ARIDHAL" to 280,
        "023 EHAI" to 283,
        "024 PUGAZH" to 272,
        "025 ARUL UDAIMAI" to 291,
        "026 PULAL MARUTHAL" to 299,
        "027 THAVAM" to 278,
        "028 KOODA OZHUKKAM" to 281,
        "029 KALLAMAI" to 273,
        "030 VAIMAI" to 257,
        "031 VEHULAMAI" to 261,
        "032 INNAH SEIYAMAI" to 254,
        "033 KOLLAMAI" to 263,
        "034 NILAIYAMAI" to 272,
        "035 THURAVU" to 265,
        "036 MEI UNARTHAL" to 265,
        "037 AVAA ARUTHAL" to 263,
        "038 OOZH" to 272,
        "039 IRAI MATCHI" to 305,
        "040 KALVI" to 276,
        "041 KALLAMAI" to 267,
        "042 KELVI" to 260,
        "043 ARIVU UDAIMAI" to 270,
        "044 KUTRAM KADIDHAL" to 261,
        "045 PERIYARAI THUNAIKKODAL" to 262,
        "046 SITRINAM SERAMAI" to 265,
        "047 THERINDHU SEYALVAGAI" to 256,
        "048 VALI ARIDHAL" to 264,
        "049 KAALAM ARIDHAL" to 260,
        "050 IDAN ARIDHAL" to 269,
        "051 THERINDHU THELITHAL" to 286,
        "052 THERINDHU VINAIYADAL" to 289,
        "053 SUTTRAM THAZHAAL" to 279,
        "054 POCHAVAAMAI" to 285,
        "055 SENGKONMAI" to 272,
        "056 KODUNGKONMAI" to 277,
        "057 VERUVANDHA SEIYAMAI" to 278,
        "058 KANNOTTAM" to 254,
        "059 OTRADAL" to 270,
        "060 OOKKAM UDAIMAI" to 262,
        "061 MADI INNMAI" to 250,
        "062 AALVINAI UDAIMAI" to 256,
        "063 IDUKKANN AZHIYAMAI" to 249,
        "064 AMAITCHU" to 260,
        "065 SOLVANMAI" to 266,
        "066 VINAITH THOOIMAI" to 252,
        "067 VINAITH THITPAM" to 245,
        "068 VINAI SEYALVAGAI" to 259,
        "069 THOODHU" to 276,
        "070 MANNARAICH SERNTHIZHUGAL" to 254,
        "071 KURIPPU ARIDHAL" to 257,
        "072 AVAI ARIDHAL" to 262,
        "073 AVAI ANJAMAI" to 257,
        "074 NAADU" to 282,
        "075 ARAN" to 273,
        "076 PORUL SEYALVAGAI" to 265,
        "077 PADAI MATCHI" to 263,
        "078 PADAICH CHERUKKU" to 279,
        "079 NATPU" to 271,
        "080 NATPU AARAIDHAL" to 264,
        "081 PAZHAMAI" to 263,
        "082 THEE NATPU" to 263,
        "083 KOODA NATPU" to 265,
        "084 PETHAIMAI" to 269,
        "085 PULLARIVANMAI" to 267,
        "086 IGAL" to 250,
        "087 PAGAI MATCHI" to 267,
        "088 PAGAITHIRAM THERIDHAL" to 256,
        "089 UTPAGAI" to 260,
        "090 PERIYARAIPPIZHAIYAMAI" to 258,
        "091 PENN VAZHICHERAL" to 265,
        "092 VARAIVIL MAGALIR" to 267,
        "093 KALL UNNAMAI" to 273,
        "094 SOODHU" to 274,
        "095 MARUNDHU" to 274,
        "096 KUDIMAI" to 269,
        "097 MAANAM" to 253,
        "098 PERUMAI" to 256,
        "099 SANDRANMAI" to 273,
        "100 PANBUDAIMAI" to 258,
        "101 NANDRI ILL SELVAM" to 282,
        "102 NAAN UDAIMAI" to 289,
        "103 KUDI SEYALVAGAI" to 265,
        "104 UZHAVU" to 274,
        "105 NALKURAVU" to 259,
        "106 IRAVU" to 268,
        "107 IRVU ACHCHAM" to 257,
        "108 KAYAMAI" to 264,
        "109 THAGAIANANG GURUTHAL" to 255,
        "110 KURIPPU ARIDHAL" to 260,
        "111 PUNARCHI MAGIZHDHAL" to 262,
        "112 NALAM PUNAINDHU URAITHAL" to 259,
        "113 KADHAR SIRAPPU URAITHAL" to 262,
        "114 NANOOTHURAVU URAITHAL" to 258,
        "115 ALAR ARIURUTHAL" to 276,
        "116 PIRIVU AATRAMAI" to 271,
        "117 PADAR MELINDHU IRANGAL" to 277,
        "118 KANN VIDUPPU AZHIDHAL" to 283,
        "119 PASAPPU URU PARUVARAL" to 276,
        "120 THANIPPADAR MIGUDHI" to 263,
        "121 NINAINDHU AVAR PULAMBAL" to 267,
        "122 KANAVUNILAI URAITHAL" to 256,
        "123 POZHUDHU KANDU IRANGAL" to 259,
        "124 URUPPU NALAN AZHIDHAL" to 267,
        "125 NENJODU KILATHTHAL" to 271,
        "126 NIRAI AZHIDHAL" to 261,
        "127 AVAR VAYIN VIDHUMBAL" to 276,
        "128 KURIPPU ARIVURRUTHAL" to 260,
        "129 PUNARCHI VIDHUMBAL" to 257,
        "130 NENJODU PULATHTHAL" to 258,
        "131 PULAVI" to 256,
        "132 PULAVI NUNUKKAM" to 273,
        "133 OODAL UVAGAI" to 250,
    )

    val chapters: List<Chapter> = run {
        check(titles.size == CHAPTER_COUNT && audio.size == CHAPTER_COUNT)
        (1..CHAPTER_COUNT).map { n ->
            val (file, seconds) = audio[n - 1]
            Chapter(
                number = n,
                title = titles[n - 1],
                fileName = file,
                durationSec = seconds,
                iyal = iyals.first { n in it.chapters },
            )
        }
    }

    val totalDurationSec: Int = chapters.sumOf { it.durationSec }

    fun chapter(number: Int): Chapter = chapters[number - 1]

    /** Chapter that contains the given kural number (1..1330). */
    fun chapterForKural(kural: Int): Chapter? =
        if (kural in 1..KURAL_COUNT) chapters[(kural - 1) / 10] else null
}
