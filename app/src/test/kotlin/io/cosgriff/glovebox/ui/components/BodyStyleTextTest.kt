package io.cosgriff.glovebox.ui.components

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.BodyStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyStyleTextTest {
    @Test
    fun `every body style has its own name`() {
        assertEquals(listOf(R.string.body_style_sedan, R.string.body_style_hatchback), listOf(BodyStyle.SEDAN, BodyStyle.HATCHBACK).map { it.label() })
        assertEquals(BodyStyle.entries.size, BodyStyle.entries.map { it.label() }.toSet().size)
    }
}
