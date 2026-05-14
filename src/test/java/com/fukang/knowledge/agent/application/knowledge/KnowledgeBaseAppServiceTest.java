package com.fukang.knowledge.agent.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.api.document.dto.DocumentResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentStatusResp;
import com.fukang.knowledge.agent.api.document.dto.DocumentUploadResp;
import com.fukang.knowledge.agent.api.knowledgebase.dto.CreateKnowledgeBaseReq;
import com.fukang.knowledge.agent.api.knowledgebase.dto.KnowledgeBaseResp;
import com.fukang.knowledge.agent.api.knowledgebase.dto.UpdateKnowledgeBaseReq;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.DocumentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.DocumentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseAppServiceTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

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

    @Test
    void testGetDocumentStatus_Success() {
        Long documentId = 1L;
        DocumentDO document = new DocumentDO();
        document.setId(documentId);
        document.setKnowledgeBaseId(100L);
        document.setTitle("test.pdf");
        document.setFilePath("documents/2026/05/13/abc123.pdf");

        when(documentMapper.selectById(documentId)).thenReturn(document);

        DocumentStatusResp resp = knowledgeBaseAppService.getDocumentStatus(documentId);

        assertNotNull(resp);
        assertEquals("uploaded", resp.status());
        verify(documentMapper).selectById(documentId);
    }

    @Test
    void testGetDocumentStatus_NotFound() {
        Long documentId = 999L;
        when(documentMapper.selectById(documentId)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.getDocumentStatus(documentId));
        assertEquals(ErrorCodeEnum.DOCUMENT_NOT_EXIST.getCode(), ex.getCode());
    }

    @Test
    void testDeleteDocument_Success() {
        Long documentId = 1L;
        DocumentDO document = new DocumentDO();
        document.setId(documentId);
        document.setKnowledgeBaseId(100L);
        document.setTitle("test.pdf");
        document.setFilePath("documents/2026/05/13/abc123.pdf");

        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(documentMapper.deleteById(documentId)).thenReturn(1);

        assertDoesNotThrow(() -> knowledgeBaseAppService.deleteDocument(documentId));

        verify(documentMapper).deleteById(documentId);
        verify(minioStorageService).deleteFile(document.getFilePath());
    }

    @Test
    void testDeleteDocument_NotFound() {
        Long documentId = 999L;
        when(documentMapper.selectById(documentId)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.deleteDocument(documentId));
        assertEquals(ErrorCodeEnum.DOCUMENT_NOT_EXIST.getCode(), ex.getCode());

        verify(documentMapper, never()).deleteById(anyLong());
        verifyNoInteractions(minioStorageService);
    }

    @Test
    void testListDocuments_ByKnowledgeBaseId() {
        Long kbId = 100L;
        DocumentDO doc1 = buildDocumentDoc(1L, kbId, "doc1.pdf", "uploads/doc1.pdf");
        DocumentDO doc2 = buildDocumentDoc(2L, kbId, "doc2.pdf", "uploads/doc2.pdf");

        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Page<DocumentDO> page = inv.getArgument(0);
                    page.setRecords(List.of(doc1, doc2));
                    page.setTotal(2);
                    return page;
                });

        PageResponse<DocumentResp> resp = knowledgeBaseAppService.listDocuments(kbId, 1, 20);

        assertNotNull(resp);
        assertEquals(1, resp.getPage());
        assertEquals(20, resp.getPageSize());
        assertEquals(2, resp.getTotal());
        assertEquals(2, resp.getItems().size());

        DocumentResp first = resp.getItems().get(0);
        assertEquals(1L, first.id());
        assertEquals("doc1.pdf", first.name());
        assertEquals("uploads/doc1.pdf", first.filePath());
        assertEquals(kbId, first.knowledgeBaseId());
        assertEquals("uploaded", first.status());
        assertEquals("1", first.uploadedBy());
    }

    @Test
    void testListDocuments_WithoutKnowledgeBaseId() {
        DocumentDO doc = buildDocumentDoc(1L, 100L, "doc.pdf", "uploads/doc.pdf");

        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Page<DocumentDO> page = inv.getArgument(0);
                    page.setRecords(List.of(doc));
                    page.setTotal(1);
                    return page;
                });

        PageResponse<DocumentResp> resp = knowledgeBaseAppService.listDocuments(null, 1, 10);

        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
    }

    private DocumentDO buildDocumentDoc(Long id, Long kbId, String title, String filePath) {
        DocumentDO doc = new DocumentDO();
        doc.setId(id);
        doc.setKnowledgeBaseId(kbId);
        doc.setTitle(title);
        doc.setFilePath(filePath);
        doc.setUploaderId(1L);
        doc.setCreateTime(LocalDateTime.of(2026, 5, 13, 10, 0));
        doc.setUpdateTime(LocalDateTime.of(2026, 5, 13, 12, 0));
        return doc;
    }

    // ======================== 知识库管理测试 ========================

    @Test
    void testCreateKnowledgeBase_Success() {
        CreateKnowledgeBaseReq req = new CreateKnowledgeBaseReq("测试知识库", "测试描述");

        when(knowledgeBaseMapper.insert(any(KnowledgeBaseDO.class))).thenAnswer(inv -> {
            KnowledgeBaseDO kb = inv.getArgument(0);
            kb.setId(1L);
            return 1;
        });

        Long id = knowledgeBaseAppService.createKnowledgeBase(req);

        assertEquals(1L, id);
        ArgumentCaptor<KnowledgeBaseDO> captor = ArgumentCaptor.forClass(KnowledgeBaseDO.class);
        verify(knowledgeBaseMapper).insert(captor.capture());
        assertEquals("测试知识库", captor.getValue().getName());
        assertEquals("测试描述", captor.getValue().getDescription());
    }

    @Test
    void testGetKnowledgeBase_Success() {
        Long kbId = 1L;
        KnowledgeBaseDO kb = buildKnowledgeBase(kbId, "测试知识库", "描述");

        when(knowledgeBaseMapper.selectById(kbId)).thenReturn(kb);
        when(documentMapper.selectCount(any())).thenReturn(3L);

        KnowledgeBaseResp resp = knowledgeBaseAppService.getKnowledgeBase(kbId);

        assertNotNull(resp);
        assertEquals(kbId, resp.id());
        assertEquals("测试知识库", resp.name());
        assertEquals(3L, resp.documentCount());
    }

    @Test
    void testGetKnowledgeBase_NotFound() {
        when(knowledgeBaseMapper.selectById(999L)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.getKnowledgeBase(999L));
        assertEquals(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST.getCode(), ex.getCode());
    }

    @Test
    void testUpdateKnowledgeBase_Success() {
        Long kbId = 1L;
        KnowledgeBaseDO kb = buildKnowledgeBase(kbId, "旧名称", "旧描述");
        UpdateKnowledgeBaseReq req = new UpdateKnowledgeBaseReq("新名称", null);

        when(knowledgeBaseMapper.selectById(kbId)).thenReturn(kb);

        assertDoesNotThrow(() -> knowledgeBaseAppService.updateKnowledgeBase(kbId, req));
        assertEquals("新名称", kb.getName());
        assertEquals("旧描述", kb.getDescription());
        verify(knowledgeBaseMapper).updateById(kb);
    }

    @Test
    void testUpdateKnowledgeBase_NotFound() {
        when(knowledgeBaseMapper.selectById(999L)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.updateKnowledgeBase(999L,
                        new UpdateKnowledgeBaseReq("新名称", "新描述")));
        assertEquals(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST.getCode(), ex.getCode());
    }

    @Test
    void testDeleteKnowledgeBase_Success() {
        Long kbId = 1L;
        KnowledgeBaseDO kb = buildKnowledgeBase(kbId, "测试知识库", "描述");

        when(knowledgeBaseMapper.selectById(kbId)).thenReturn(kb);
        when(knowledgeBaseMapper.deleteById(kbId)).thenReturn(1);

        assertDoesNotThrow(() -> knowledgeBaseAppService.deleteKnowledgeBase(kbId));
        verify(knowledgeBaseMapper).deleteById(kbId);
    }

    @Test
    void testDeleteKnowledgeBase_NotFound() {
        when(knowledgeBaseMapper.selectById(999L)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> knowledgeBaseAppService.deleteKnowledgeBase(999L));
        assertEquals(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST.getCode(), ex.getCode());
        verify(knowledgeBaseMapper, never()).deleteById(anyLong());
    }

    private KnowledgeBaseDO buildKnowledgeBase(Long id, String name, String description) {
        KnowledgeBaseDO kb = new KnowledgeBaseDO();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(description);
        kb.setCreateTime(LocalDateTime.of(2026, 5, 13, 10, 0));
        kb.setUpdateTime(LocalDateTime.of(2026, 5, 13, 12, 0));
        return kb;
    }
}