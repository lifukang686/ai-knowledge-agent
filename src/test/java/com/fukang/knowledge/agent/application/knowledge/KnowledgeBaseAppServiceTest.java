package com.fukang.knowledge.agent.application.knowledge;

import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.storage.MinioStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseAppServiceTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private MinioStorageService minioStorageService;

    @InjectMocks
    private KnowledgeBaseAppService knowledgeBaseAppService;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void testUploadDocument_Success() throws Exception {
        Long knowledgeBaseId = 100L;
        MultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf", "test content".getBytes()
        );
        String mockPath = "documents/2026/05/13/abc123.pdf";

        when(minioStorageService.uploadFile(any(MultipartFile.class))).thenReturn(mockPath);
        when(documentMapper.insert(any(DocumentDO.class))).thenAnswer(invocation -> {
            DocumentDO doc = invocation.getArgument(0);
            doc.setId(1L);
            return 1;
        });

        DocumentUploadResp resp = knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file);

        assertNotNull(resp);
        assertEquals(1L, resp.documentId());
        assertEquals("uploaded", resp.status());

        // 验证 documentMapper.insert 被调用且参数正确
        ArgumentCaptor<DocumentDO> captor = ArgumentCaptor.forClass(DocumentDO.class);
        verify(documentMapper).insert(captor.capture());
        DocumentDO insertedDoc = captor.getValue();
        assertEquals(knowledgeBaseId, insertedDoc.getKnowledgeBaseId());
        assertEquals("test-doc.pdf", insertedDoc.getTitle());
        assertEquals(mockPath, insertedDoc.getFilePath());
        assertEquals(1L, insertedDoc.getUploaderId());
    }

    @Test
    void testUploadDocument_FileEmpty() {
        Long knowledgeBaseId = 100L;
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]
        );

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file));
        assertEquals(ErrorCodeEnum.FILE_EMPTY.getCode(), ex.getCode());

        verifyNoInteractions(minioStorageService);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void testUploadDocument_FileNull() {
        Long knowledgeBaseId = 100L;

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.uploadDocument(knowledgeBaseId, null));
        assertEquals(ErrorCodeEnum.FILE_EMPTY.getCode(), ex.getCode());

        verifyNoInteractions(minioStorageService);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void testUploadDocument_FileNameEmpty() {
        Long knowledgeBaseId = 100L;
        MultipartFile file = new MockMultipartFile(
                "file", "", "application/pdf", "content".getBytes()
        );

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file));
        assertEquals(ErrorCodeEnum.FILE_NAME_EMPTY.getCode(), ex.getCode());

        verifyNoInteractions(minioStorageService);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void testUploadDocument_UnsupportedFileType() {
        Long knowledgeBaseId = 100L;
        MultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", new byte[]{1, 2, 3}
        );

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file));
        assertEquals(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());

        verifyNoInteractions(minioStorageService);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void testUploadDocument_MinioUploadFails() {
        Long knowledgeBaseId = 100L;
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes()
        );

        when(minioStorageService.uploadFile(any(MultipartFile.class)))
                .thenThrow(new BaseException(ErrorCodeEnum.FILE_UPLOAD_FAILED));

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.uploadDocument(knowledgeBaseId, file));
        assertEquals(ErrorCodeEnum.FILE_UPLOAD_FAILED.getCode(), ex.getCode());

        verify(minioStorageService).uploadFile(any(MultipartFile.class));
        verifyNoInteractions(documentMapper);
    }

    @Test
    void testUploadDocument_SupportedFileTypes() throws Exception {
        Long knowledgeBaseId = 100L;
        when(minioStorageService.uploadFile(any(MultipartFile.class))).thenReturn("path/doc.txt");
        when(documentMapper.insert(any(DocumentDO.class))).thenAnswer(invocation -> {
            DocumentDO doc = invocation.getArgument(0);
            doc.setId(1L);
            return 1;
        });

        // txt 文件应该被接受
        MultipartFile txtFile = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        DocumentUploadResp resp = knowledgeBaseAppService.uploadDocument(knowledgeBaseId, txtFile);
        assertEquals("uploaded", resp.status());

        // md 文件应该被接受
        MultipartFile mdFile = new MockMultipartFile("file", "README.md", "text/markdown", "# Hello".getBytes());
        resp = knowledgeBaseAppService.uploadDocument(knowledgeBaseId, mdFile);
        assertEquals("uploaded", resp.status());

        // docx 文件应该被接受
        MultipartFile docxFile = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "data".getBytes());
        resp = knowledgeBaseAppService.uploadDocument(knowledgeBaseId, docxFile);
        assertEquals("uploaded", resp.status());
    }
}