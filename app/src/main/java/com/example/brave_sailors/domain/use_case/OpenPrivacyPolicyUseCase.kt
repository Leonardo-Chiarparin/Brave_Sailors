package com.example.brave_sailors.domain.use_case

import android.content.Context
import android.content.Intent
import android.net.Uri

class OpenPrivacyPolicyUseCase {
    operator fun invoke(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}