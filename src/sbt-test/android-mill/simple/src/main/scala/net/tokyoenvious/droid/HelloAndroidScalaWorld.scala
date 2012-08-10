package net.tokyoenvious.droid

import android.app.Activity
import android.os.Bundle

class HelloAndroidScalaWorld extends Activity
{
    /** Called when the activity is first created. */
    override def onCreate(savedInstanceState : Bundle)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
    }
}
