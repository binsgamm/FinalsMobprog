package com.example.myapplication.utils

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object supabaseClient {
    // Using the stable key from your project and adding session persistence
    val supabase = createSupabaseClient(
        supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
        supabaseKey = "sb_publishable_pdEutnY70rVI_FVG6Casaw_03co6UQR"
    ) {
        install(Postgrest)
        install(Auth) {
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
        }
    }
}