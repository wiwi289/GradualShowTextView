package swu.cx.gradualtext

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import swu.cx.gradualshowtextview.GradualShowTextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gradualShowTextView = findViewById<GradualShowTextView>(R.id.gradual)
        findViewById<Button>(R.id.startbtn).setOnClickListener {
            gradualShowTextView.startAnim()
        }
        findViewById<Button>(R.id.continuebtn).setOnClickListener {
            gradualShowTextView.continueAnim()
        }
        findViewById<Button>(R.id.resetbtn).setOnClickListener {
            gradualShowTextView.resetAnim()
        }
        findViewById<Button>(R.id.stopbtn).setOnClickListener {
            gradualShowTextView.stopAnim()
        }
    }
}