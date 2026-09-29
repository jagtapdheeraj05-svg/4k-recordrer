package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.camera.DeviceCapabilityDetector
import com.example.data.model.VideoProject
import com.example.graphics.FilterType
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
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CinePro 4K", appName)
    }

    @Test
    fun `device inspection returns valid spec`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val spec = DeviceCapabilityDetector.inspectDevice(context)
        assertNotNull(spec)
        assertEquals("Moto G35 5G / Android 14", spec.modelName)
        assertTrue(spec.supports4K30fps)
    }

    @Test
    fun `video project model default 4k checks`() {
        val project = VideoProject(
            title = "Test 4K Shot",
            filePath = "/storage/test.mp4",
            resolutionWidth = 3840,
            resolutionHeight = 2160,
            bitrateMbps = 20,
            is4k = true
        )
        assertEquals(3840, project.resolutionWidth)
        assertEquals(2160, project.resolutionHeight)
        assertEquals(20, project.bitrateMbps)
        assertTrue(project.is4k)
    }

    @Test
    fun `filter presets contain cinematic and vivid`() {
        val cinematic = FilterType.fromId("cinematic")
        assertEquals(FilterType.CINEMATIC, cinematic)
        assertNotNull(cinematic.colorMatrix)
    }
}
