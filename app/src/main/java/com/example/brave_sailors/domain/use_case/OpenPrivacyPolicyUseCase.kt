package com.example.brave_sailors.domain.use_case

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * A use case for opening a URL in an external browser.
 */
class OpenPrivacyPolicyUseCase {
    operator fun invoke(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
