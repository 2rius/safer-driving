import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.setPadding
import com.example.saferdriving.R

fun showConnectionTypeDialog(
    context: Context,
    getRequestBluetooth: (onGranted: () -> Unit, onDenied: () -> Unit) -> () -> Unit
) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Choose OBD-II Connection Type")

    val radioGroup = RadioGroup(context)
    radioGroup.orientation = RadioGroup.HORIZONTAL
    radioGroup.gravity = Gravity.CENTER

    val wifiRadioButton = RadioButton(context)
    wifiRadioButton.text = context.getString(R.string.wifi)
    radioGroup.addView(wifiRadioButton)
    wifiRadioButton.isChecked = true

    val bluetoothRadioButton = RadioButton(context)
    bluetoothRadioButton.text = context.getString(R.string.bluetooth)
    radioGroup.addView(bluetoothRadioButton)

    val ipEditText = EditText(context)
    ipEditText.hint = "IP Address (Optional)"

    val portEditText = EditText(context)
    portEditText.hint = "Port (Optional)"

    val linearLayout = LinearLayout(context)
    linearLayout.orientation = LinearLayout.VERTICAL
    linearLayout.addView(radioGroup)
    linearLayout.addView(ipEditText)
    linearLayout.addView(portEditText)
    linearLayout.setPadding(30)

    // Adds a little more space between the buttons themselves
    val radioButtonLayoutParams = RadioGroup.LayoutParams(
        RadioGroup.LayoutParams.WRAP_CONTENT,
        RadioGroup.LayoutParams.WRAP_CONTENT
    )
    radioButtonLayoutParams.setMargins(0, 0, 40, 0)
    bluetoothRadioButton.layoutParams = radioButtonLayoutParams
    wifiRadioButton.layoutParams = radioButtonLayoutParams

    builder.setView(linearLayout)
    builder.setPositiveButton("OK") { _, _ ->
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId

        if (selectedRadioButtonId != -1) {
            val ip = ipEditText.text.toString()
            val port = portEditText.text.toString()

            when (radioGroup.findViewById<RadioButton>(selectedRadioButtonId)) {
                bluetoothRadioButton -> {

                }
                wifiRadioButton -> {

                }
                else -> {

                }
            }
        }
    }

    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.show()

    val request = getRequestBluetooth({}) {
        wifiRadioButton.isChecked = true
        bluetoothRadioButton.isChecked = false
    }

    // Disable port edit text when nothing is selected, enable when wifi is selected
    // Also, request for bluetooth permission if bluetooth is selected
    radioGroup.setOnCheckedChangeListener { _, checkedId ->
        portEditText.visibility = if (checkedId == wifiRadioButton.id) View.VISIBLE else View.GONE

        if (checkedId == bluetoothRadioButton.id)
            request()
    }
}