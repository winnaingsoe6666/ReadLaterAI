package com.knowvault.import_;

import com.knowvault.import_.dto.ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportService importService;

    @Test
    void importFacebook_returnsResult() throws Exception {
        ImportResult result = new ImportResult(5, 1, 6);
        when(importService.importFacebookArchive(any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", "data".getBytes());

        mockMvc.perform(multipart("/api/import/facebook").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(5))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.total").value(6));
    }
}
