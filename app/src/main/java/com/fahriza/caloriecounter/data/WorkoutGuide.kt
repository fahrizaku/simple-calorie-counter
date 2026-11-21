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

data class IntervalConfig(val workSec: Int, val restSec: Int, val rounds: Int) {
    val cycleSec: Int get() = workSec + restSec
    val totalSec: Int get() = rounds * workSec + (rounds - 1) * restSec
}

data class ComboStep(val workoutName: String, val workSec: Int, val restSec: Int)

data class Combo(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<ComboStep>,
    val rounds: Int,
    val builtIn: Boolean = false
)

data class SessionPhase(
    val workoutName: String,
    val met: Double,
    val isWork: Boolean,
    val seconds: Int,
    val round: Int
)

fun findWorkout(name: String): Workout? = workouts.firstOrNull { it.name == name }

/** Urutan fase kerja/istirahat; istirahat setelah gerakan terakhir pada ronde terakhir dibuang. */
fun buildPhases(combo: Combo): List<SessionPhase> {
    val result = mutableListOf<SessionPhase>()
    for (r in 1..combo.rounds) {
        combo.steps.forEachIndexed { i, step ->
            val w = findWorkout(step.workoutName) ?: return@forEachIndexed
            if (step.workSec > 0) result.add(SessionPhase(w.name, w.met, true, step.workSec, r))
            if (step.restSec > 0) result.add(SessionPhase(w.name, w.met, false, step.restSec, r))
        }
    }
    while (result.isNotEmpty() && !result.last().isWork) result.removeAt(result.lastIndex)
    return result
}

fun comboTotalSeconds(combo: Combo): Int = buildPhases(combo).sumOf { it.seconds }

fun estimateComboKcal(combo: Combo, weightKg: Float): Int =
    Math.round(
        buildPhases(combo).filter { it.isWork }.sumOf { it.met * weightKg * it.seconds / 3600.0 }
    ).toInt()

fun Combo.isHighImpact(): Boolean = steps.any { findWorkout(it.workoutName)?.highImpact == true }

