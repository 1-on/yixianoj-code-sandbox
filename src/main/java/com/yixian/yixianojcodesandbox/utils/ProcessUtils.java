package com.yixian.yixianojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.yixian.yixianojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息
     *
     * @param runProcess 进程
     * @param opName     操作名称
     * @return
     */
    public static ExecuteMessage runProcessAdnGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 分批获取进程的输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputList, "\n"));
            } else {
                // 异常退出
                System.out.println(opName + "失败" + exitValue);

                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));

                List<String> outputList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputList, "\n"));
                // 分批获取进程的输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));

                List<String> errorOutpputList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutpputList.add(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutpputList, "\n"));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程获取信息
     *
     * @param runProcess 进程
     * @param opName     操作名称
     * @return
     */
    public static ExecuteMessage runInteractProcessAdnGetMessage(Process runProcess, String opName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            outputStreamWriter.write(StrUtil.join("\n", s) + "\n");
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源回收
            outputStreamWriter.close();
            inputStream.close();
            outputStream.close();
            runProcess.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
