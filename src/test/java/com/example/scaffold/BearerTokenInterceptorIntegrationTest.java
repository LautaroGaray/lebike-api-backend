package com.example.scaffold;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BearerTokenInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnResponseDataWhenBearerTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"atest@test\",\"password\":\"test123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(containsString("\"isSuccess\":false")))
                .andExpect(content().string(containsString("\"message\":\"Missing bearer token\"")))
                .andExpect(content().string(containsString("false")));
    }

    @Test
    public void shouldReturnResponseDataWhenBearerTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"atest@test\",\"password\":\"test123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(containsString("\"isSuccess\":false")))
                .andExpect(content().string(containsString("\"message\":\"Invalid or expired bearer token\"")))
                .andExpect(content().string(containsString("false")));
    }
}

