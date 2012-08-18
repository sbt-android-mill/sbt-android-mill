package sbt.android.mill.helloworldeclipse

import android.app.Activity
import android.os.Bundle

class HelloWorld extends Activity
{
    /** Called when the activity is first created. */
    override def onCreate(savedInstanceState : Bundle)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
    }
}
