package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.service.UserService;
import ikun.yc.ycpage.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户接口请求参数绑定测试。
 */
class UserControllerTest {

    /**
     * 验证登录参数可通过 Java 反射参数名正确绑定。
     */
    @Test
    void loginBindsRequestParameters() throws Exception {
        UserService userService = mock(UserService.class); // 登录服务替身
        doReturn(R.success("token")).when(userService)
                .login(any(HttpServletRequest.class), eq("123456"), eq(JwtUtils.DEFAULT_EXPIRE));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build(); // MVC 测试入口

        mockMvc.perform(post("/users/login").param("key", "123456"))
                .andExpect(status().isOk());

        verify(userService).login(any(HttpServletRequest.class), eq("123456"), eq(JwtUtils.DEFAULT_EXPIRE));
    }
}
