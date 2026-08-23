package com.example.scaffold;

import com.example.scaffold.domain.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuildDummyTest {

    @Test
    public void shouldPassBuildSmokeTest() {
        assertTrue(true);
    }

    @Test
    public void shouldKeepExpectedRoleConstants() {
        assertEquals("ADMIN", Role.ADMIN);
    }
}


