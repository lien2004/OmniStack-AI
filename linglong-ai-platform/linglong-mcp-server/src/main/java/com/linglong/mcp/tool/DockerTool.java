package com.linglong.mcp.tool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Docker操作工具
 * 支持Docker镜像构建、容器运行等操作
 */
@Component
public class DockerTool {

    private static final Logger log = LoggerFactory.getLogger(DockerTool.class);

    private DockerClient dockerClient;

    public DockerTool() {
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("unix:///var/run/docker.sock")
                    .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            dockerClient = DockerClientImpl.getInstance(config, httpClient);
            log.info("Docker客户端初始化成功");
        } catch (Exception e) {
            log.warn("Docker客户端初始化失败(可能Docker未运行): {}", e.getMessage());
        }
    }

    /**
     * 构建Docker镜像
     *
     * @param request 工具执行请求，包含dockerfilePath、imageName、imageTag、buildContext参数
     * @return 构建结果
     */
    @MCPTool(
            name = "docker_build",
            description = "根据Dockerfile构建Docker镜像",
            category = "docker"
    )
    public ToolExecutionResult dockerBuild(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化，请检查Docker是否运行");
        }

        Map<String, Object> params = request.getParameters();
        String dockerfilePath = (String) params.get("dockerfilePath");
        String imageName = (String) params.get("imageName");
        String imageTag = (String) params.getOrDefault("imageTag", "latest");
        String buildContext = (String) params.getOrDefault("buildContext", ".");

        if (dockerfilePath == null || dockerfilePath.isEmpty()) {
            return ToolExecutionResult.error("dockerfilePath参数不能为空");
        }

        if (imageName == null || imageName.isEmpty()) {
            return ToolExecutionResult.error("imageName参数不能为空");
        }

        try {
            File dockerfile = new File(dockerfilePath);
            if (!dockerfile.exists()) {
                return ToolExecutionResult.error("Dockerfile不存在: " + dockerfilePath);
            }

            File baseDir = new File(buildContext);
            String imageId = dockerClient.buildImageCmd(baseDir)
                    .withDockerfile(dockerfile)
                    .withTags(java.util.Set.of(imageName + ":" + imageTag))
                    .exec(new BuildImageResultCallback())
                    .awaitImageId();

            log.info("Docker镜像构建成功: {}:{}, ID: {}", imageName, imageTag, imageId);
            return ToolExecutionResult.success("镜像构建成功", Map.of(
                    "imageName", imageName,
                    "imageTag", imageTag,
                    "imageId", imageId
            ));

        } catch (Exception e) {
            log.error("Docker镜像构建失败", e);
            return ToolExecutionResult.error("镜像构建失败: " + e.getMessage());
        }
    }

    /**
     * 运行Docker容器
     *
     * @param request 工具执行请求，包含imageName、containerName、ports、env、volumes、command、detach参数
     * @return 运行结果
     */
    @MCPTool(
            name = "docker_run",
            description = "运行Docker容器",
            category = "docker"
    )
    public ToolExecutionResult dockerRun(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化，请检查Docker是否运行");
        }

        Map<String, Object> params = request.getParameters();
        String imageName = (String) params.get("imageName");
        String containerName = (String) params.get("containerName");
        @SuppressWarnings("unchecked")
        List<String> ports = (List<String>) params.get("ports");
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) params.get("env");
        @SuppressWarnings("unchecked")
        List<String> volumes = (List<String>) params.get("volumes");
        String command = (String) params.get("command");
        boolean detach = params.containsKey("detach") ? (Boolean) params.get("detach") : true;

        if (imageName == null || imageName.isEmpty()) {
            return ToolExecutionResult.error("imageName参数不能为空");
        }

        try {
            // 检查镜像是否存在，不存在则拉取
            try {
                dockerClient.inspectImageCmd(imageName).exec();
            } catch (Exception e) {
                log.info("镜像不存在，开始拉取: {}", imageName);
                dockerClient.pullImageCmd(imageName)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(300, TimeUnit.SECONDS);
            }

            // 创建容器
            var createCmd = dockerClient.createContainerCmd(imageName);

            if (containerName != null && !containerName.isEmpty()) {
                createCmd.withName(containerName);
            }

            // 端口映射
            if (ports != null && !ports.isEmpty()) {
                Ports portBindings = new Ports();
                for (String portMapping : ports) {
                    String[] parts = portMapping.split(":");
                    if (parts.length == 2) {
                        int hostPort = Integer.parseInt(parts[0]);
                        int containerPort = Integer.parseInt(parts[1]);
                        portBindings.bind(ExposedPort.tcp(containerPort),
                                Ports.Binding.bindPort(hostPort));
                    }
                }
                createCmd.withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(portBindings));
            }

            // 环境变量
            if (env != null && !env.isEmpty()) {
                List<String> envList = env.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .toList();
                createCmd.withEnv(envList);
            }

            // 卷映射
            if (volumes != null && !volumes.isEmpty()) {
                List<Bind> binds = volumes.stream()
                        .map(v -> {
                            String[] parts = v.split(":");
                            if (parts.length >= 2) {
                                return new Bind(parts[0], new Volume(parts[1]));
                            }
                            return null;
                        })
                        .filter(b -> b != null)
                        .toList();
                createCmd.withHostConfig(HostConfig.newHostConfig()
                        .withBinds(binds));
            }

            // 命令
            if (command != null && !command.isEmpty()) {
                createCmd.withCmd(command.split(" "));
            }

            CreateContainerResponse container = createCmd.exec();
            String containerId = container.getId();

            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            log.info("Docker容器启动成功: {}, ID: {}", containerName, containerId);
            return ToolExecutionResult.success("容器启动成功", Map.of(
                    "containerId", containerId,
                    "containerName", containerName,
                    "imageName", imageName
            ));

        } catch (Exception e) {
            log.error("Docker容器启动失败", e);
            return ToolExecutionResult.error("容器启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止容器
     *
     * @param request 工具执行请求，包含containerId参数
     * @return 停止结果
     */
    @MCPTool(
            name = "docker_stop",
            description = "停止运行中的Docker容器",
            category = "docker"
    )
    public ToolExecutionResult dockerStop(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化");
        }

        Map<String, Object> params = request.getParameters();
        String containerId = (String) params.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            return ToolExecutionResult.error("containerId参数不能为空");
        }

        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("Docker容器停止成功: {}", containerId);
            return ToolExecutionResult.success("容器停止成功", Map.of(
                    "containerId", containerId
            ));

        } catch (Exception e) {
            log.error("Docker容器停止失败: {}", containerId, e);
            return ToolExecutionResult.error("容器停止失败: " + e.getMessage());
        }
    }

    /**
     * 删除容器
     *
     * @param request 工具执行请求，包含containerId和force参数
     * @return 删除结果
     */
    @MCPTool(
            name = "docker_remove_container",
            description = "删除Docker容器",
            category = "docker"
    )
    public ToolExecutionResult dockerRemoveContainer(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化");
        }

        Map<String, Object> params = request.getParameters();
        String containerId = (String) params.get("containerId");
        boolean force = params.containsKey("force") ? (Boolean) params.get("force") : false;

        if (containerId == null || containerId.isEmpty()) {
            return ToolExecutionResult.error("containerId参数不能为空");
        }

        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(force)
                    .exec();
            log.info("Docker容器删除成功: {}", containerId);
            return ToolExecutionResult.success("容器删除成功", Map.of(
                    "containerId", containerId
            ));

        } catch (Exception e) {
            log.error("Docker容器删除失败: {}", containerId, e);
            return ToolExecutionResult.error("容器删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取容器列表
     *
     * @param request 工具执行请求，包含showAll参数
     * @return 容器列表
     */
    @MCPTool(
            name = "docker_ps",
            description = "获取Docker容器列表",
            category = "docker"
    )
    public ToolExecutionResult dockerPs(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化");
        }

        Map<String, Object> params = request.getParameters();
        boolean showAll = params.containsKey("showAll") ? (Boolean) params.get("showAll") : false;

        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(showAll)
                    .exec();

            List<Map<String, Object>> containerList = containers.stream()
                    .map(c -> Map.<String, Object>of(
                            "id", c.getId(),
                            "names", c.getNames(),
                            "image", c.getImage(),
                            "status", c.getStatus(),
                            "state", c.getState(),
                            "ports", c.getPorts()
                    ))
                    .toList();

            return ToolExecutionResult.success("容器列表获取成功", Map.of(
                    "containers", containerList,
                    "count", containerList.size()
            ));

        } catch (Exception e) {
            log.error("获取容器列表失败", e);
            return ToolExecutionResult.error("获取容器列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取容器日志
     *
     * @param request 工具执行请求，包含containerId和tail参数
     * @return 日志内容
     */
    @MCPTool(
            name = "docker_logs",
            description = "获取Docker容器日志",
            category = "docker"
    )
    public ToolExecutionResult dockerLogs(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化");
        }

        Map<String, Object> params = request.getParameters();
        String containerId = (String) params.get("containerId");
        int tail = params.containsKey("tail") ? (Integer) params.get("tail") : 100;

        if (containerId == null || containerId.isEmpty()) {
            return ToolExecutionResult.error("containerId参数不能为空");
        }

        try {
            StringBuilder logs = new StringBuilder();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tail)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame frame) {
                            logs.append(new String(frame.getPayload()));
                        }
                    })
                    .awaitCompletion(5, TimeUnit.SECONDS);

            return ToolExecutionResult.success("日志获取成功", Map.of(
                    "containerId", containerId,
                    "logs", logs.toString()
            ));

        } catch (Exception e) {
            log.error("获取容器日志失败: {}", containerId, e);
            return ToolExecutionResult.error("获取日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取镜像列表
     *
     * @return 镜像列表
     */
    @MCPTool(
            name = "docker_images",
            description = "获取Docker镜像列表",
            category = "docker"
    )
    public ToolExecutionResult dockerImages(ToolExecutionRequest request) {
        if (dockerClient == null) {
            return ToolExecutionResult.error("Docker客户端未初始化");
        }

        try {
            List<Image> images = dockerClient.listImagesCmd().exec();

            List<Map<String, Object>> imageList = images.stream()
                    .map(i -> Map.<String, Object>of(
                            "id", i.getId(),
                            "repoTags", i.getRepoTags(),
                            "size", i.getSize(),
                            "created", i.getCreated()
                    ))
                    .toList();

            return ToolExecutionResult.success("镜像列表获取成功", Map.of(
                    "images", imageList,
                    "count", imageList.size()
            ));

        } catch (Exception e) {
            log.error("获取镜像列表失败", e);
            return ToolExecutionResult.error("获取镜像列表失败: " + e.getMessage());
        }
    }
}
