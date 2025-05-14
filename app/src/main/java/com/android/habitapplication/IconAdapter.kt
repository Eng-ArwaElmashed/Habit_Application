import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class IconAdapter(private val context: Context, private val iconNames: List<String>) : BaseAdapter() {

    override fun getCount(): Int = iconNames.size

    override fun getItem(position: Int): Any = iconNames[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = ImageView(context)
        val iconResId = context.resources.getIdentifier(iconNames[position], "drawable", context.packageName)
        imageView.setImageResource(iconResId)
        imageView.layoutParams = ViewGroup.LayoutParams(100, 100)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
    }
}
