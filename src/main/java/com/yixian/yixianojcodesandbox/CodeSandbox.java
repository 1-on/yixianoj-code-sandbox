package com.yixian.yixianojcodesandbox;

import com.yixian.yixianojcodesandbox.model.ExecuteCodeRequest;
import com.yixian.yixianojcodesandbox.model.ExecuteCodeResponse;

public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
