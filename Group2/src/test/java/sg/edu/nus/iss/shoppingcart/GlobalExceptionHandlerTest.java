package sg.edu.nus.iss.shoppingcart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import sg.edu.nus.iss.shoppingcart.exception.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void clientAbortDoesNotRenderAnotherResponse() throws Exception {
        mvc.perform(get("/images/disconnected.png"))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andExpect(result -> assertThat(result.getModelAndView()).isNull());
    }

    @Test
    void wrappedClientAbortIsAlsoHandledWithoutAView() throws Exception {
        mvc.perform(get("/images/wrapped-disconnect.png"))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andExpect(result -> assertThat(result.getModelAndView()).isNull());
    }

    @Test
    void realIoFailureStillReturnsServerError() throws Exception {
        mvc.perform(get("/broken-download"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/not-found"))
                .andExpect(model().attribute("errorTitle", "Server error"));
    }

    @Test
    void committedDownloadIsNotReplacedWithAnErrorPage() throws Exception {
        mvc.perform(get("/committed-download"))
                .andExpect(content().string("partial download"))
                .andExpect(result -> assertThat(result.getResponse().isCommitted()).isTrue())
                .andExpect(result -> assertThat(result.getModelAndView()).isNull());
    }

    @Test
    void unexpectedApiFailureReturnsJsonWithoutInternalDetails() throws Exception {
        mvc.perform(get("/api/broken-download"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Something went wrong on our side. Please try again."));
    }

    @Controller
    static class FailingController {
        @GetMapping("/images/disconnected.png")
        void disconnected(HttpServletResponse response) throws IOException {
            response.getOutputStream();
            throw new ClientAbortException(new IOException("Client closed the connection"));
        }

        @GetMapping("/images/wrapped-disconnect.png")
        void wrappedDisconnect() {
            throw new IllegalStateException("Download failed", new ClientAbortException("Client closed the connection"));
        }

        @GetMapping({"/broken-download", "/api/broken-download"})
        void realFailure() throws IOException {
            throw new IOException("Private storage failure details");
        }

        @GetMapping("/committed-download")
        void committedDownload(HttpServletResponse response) throws IOException {
            response.getOutputStream().write("partial download".getBytes(StandardCharsets.UTF_8));
            response.flushBuffer();
            throw new IOException("Private storage failure details");
        }
    }
}
