package com.github.aemtoolkit.server

import kotlin.test.Test
import kotlin.test.assertEquals

class AemJsonFieldsTest {
    @Test
    fun `reads string and boolean jcr fields`() {
        val json = """{"cq:lastReplicationAction":"Activate","cq:isLiveRelationship":true}"""
        assertEquals("Activate", AemJsonFields.read(json, "cq:lastReplicationAction"))
        assertEquals("true", AemJsonFields.read(json, "cq:isLiveRelationship"))
    }
}