data class WorkoutSession(
    val title: String,
    val met: Double,
    val targetSeconds: Int?,
    val interval: IntervalConfig? = null,
    val phases: List<SessionPhase>? = null,
    val accumulatedMs: Long = 0L,
    val runningSince: Long? = null,
    val finished: Boolean = false
) {
    val isRunning: Boolean get() = runningSince != null

    val totalSeconds: Int?
        get() = interval?.totalSec ?: phases?.sumOf { it.seconds } ?: targetSeconds

    fun elapsedMs(now: Long): Long = accumulatedMs + (runningSince?.let { now - it } ?: 0L)

    fun phaseIndexAt(elapsedSec: Long): Int {
        val list = phases ?: return 0
        var acc = 0L
        list.forEachIndexed { i, p ->
            acc += p.seconds
            if (elapsedSec < acc) return i
        }
        return list.lastIndex
    }

    fun phaseStartSec(index: Int): Long = phases?.take(index)?.sumOf { it.seconds.toLong() } ?: 0L

    /** Berubah setiap pindah fase, dipakai untuk memicu getar singkat. */
    fun phaseKey(elapsedSec: Long): Long {
        val iv = interval
        if (iv != null) {
            return (elapsedSec / iv.cycleSec) * 2 + (if (elapsedSec % iv.cycleSec < iv.workSec) 0 else 1)
        }
        if (phases != null) return phaseIndexAt(elapsedSec).toLong()
        return 0L
    }

    /** Detik latihan yang dihitung kalorinya: seluruh waktu, atau hanya fase kerja pada interval/kombinasi. */
    fun activeSeconds(elapsedSec: Long): Long {
        val list = phases
        if (list != null) {
            var remaining = elapsedSec
            var active = 0L
            for (p in list) {
                if (remaining <= 0) break
                val used = minOf(remaining, p.seconds.toLong())
                if (p.isWork) active += used
                remaining -= used
            }
            return active
        }
        val iv = interval ?: return elapsedSec
        val t = elapsedSec.coerceAtMost(iv.totalSec.toLong())
        val full = t / iv.cycleSec
        val within = t % iv.cycleSec
        return full * iv.workSec + minOf(within, iv.workSec.toLong())
    }

    fun kcal(weightKg: Float, elapsedSec: Long): Int {
        val list = phases ?: return estimateKcal(met, weightKg, activeSeconds(elapsedSec))
        var remaining = elapsedSec
        var total = 0.0
        for (p in list) {
            if (remaining <= 0) break
            val used = minOf(remaining, p.seconds.toLong())
            if (p.isWork) total += p.met * weightKg * used / 3600.0
            remaining -= used
        }
        return Math.round(total).toInt()
    }
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
        name = "Bird Dog",
        met = 2.5,
        focus = "Inti tubuh dan punggung",
        highImpact = false,
        steps = listOf(
            "Merangkak dengan tangan tepat di bawah bahu dan lutut di bawah pinggul.",
            "Luruskan tangan kanan ke depan dan kaki kiri ke belakang sejajar lantai.",
            "Tahan 2 detik tanpa memutar pinggul.",
            "Kembali ke awal, lalu ganti sisi."
        ),
        tips = listOf("Jaga punggung tetap datar dan gerakan pelan.")
    ),
    Workout(
        name = "Dead Bug",
        met = 3.0,
        focus = "Perut dan inti tubuh",
        highImpact = false,
        steps = listOf(
            "Berbaring telentang, lengan lurus ke atas, lutut ditekuk 90 derajat di atas pinggul.",
            "Turunkan lengan kanan dan kaki kiri perlahan mendekati lantai.",
            "Kembali ke posisi awal, lalu ganti sisi.",
            "Napas teratur selama bergerak."
        ),
        tips = listOf("Punggung bawah tetap menempel lantai; kecilkan gerakan jika mulai melengkung.")
    ),
    Workout(
        name = "Calf Raise",
        met = 3.0,
        focus = "Betis dan keseimbangan",
        highImpact = false,
        steps = listOf(
            "Berdiri dengan kaki selebar pinggul, pegang dinding atau kursi.",
            "Naik ke ujung kaki secara perlahan.",
            "Tahan 1 detik di atas.",
            "Turun perlahan ke posisi awal."
        ),
        tips = listOf("Ringan untuk pergelangan kaki; pegangan membantu menjaga keseimbangan.")
    ),
    Workout(
        name = "Wall Sit",
        met = 3.5,
        focus = "Paha dan pinggul",
        highImpact = false,
        steps = listOf(
            "Sandarkan punggung ke dinding, kaki selebar pinggul sejauh satu langkah dari dinding.",
            "Geser turun sampai lutut kira-kira 90 derajat, atau lebih tinggi jika berat.",
            "Pastikan lutut berada di atas pergelangan kaki.",
            "Tahan posisi sambil bernapas teratur."
        ),
        tips = listOf("Jangan turun lebih rendah dari 90 derajat dan berhenti jika lutut nyeri.")
    ),
    Workout(
        name = "Marching Angkat Lengan",
        met = 3.5,
        focus = "Kardio ringan seluruh tubuh",
        highImpact = false,
        steps = listOf(
            "Berdiri tegak, lakukan jalan di tempat dengan lutut terangkat.",
            "Angkat kedua lengan ke atas setiap beberapa langkah, lalu turunkan.",
            "Jaga irama tetap stabil.",
            "Tegakkan dada dan rilekskan bahu."
        ),
        tips = listOf("Alternatif sangat ringan untuk jumping jack.")
    ),
    Workout(
        name = "Side Step",
        met = 3.5,
        focus = "Kardio ringan dan paha samping",
        highImpact = false,
        steps = listOf(
            "Berdiri dengan lutut sedikit ditekuk.",
            "Langkah ke kanan dua kali, lalu ke kiri dua kali.",
            "Ayunkan lengan mengikuti irama langkah.",
            "Pertahankan pinggul tetap rendah dan stabil."
        ),
        tips = listOf("Pastikan lantai tidak licin dan ruang di samping cukup.")
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

private const val N_JALAN = "Jalan Cepat di Tempat"
private const val N_STEP_UP = "Step-up di Anak Tangga"
private const val N_STEP_JACK = "Step Jack (Tanpa Lompat)"
private const val N_BOXING = "Shadow Boxing Pelan"
private const val N_SIT_STAND = "Sit-to-Stand dari Kursi"
private const val N_PUSH_UP = "Push-up Dinding atau Lutut"
private const val N_BRIDGE = "Glute Bridge"
private const val N_PLANK = "Plank di Lutut"
private const val N_YOGA = "Yoga atau Stretching"
private const val N_ROPE = "Lompat Tali"
private const val N_JUMPING_JACK = "Jumping Jack"
private const val N_HIGH_KNEES = "High Knees"
private const val N_SQUAT_JUMP = "Squat Jump"
private const val N_BIRD_DOG = "Bird Dog"
private const val N_DEAD_BUG = "Dead Bug"
private const val N_CALF = "Calf Raise"
private const val N_WALL_SIT = "Wall Sit"
private const val N_MARCHING = "Marching Angkat Lengan"

private fun step(name: String, work: Int, rest: Int) = ComboStep(name, work, rest)

val comboTemplates = listOf(
    Combo(
        id = "tpl_pemanasan",
        name = "Pemanasan",
        description = "Menaikkan detak jantung dan melonggarkan sendi sebelum latihan.",
        steps = listOf(
            step(N_JALAN, 90, 0),
            step(N_MARCHING, 60, 0),
            step(N_STEP_JACK, 60, 0),
            step(N_SIT_STAND, 45, 0),
            step(N_YOGA, 45, 0)
        ),
        rounds = 1,
        builtIn = true
    ),
    Combo(
        id = "tpl_pendinginan",
        name = "Pendinginan",
        description = "Menurunkan detak jantung dan meregangkan otot setelah latihan.",
        steps = listOf(
            step(N_JALAN, 90, 0),
            step(N_YOGA, 210, 0)
        ),
        rounds = 1,
        builtIn = true
    ),
    Combo(
        id = "tpl_pemula",
        name = "Pemula Low-Impact",
        description = "Gerakan dasar tanpa lompatan untuk mulai dari nol.",
        steps = listOf(
            step(N_JALAN, 60, 15),
            step(N_SIT_STAND, 45, 15),
            step(N_STEP_JACK, 45, 15),
            step(N_PUSH_UP, 30, 15),
            step(N_BRIDGE, 30, 15)
        ),
        rounds = 3,
        builtIn = true
    ),
    Combo(
        id = "tpl_ramah_lutut",
        name = "Ramah Lutut",
        description = "Tanpa lompat dan tanpa step-up, cocok untuk lutut atau pinggang yang sensitif.",
        steps = listOf(
            step(N_BRIDGE, 40, 20),
            step(N_PUSH_UP, 30, 20),
            step(N_PLANK, 20, 20),
            step(N_DEAD_BUG, 30, 20),
            step(N_MARCHING, 60, 20)
        ),
        rounds = 3,
        builtIn = true
    ),
    Combo(
        id = "tpl_tubuh_bawah",
        name = "Tubuh Bawah Tanpa Lompat",
        description = "Menguatkan kaki dan pinggul tanpa hentakan.",
        steps = listOf(
            step(N_SIT_STAND, 45, 15),
            step(N_STEP_UP, 45, 15),
            step(N_BRIDGE, 40, 15),
            step(N_WALL_SIT, 20, 20),
            step(N_CALF, 30, 15)
        ),
        rounds = 3,
        builtIn = true
    ),
    Combo(
        id = "tpl_atas_inti",
        name = "Tubuh Atas dan Inti",
        description = "Dada, bahu, perut, dan punggung dengan intensitas ringan.",
        steps = listOf(
            step(N_PUSH_UP, 30, 15),
            step(N_BOXING, 45, 15),
            step(N_PLANK, 20, 15),
            step(N_DEAD_BUG, 30, 15),
            step(N_BIRD_DOG, 30, 15)
        ),
        rounds = 3,
        builtIn = true
    ),
    Combo(
        id = "tpl_snack_gerak",
        name = "Snack Gerak",
        description = "Gerak singkat di sela kerja agar tidak terlalu lama duduk.",
        steps = listOf(
            step(N_SIT_STAND, 40, 10),
            step(N_MARCHING, 60, 10),
            step(N_CALF, 30, 10),
            step(N_YOGA, 60, 0)
        ),
        rounds = 1,
        builtIn = true
    ),
    Combo(
        id = "tpl_pemulihan",
        name = "Pemulihan Ringan",
        description = "Gerak lembut untuk hari setelah latihan berat.",
        steps = listOf(
            step(N_JALAN, 120, 20),
            step(N_YOGA, 90, 20),
            step(N_BRIDGE, 40, 20),
            step(N_BIRD_DOG, 40, 20),
            step(N_MARCHING, 60, 20)
        ),
        rounds = 3,
        builtIn = true
    ),
    Combo(
        id = "tpl_kardio_interval",
        name = "Kardio Interval",
        description = "Interval kerja dan istirahat dengan lompatan, lakukan pemanasan dulu.",
        steps = listOf(
            step(N_JUMPING_JACK, 45, 15),
            step(N_HIGH_KNEES, 45, 15),
            step(N_STEP_JACK, 45, 15),
            step(N_BOXING, 45, 15)
        ),
        rounds = 5,
        builtIn = true
    ),
    Combo(
        id = "tpl_lompat_tali",
        name = "Lompat Tali Interval",
        description = "Lompat tali 60 detik dan istirahat 30 detik, lakukan pemanasan dulu.",
        steps = listOf(
            step(N_ROPE, 60, 30)
        ),
        rounds = 10,
        builtIn = true
    ),
    Combo(
        id = "tpl_bakar_maksimal",
        name = "Bakar Maksimal",
        description = "Untuk yang sudah terbiasa: lompatan bergantian, lakukan pemanasan dulu.",
        steps = listOf(
            step(N_ROPE, 60, 20),
            step(N_SQUAT_JUMP, 40, 20),
            step(N_HIGH_KNEES, 45, 15),
            step(N_JUMPING_JACK, 45, 15)
        ),
        rounds = 6,
        builtIn = true
    )
)
