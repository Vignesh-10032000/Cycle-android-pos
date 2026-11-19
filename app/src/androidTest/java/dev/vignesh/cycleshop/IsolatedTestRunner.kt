package dev.vignesh.cycleshop

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class IsolatedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(loader: ClassLoader, className: String, context: Context): Application {
        check(context.packageName == TARGET_PACKAGE) {
            "Instrumentation requires the isolated Cycle Shop application ID."
        }
        return super.newApplication(loader, className, context)
    }

    companion object {
        const val TARGET_PACKAGE = "dev.vignesh.cycleshop.isolated"
    }
}
