import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun calculateStreak(completedDates: List<String>): Int {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateList = completedDates.map { LocalDate.parse(it, formatter) }.sorted()

    var streak = 0
    var currentDate = LocalDate.now()

    for (i in dateList.size - 1 downTo 0) {
        val date = dateList[i]
        if (date == currentDate) {
            streak++
            currentDate = currentDate.minusDays(1)
        } else if (date == currentDate.minusDays(1)) {
            streak++
            currentDate = currentDate.minusDays(1)
        } else {
            break
        }
    }

    return streak
}
