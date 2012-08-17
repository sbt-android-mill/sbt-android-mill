package sbt.android.mill.helloworldjni

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.view.View.OnClickListener
import android.widget.EditText
import android.view.View

class HelloWorldAndroidJNIActivity extends Activity {
  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    // update UI
    val helloText = HelloWorldAndroidJNI.hello()
    val outText = findViewById(R.id.hello).asInstanceOf[TextView]
    outText.setText(helloText) // text from JNI

    // Setup the UI
    val buttonCalc = findViewById(R.id.buttonCalc).asInstanceOf[Button]
    buttonCalc.setOnClickListener(new OnClickListener() {
      val result = findViewById(R.id.result).asInstanceOf[TextView]
      val value1 = findViewById(R.id.value1).asInstanceOf[EditText]
      val value2 = findViewById(R.id.value2).asInstanceOf[EditText]
      def onClick(v: View) {
        val v1 = Integer.parseInt(value1.getText().toString())
        val v2 = Integer.parseInt(value2.getText().toString())
        val res = HelloWorldAndroidJNI.add(v1, v2)
        result.setText(new Integer(res).toString())
      }
    })
  }
}