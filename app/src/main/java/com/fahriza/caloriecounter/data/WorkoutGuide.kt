package com.fahriza.caloriecounter.data

const val DEFAULT_WEIGHT_KG = 65f

data class Workout(
    val name: String,
    val met: Double,
    val focus: String,
    val highImpact: Boolean,
    val steps: List<String>,
    val tips: List<String>
)

data class WorkoutSession(
    val workout: Workout,
    val targetSeconds: Int?,
    val accumulatedMs: Long = 0L,
    val runningSince: Long? = null,
    val finished: Boolean = false
) {
    val isRunning: Boolean get() = runningSince != null

    fun elapsedMs(now: Long): Long = accumulatedMs + (runningSince?.let { now - it } ?: 0L)
}

fun estimateKcal(met: Double, weightKg: Float, activeSeconds: Long): Int =
    Math.round(met * weightKg * activeSeconds / 3600.0).toInt()

val highImpactWarnings = listOf(
    "Pemanasan 3-5 menit dulu, lalu pendinginan setelahnya.",
    "Pakai sepatu dan alas yang empuk, hindari lantai keras atau keramik langsung.",
    "Mendarat di ujung kaki dengan lutut sedikit ditekuk.",
    "Mulai dengan interval pendek (30-60 detik) lalu istirahat, jangan langsung nonstop.",
    "Berhenti jika nyeri di lutut, tulang kering, atau pergelangan kaki.",
    "Jika sedang cedera atau berat badan tinggi, pilih latihan low-impact."
)

