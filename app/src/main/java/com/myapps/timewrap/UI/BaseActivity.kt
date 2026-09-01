package com.myapps.timewrap.UI

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import java.util.Locale

abstract class BaseActivity : LocalizationActivity() {

    fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun applyEdgeToEdgePadding(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

//    override fun attachBaseContext(newBase: Context) {
//        val localeToSwitchTo = Locale(ApplicationData(newBase!!).language)
//
//        val localeUpdatedContext: ContextWrapper =
//            Utils.updateLocale(newBase, localeToSwitchTo)
//
//        super.attachBaseContext(localeUpdatedContext)
//    }
}