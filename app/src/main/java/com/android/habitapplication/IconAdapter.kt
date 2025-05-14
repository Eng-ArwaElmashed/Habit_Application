import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.util.TypedValue

class IconAdapter(private val context: Context, private val iconNames: List<String>) : BaseAdapter() {

    override fun getCount(): Int = iconNames.size

    override fun getItem(position: Int): Any = iconNames[position]

    override fun getItemId(position: Int): Long = position.toLong()

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        val iconSize = dpToPx(40)
        val padding = dpToPx(4)

        if (convertView == null) {
            imageView = ImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(iconSize, iconSize)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.setPadding(padding, padding, padding, padding)
        } else {
            imageView = convertView as ImageView
            imageView.layoutParams = ViewGroup.LayoutParams(iconSize, iconSize)
            imageView.setPadding(padding, padding, padding, padding)
        }

        val iconResId = context.resources.getIdentifier(iconNames[position], "drawable", context.packageName)
        if (iconResId != 0) {
            imageView.setImageResource(iconResId)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        return imageView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        val iconSize = dpToPx(56)
        val padding = dpToPx(8)

        if (convertView == null) {
            imageView = ImageView(context)
            imageView.layoutParams = ViewGroup.LayoutParams(iconSize, iconSize)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.setPadding(padding, padding, padding, padding)
        } else {
            imageView = convertView as ImageView
            imageView.layoutParams = ViewGroup.LayoutParams(iconSize, iconSize)
            imageView.setPadding(padding, padding, padding, padding)
        }

        val iconResId = context.resources.getIdentifier(iconNames[position], "drawable", context.packageName)
        if (iconResId != 0) {
            imageView.setImageResource(iconResId)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        return imageView
    }
}
