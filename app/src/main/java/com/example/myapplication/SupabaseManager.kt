package com.example.myapplication

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.plugins.HttpTimeout // REQUIRED IMPORT
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

object SupabaseManager {

    @OptIn(SupabaseInternal::class)
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

            // Corrected: Install the HttpTimeout plugin explicitly
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30000L // 30 seconds
                    connectTimeoutMillis = 30000L
                    socketTimeoutMillis = 30000L
                }
            }
        }
    }

    suspend fun waitForSession() {
        if (client.auth.sessionStatus.value !is SessionStatus.Authenticated) {
            client.auth.sessionStatus.filter { it is SessionStatus.Authenticated }.first()
        }
    }
}