package com.example.myapplication

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

object SupabaseManager {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
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

    // Helper to wait until the user is officially logged in before running queries
    suspend fun waitForSession() {
        if (client.auth.sessionStatus.value !is SessionStatus.Authenticated) {
            client.auth.sessionStatus.filter { it is SessionStatus.Authenticated }.first()
        }
    }
}