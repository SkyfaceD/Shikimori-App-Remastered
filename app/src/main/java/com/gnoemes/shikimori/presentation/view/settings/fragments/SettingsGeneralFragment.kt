package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.Manifest
import android.os.Bundle
import android.os.Environment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.utils.preference
import com.gnoemes.shikimori.utils.prefs
import com.gnoemes.shikimori.utils.putString
import com.kotlinpermissions.KotlinPermissions
import java.io.File


class SettingsGeneralFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        preference(R.string.settings_content_download_folder_key)?.apply {
            updateFolderSummary()
            setOnPreferenceClickListener { checkStoragePermissions();true }
        }
    }

    private fun checkStoragePermissions() {
        KotlinPermissions.with(activity!!)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .onAccepted { showFolderChooserDialog() }
                .ask()
    }

    override val preferenceScreen: Int
        get() = R.xml.preferences_general

    private fun updateFolderSummary() {
        val folder = prefs().getString(SettingsExtras.DOWNLOAD_FOLDER, "")
        val summary =
                if (!folder.isNullOrEmpty()) folder
                else context!!.getString(R.string.settings_content_download_folder_summary)
        preference(SettingsExtras.DOWNLOAD_FOLDER)?.summary = summary
    }

    private fun showFolderChooserDialog() {
        val path = prefs().getString(SettingsExtras.DOWNLOAD_FOLDER, "")!!
        val initialDirectory = try {
            File(path).let { if (it.canWrite()) it else Environment.getExternalStorageDirectory() }
        } catch (e: Exception) {
            Environment.getExternalStorageDirectory()
        }

        MaterialDialog(context!!).show {
            folderChooser(
                    initialDirectory = initialDirectory,
                    allowFolderCreation = true,
                    emptyTextRes = R.string.download_folder_empty,
                    folderCreationLabel = R.string.download_new_folder)
            { dialog, file ->
                prefs().putString(SettingsExtras.DOWNLOAD_FOLDER, file.absolutePath)
                updateFolderSummary()
            }
        }
    }
}


