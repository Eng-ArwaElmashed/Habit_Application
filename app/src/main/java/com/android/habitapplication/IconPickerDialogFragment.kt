import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.habitapplication.R

class IconPickerDialogFragment(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.icon_picker, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewIcons)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.adapter = IconAdapter(icons) {
            onIconSelected(it)
            dismiss()
        }
        return view
    }
}
