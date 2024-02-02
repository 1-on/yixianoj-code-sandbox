package com.yixian.yixianojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.yixian.yixianojcodesandbox.model.ExecuteCodeRequest;
import com.yixian.yixianojcodesandbox.model.ExecuteCodeResponse;
import com.yixian.yixianojcodesandbox.model.ExecuteMessage;
import com.yixian.yixianojcodesandbox.model.JudgeInfo;
import com.yixian.yixianojcodesandbox.security.DefaultSecurityManager;
import com.yixian.yixianojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final Long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new DefaultSecurityManager());
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        // 1)把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        // 2)编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        // 3)执行代码
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        // 4)收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        // 5)文件清理
        boolean b = clearFile(userCodeFile);
        if (!b) {
            log.error("删除文件失败，文件地址 = {}", userCodeFile.getAbsoluteFile());
        }
        return outputResponse;
    }

    /**
     * 2.编译代码得到文件
     *
     * @param userCodeFile 用户代码文件
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process complieProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAdnGetMessage(complieProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 3.执行文件、获取执行结果列表
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            System.out.println(runCmd);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAdnGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAdnGetMessage(runProcess, "运行", inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行cuowu");
            }
        }
        return executeMessageList;
    }

    /**
     * 1.把用户代码保存为文件
     *
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 1)把用户代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        System.out.println(userCodeFile);
        return userCodeFile;
    }

    /**
     * 4.获取输结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            // 存在错误
            if (StrUtil.isNotBlank(errorMessage)) {
                {
                    executeCodeResponse.setMessage(errorMessage);
                    // 用户提交代码执行错误
                    executeCodeResponse.setStatus(3);
                }
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
//        judgeInfo.setMemoryLimit(0L);
        // 要用第三方库，很麻烦 此处不调用
//        judgeInfo.setMemoryLimit();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean clearFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }


    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
