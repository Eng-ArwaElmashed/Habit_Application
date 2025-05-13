import com.android.habitapplication.R
import java.io.Serializable

data class AddHabit(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val progress: Int = 0,
    var completedTasks: Int = 0,
    var totalTasks: Int = 0,
    val icon: String = "",
    var completedDates: Int = 0
) : Serializable