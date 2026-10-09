package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regression tests for HTTP errors observed in the delivered application.
 * @author OpenAI Codex (review and regression tests)
 */
@SpringBootTest
@AutoConfigureMockMvc
class HttpErrorHandlingTest {
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/products?page=-1&size=6", "/api/products?page=0&size=0",
            "/api/products?page=0&size=101"})
    void invalidPaginationIsAJsonBadRequest(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/no-such-page", "/images/no-such.png"})
    void missingPagesAndAssetsAreNotFound(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/no-such-resource"})
    void missingApiResourceIsAJsonNotFound(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }
}
