package com.yixian.yixianojcodesandbox;

import com.yixian.yixianojcodesandbox.model.ExecuteCodeRequest;
import com.yixian.yixianojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * java原生实现代码沙箱（直接服用模板方法）
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
