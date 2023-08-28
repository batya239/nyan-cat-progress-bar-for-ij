package org.jetbrains.nyanprogressbar

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import javax.swing.UIManager

class NyanApplicationComponent : Disposable {
    init {
        ApplicationManager
            .getApplication()
            .messageBus
            .connect(this)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { updateProgressBarUi() })
        updateProgressBarUi()
    }

    private fun updateProgressBarUi() {
        UIManager.put("ProgressBarUI", NyanProgressBarUi::class.java.name)
        UIManager.getDefaults()[NyanProgressBarUi::class.java.name] = NyanProgressBarUi::class.java
    }

    override fun dispose() = Unit
}