val workouts = listOf(
    Workout(
        name = "Jalan Cepat di Tempat",
        met = 3.5,
        focus = "Kardio ringan",
        highImpact = false,
        steps = listOf(
            "Berdiri tegak, bahu rileks.",
            "Angkat lutut bergantian setinggi pinggul atau lebih rendah.",
            "Ayunkan lengan berlawanan dengan kaki.",
            "Jaga napas teratur dan tempo stabil."
        ),
        tips = listOf("Paling ringan untuk lutut, cocok sebagai pemanasan.")
    ),
    Workout(
        name = "Step-up di Anak Tangga",
        met = 4.5,
        focus = "Kardio dan kaki",
        highImpact = false,
        steps = listOf(
            "Berdiri menghadap anak tangga yang kokoh.",
            "Naikkan satu kaki penuh ke anak tangga, lalu susul dengan kaki lain.",
            "Turun satu per satu dengan terkontrol.",
            "Ganti kaki yang memimpin setiap beberapa putaran."
        ),
        tips = listOf("Pegang pegangan tangga jika perlu.", "Seluruh telapak kaki menapak, jangan hanya ujung.")
    ),
    Workout(
        name = "Step Jack (Tanpa Lompat)",
        met = 4.0,
        focus = "Kardio seluruh tubuh",
        highImpact = false,
        steps = listOf(
            "Mulai dengan kaki rapat.",
            "Melangkah ke samping dengan satu kaki sambil mengangkat kedua lengan.",
            "Kembali ke posisi awal, lalu ganti sisi.",
            "Ulangi dengan irama teratur."
        ),
        tips = listOf("Pengganti jumping jack yang lebih aman untuk sendi.")
    ),
    Workout(
        name = "Shadow Boxing Pelan",
        met = 4.5,
        focus = "Kardio, bahu, dan inti tubuh",
        highImpact = false,
        steps = listOf(
            "Berdiri dengan kaki selebar bahu, lutut sedikit ditekuk.",
            "Pukul ke depan bergantian dengan kepalan rileks.",
            "Putar pinggul sedikit setiap pukulan.",
            "Tarik tangan kembali ke dekat dagu."
        ),
        tips = listOf("Jangan kunci siku saat meluruskan lengan.")
    ),
    Workout(
        name = "Sit-to-Stand dari Kursi",
        met = 3.5,
        focus = "Kaki dan pinggul",
        highImpact = false,
        steps = listOf(
            "Duduk di tepi kursi yang kokoh, kaki selebar pinggul.",
            "Berdiri tanpa berpegangan, dorong dari tumit.",
            "Duduk perlahan dengan terkontrol.",
            "Ulangi tanpa menjatuhkan badan ke kursi."
        ),
        tips = listOf("Versi ringan squat; lutut searah dengan jari kaki.")
    ),
    Workout(
        name = "Push-up Dinding atau Lutut",
        met = 3.0,
        focus = "Dada, bahu, dan lengan",
        highImpact = false,
        steps = listOf(
            "Letakkan tangan di dinding setinggi dada (atau lutut di lantai).",
            "Tubuh lurus dari kepala sampai kaki (atau lutut).",
            "Tekuk siku dan dekatkan dada ke dinding atau lantai.",
            "Dorong kembali ke posisi awal."
        ),
        tips = listOf("Lebih aman untuk bahu dan siku daripada push-up penuh.")
    ),
    Workout(
        name = "Glute Bridge",
        met = 3.0,
        focus = "Pinggul dan punggung bawah",
        highImpact = false,
        steps = listOf(
            "Berbaring telentang, lutut ditekuk, telapak kaki di lantai.",
            "Angkat pinggul sampai badan lurus dari bahu ke lutut.",
            "Tahan 1-2 detik sambil kencangkan bokong.",
            "Turunkan perlahan."
        ),
        tips = listOf("Jangan melengkungkan punggung bawah berlebihan.")
    ),
    Workout(
        name = "Plank di Lutut",
        met = 3.0,
        focus = "Perut dan inti tubuh",
        highImpact = false,
        steps = listOf(
            "Bertumpu pada lengan bawah dan lutut.",
            "Badan membentuk garis lurus dari kepala ke lutut.",
            "Kencangkan perut dan tahan posisi.",
            "Istirahat sebentar jika bentuk mulai turun."
        ),
        tips = listOf("Alasi lutut dengan matras agar nyaman.")
    ),
    Workout(
        name = "Yoga atau Stretching",
        met = 2.5,
        focus = "Fleksibilitas, pemanasan, dan pendinginan",
        highImpact = false,
        steps = listOf(
            "Lakukan peregangan leher, bahu, punggung, dan kaki secara perlahan.",
            "Tahan tiap gerakan 20-30 detik.",
            "Bernapas dalam dan teratur.",
            "Jangan memaksa sampai terasa nyeri."
        ),
        tips = listOf("Cocok sebelum dan sesudah latihan lain.")
    ),
    Workout(
        name = "Lompat Tali",
        met = 10.0,
        focus = "Kardio intens seluruh tubuh",
        highImpact = true,
        steps = listOf(
            "Pegang tali dengan lengan dekat badan, siku di samping pinggang.",
            "Putar tali dengan pergelangan tangan, bukan seluruh lengan.",
            "Lompat rendah saja, cukup agar tali lewat.",
            "Mendarat di ujung kaki dengan lutut sedikit ditekuk."
        ),
        tips = listOf("Pakai Jeda saat istirahat supaya kalori hanya dihitung saat melompat.")
    ),
    Workout(
        name = "Jumping Jack",
        met = 8.0,
        focus = "Kardio seluruh tubuh",
        highImpact = true,
        steps = listOf(
            "Mulai berdiri dengan kaki rapat, lengan di samping.",
            "Lompat sambil membuka kaki dan mengangkat lengan ke atas.",
            "Lompat kembali ke posisi awal.",
            "Jaga irama dan napas."
        ),
        tips = listOf("Versi low-impact: Step Jack.")
    ),
    Workout(
        name = "High Knees",
        met = 8.0,
        focus = "Kardio dan kaki",
        highImpact = true,
        steps = listOf(
            "Berdiri tegak, kaki selebar pinggul.",
            "Berlari di tempat sambil mengangkat lutut setinggi pinggul.",
            "Ayunkan lengan berlawanan dengan kaki.",
            "Mendarat ringan di ujung kaki."
        ),
        tips = listOf("Versi low-impact: Jalan Cepat di Tempat.")
    ),
    Workout(
        name = "Squat Jump",
        met = 8.0,
        focus = "Kaki dan kardio",
        highImpact = true,
        steps = listOf(
            "Turun ke posisi squat, punggung tegak.",
            "Dorong dari tumit dan lompat setinggi yang nyaman.",
            "Mendarat lembut dan langsung turun ke squat berikutnya.",
            "Lutut selalu searah dengan jari kaki."
        ),
        tips = listOf("Versi low-impact: Sit-to-Stand dari Kursi.")
    )
)
