package uk.makemydaylondon.mmd.ui.account

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class PasskeyCredentialsTest {
    @Test
    fun `removes null fields at every level`() {
        val options = Json.parseToJsonElement(
            """
            {"challenge":"abc","timeout":null,
             "authenticatorSelection":{"authenticatorAttachment":null,"residentKey":"required"},
             "excludeCredentials":[{"id":"x","transports":null}]}
            """,
        ) as JsonObject

        val cleaned = PasskeyCredentials.withoutNulls(options)

        assertThat(cleaned).isEqualTo(
            Json.parseToJsonElement(
                """
                {"challenge":"abc",
                 "authenticatorSelection":{"residentKey":"required"},
                 "excludeCredentials":[{"id":"x"}]}
                """,
            ),
        )
    }
}
