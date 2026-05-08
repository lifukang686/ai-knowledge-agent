package com.fukang.knowledge.agent.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.api.auth.dto.LoginReq;
import com.fukang.knowledge.agent.api.auth.dto.LoginResp;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthAppService authAppService;

    @Test
    void testLogin_Success() {
        LoginReq req = new LoginReq("testuser", "password123");
        UserDO mockUser = new UserDO();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        LoginResp resp = authAppService.login(req);

        assertNotNull(resp);
        assertEquals("mock-token-123456", resp.token());
        assertEquals(1L, resp.userId());
        assertEquals("testuser", resp.username());
    }

    @Test
    void testLogin_UserNotExist() {
        LoginReq req = new LoginReq("unknown", "123");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BaseException.class, () -> authAppService.login(req));
    }
}
