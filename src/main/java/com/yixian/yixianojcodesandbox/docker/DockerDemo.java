package com.yixian.yixianojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException, IOException {
//        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
//        dockerClient.pingCmd().exec();

        // 拉取镜像
//        dockerClient.pullImageCmd("nginx:latest").exec(new PullImageResultCallback()).awaitCompletion();

        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println(item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//
//        System.out.println("下载完成");
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd.withCmd("echo", "hello docker").exec();
        System.out.println(createContainerResponse);

        String containerId = createContainerResponse.getId();

        // 查看容器状态
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> exec = listContainersCmd.withShowAll(true).exec();
        for (Container container : exec) {
            System.out.println(container);
        }
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        // 查看日志
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                System.out.println(item.getStreamType());
                System.out.println("日志：" + new String(item.getPayload()));
                super.onNext(item);
            }
        };

        // 阻塞等待日志输出
        dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();
        // 删除容器
        System.out.println("容器id：" + containerId);
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();

        // 删除容器
        dockerClient.removeImageCmd(image).exec();
        dockerClient.close();
    }
}
