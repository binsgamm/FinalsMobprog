package com.example.myapplication.utils

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object supabaseClient {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://mxxyzcoevcsniinvleos.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im14eHl6Y29ldmNzbmlpbnZsZW9zIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgzMDQ5NDQsImV4cCI6MjA4Mzg4MDk0NH0.1kuZLURUD68d2xcbRtEIOkzIyqdX4XYcVGaMvt7ui6s"
    ) {
        install(Postgrest)
        install(Auth)
    }
}