package com.github.aemtoolkit.classicui

import junit.framework.TestCase

class ClassicUiWidgetRepositoryTest : TestCase() {
    fun testProvidesCommonXtypesAndSpecificFields() {
        assertTrue(
            ClassicUiWidgetRepository.all().map { it.xtype }.containsAll(
                listOf("textfield", "pathfield", "multifield", "richtext"),
            ),
        )
        assertTrue(
            ClassicUiWidgetRepository.find("textfield")!!.fields.map { it.name }.containsAll(
                listOf("name", "fieldLabel", "allowBlank", "maxLength"),
            ),
        )
    }
}
