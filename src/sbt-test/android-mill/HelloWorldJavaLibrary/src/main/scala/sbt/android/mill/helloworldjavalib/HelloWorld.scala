package sbt.android.mill.helloworldjavalib

import android.app.Activity
import android.os.Bundle

class HelloWorld extends Activity
{
    /** Called when the activity is first created. */
    override def onCreate(savedInstanceState : Bundle)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val lib = new sbt.android.mill.androidlib.Lib
        findViewById(android.R.id.text1).asInstanceOf[android.widget.TextView].setText(lib.hello)
        findViewById(android.R.id.text2).asInstanceOf[android.widget.TextView].setText(R.string.hello)
    }
}
