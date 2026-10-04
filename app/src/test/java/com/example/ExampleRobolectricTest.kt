package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.snmp.Asn1Ber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NetPulse SNMP", appName)
  }

  @Test
  fun `test asn1 ber get request packet creation`() {
    val req = Asn1Ber.buildGetRequest(
      version = 1,
      community = "public",
      requestId = 1001,
      oids = listOf(".1.3.6.1.2.1.1.1.0")
    )
    assertNotNull(req)
    assertTrue("SNMP request packet should not be empty", req.isNotEmpty())
    assertEquals(0x30.toByte(), req[0]) // Starts with SEQUENCE tag
  }
}
