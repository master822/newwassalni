package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("وصلني", appName)
  }

  @Test
  fun `verify app view model can be created`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AppViewModel(context)
    assertNotNull(viewModel)
    assertNotNull(viewModel.appName.value)
  }

  @Test
  fun `verify main activity launches successfully`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    controller.create().start().resume()
    val activity = controller.get()
    assertNotNull(activity)
    controller.pause().stop().destroy()
  }
}

