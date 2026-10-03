/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers - URL Shortener Tile
 * File: UrlShortenerTileService.kt
 * Description: Quick Settings tile that opens the shortener for the web URL in the clipboard; the link is sent only after the user confirms it there.
 */

package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.LinkPickerActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.UrlShortener

@RequiresApi(Build.VERSION_CODES.N)
class UrlShortenerTileService : BaseTileService() {
    override val isSensitiveTile: Boolean = true

    override fun getTileLabel(): String = getString(R.string.tile_url_shortener)

    override fun getTileSubtitle(): String = getString(R.string.tile_url_shortener_subtitle)

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_link_24)

    override fun getTileState(): Int = Tile.STATE_INACTIVE

    override fun onTileClick() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()

        if (text == null || !UrlShortener.looksLikeWebUrl(text)) {
            Toast.makeText(this, getString(R.string.shorten_qs_no_url_clipboard), Toast.LENGTH_SHORT).show()
            return
        }

        // The clipboard content only leaves the device after the user checked the link and the
        // shortener service in the sheet and tapped "Shorten" there
        val intent =
            Intent(this, LinkPickerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(UrlShortener.sanitizeUrl(text))
                putExtra(LinkPickerActivity.EXTRA_OPEN_SHORTENER, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
