package com.doclab.doclab.controller;

import com.doclab.doclab.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

// ✅ new import
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired MockMvc mvc;

    // ✅ replace @MockBean with @MockitoBean
    @MockitoBean DocumentService documentService;

    @Test
    void upload_emptyFile_returns400() throws Exception {
        var empty = new MockMultipartFile("file", new byte[0]);
        mvc.perform(multipart("/api/documents/upload")
                        .file(empty)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
        verify(documentService, never()).save(any());
    }


//    @Test
//    void upload_unsupportedType_returns415() throws Exception {
//        var bad = new MockMultipartFile("file","x.bin","application/octet-stream", "x".getBytes());
//        mvc.perform(multipart("/api/documents/upload")
//                        .file(bad)
//                        .param("docType","test")
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isUnsupportedMediaType());
//        verify(documentService, never()).save(any());
//    }
}