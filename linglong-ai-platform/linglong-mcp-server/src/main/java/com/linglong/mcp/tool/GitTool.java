package com.linglong.mcp.tool;

import com.linglong.mcp.annotation.MCPParam;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Git操作工具
 * 支持Git常用操作：commit、push、pull、clone等
 */
@Component
public class GitTool {

    private static final Logger log = LoggerFactory.getLogger(GitTool.class);

    /**
     * 初始化Git仓库
     *
     * @param directory 仓库目录
     * @return 初始化结果
     */
    @MCPTool(
            name = "git_init",
            description = "在指定目录初始化Git仓库",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    public ToolExecutionResult gitInit(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        try {
            File dir = new File(directory);
            Git git = Git.init().setDirectory(dir).call();
            git.close();

            log.info("Git仓库初始化成功: {}", directory);
            return ToolExecutionResult.success("Git仓库初始化成功", Map.of(
                    "directory", directory
            ));

        } catch (GitAPIException e) {
            log.error("Git初始化失败: {}", directory, e);
            return ToolExecutionResult.error("Git初始化失败: " + e.getMessage());
        }
    }

    /**
     * 克隆远程仓库
     *
     * @param request 远程仓库URL
     * @param directory 本地目录
     * @return 克隆结果
     */
    @MCPTool(
            name = "git_clone",
            description = "克隆远程Git仓库到本地",
            category = "git"
    )
    @MCPParam(name = "remoteUrl", description = "远程仓库URL", type = "string", required = true)
    @MCPParam(name = "directory", description = "本地目标目录(必须为空或不存在)", type = "string", required = true)
    @MCPParam(name = "username", description = "认证用户名(私有仓库需要)", type = "string", required = false)
    @MCPParam(name = "password", description = "认证密码或Token(私有仓库需要)", type = "string", required = false)
    public ToolExecutionResult gitClone(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String remoteUrl = (String) params.get("remoteUrl");
        String directory = (String) params.get("directory");
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return ToolExecutionResult.error("remoteUrl参数不能为空");
        }

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        // 检查目标目录是否已存在
        File targetDir = new File(directory);
        if (targetDir.exists()) {
            if (targetDir.isFile()) {
                return ToolExecutionResult.error("目标路径是一个文件，请选择目录路径: " + directory);
            }
            String[] files = targetDir.list();
            if (files != null && files.length > 0) {
                return ToolExecutionResult.error("目标目录已存在且不为空，请选择一个新的目录或手动删除现有目录后再试: " + directory);
            }
        }

        try {
            Git git;
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(targetDir)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                        .call();
            } else {
                git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(targetDir)
                        .call();
            }
            git.close();

            log.info("Git克隆成功: {} -> {}", remoteUrl, directory);
            return ToolExecutionResult.success("Git克隆成功", Map.of(
                    "remoteUrl", remoteUrl,
                    "directory", directory
            ));

        } catch (GitAPIException e) {
            log.error("Git克隆失败: {}", remoteUrl, e);
            String errorMsg = e.getMessage();
            // 提供更友好的错误提示
            if (errorMsg.contains("Authentication")) {
                return ToolExecutionResult.error("认证失败，请检查用户名和密码是否正确，或确认是否有权限访问该仓库");
            } else if (errorMsg.contains("not found") || errorMsg.contains("404")) {
                return ToolExecutionResult.error("仓库不存在或URL错误，请检查仓库地址: " + remoteUrl);
            } else if (errorMsg.contains("Connection") || errorMsg.contains("timeout")) {
                return ToolExecutionResult.error("网络连接失败，请检查网络或仓库地址是否可访问");
            }
            return ToolExecutionResult.error("Git克隆失败: " + errorMsg);
        }
    }

    /**
     * 添加文件到暂存区
     *
     * @param directory 仓库目录
     * @param filePattern 文件模式(如".", "*.java")
     * @return 添加结果
     */
    @MCPTool(
            name = "git_add",
            description = "将文件添加到Git暂存区",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    @MCPParam(name = "filePattern", description = "文件模式(如. 或 *.java)", type = "string", required = false, defaultValue = ".")
    public ToolExecutionResult gitAdd(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");
        String filePattern = (String) params.getOrDefault("filePattern", ".");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));
            git.add().addFilepattern(filePattern).call();
            git.close();

            log.info("Git add成功: {} -> {}", directory, filePattern);
            return ToolExecutionResult.success("文件已添加到暂存区", Map.of(
                    "directory", directory,
                    "filePattern", filePattern
            ));

        } catch (Exception e) {
            log.error("Git add失败: {}", directory, e);
            return ToolExecutionResult.error("Git add失败: " + e.getMessage());
        }
    }

    /**
     * 提交更改
     *
     * @param directory 仓库目录
     * @param message   提交信息
     * @return 提交结果
     */
    @MCPTool(
            name = "git_commit",
            description = "提交Git更改",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    @MCPParam(name = "message", description = "提交信息", type = "string", required = true)
    public ToolExecutionResult gitCommit(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");
        String message = (String) params.get("message");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        if (message == null || message.isEmpty()) {
            return ToolExecutionResult.error("message参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));
            var commit = git.commit().setMessage(message).call();
            String commitId = commit.getName();
            git.close();

            log.info("Git commit成功: {} - {}", directory, message);
            return ToolExecutionResult.success("提交成功", Map.of(
                    "directory", directory,
                    "commitId", commitId,
                    "message", message
            ));

        } catch (Exception e) {
            log.error("Git commit失败: {}", directory, e);
            return ToolExecutionResult.error("Git commit失败: " + e.getMessage());
        }
    }

    /**
     * 推送到远程仓库
     *
     * @param directory 仓库目录
     * @param remote    远程名称(默认origin)
     * @param branch    分支名称(默认main)
     * @return 推送结果
     */
    @MCPTool(
            name = "git_push",
            description = "推送本地提交到远程仓库",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径(包含.git的目录)", type = "string", required = true)
    @MCPParam(name = "remote", description = "远程名称", type = "string", required = false, defaultValue = "origin")
    @MCPParam(name = "branch", description = "分支名称", type = "string", required = false, defaultValue = "main")
    @MCPParam(name = "username", description = "认证用户名", type = "string", required = false)
    @MCPParam(name = "password", description = "认证密码或Token", type = "string", required = false)
    public ToolExecutionResult gitPush(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");
        String remote = (String) params.getOrDefault("remote", "origin");
        String branch = (String) params.getOrDefault("branch", "main");
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        // 检查是否是有效的Git仓库
        File gitDir = new File(directory, ".git");
        if (!gitDir.exists()) {
            return ToolExecutionResult.error("该目录不是Git仓库（找不到.git目录），请检查路径: " + directory + "\n提示：Git仓库目录应该包含.git子目录");
        }

        try {
            Git git = Git.open(new File(directory));

            var pushCommand = git.push()
                    .setRemote(remote)
                    .add(branch);

            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            var results = pushCommand.call();
            git.close();

            log.info("Git push成功: {} -> {}/{}", directory, remote, branch);
            return ToolExecutionResult.success("推送成功", Map.of(
                    "directory", directory,
                    "remote", remote,
                    "branch", branch
            ));

        } catch (Exception e) {
            log.error("Git push失败: {}", directory, e);
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Authentication") || errorMsg.contains("not authorized")) {
                return ToolExecutionResult.error("认证失败，请填写正确的用户名和密码/Token");
            } else if (errorMsg.contains("rejected")) {
                return ToolExecutionResult.error("推送被拒绝，可能需要先拉取远程更新或检查分支权限");
            } else if (errorMsg.contains("Repository not found") || errorMsg.contains("404")) {
                return ToolExecutionResult.error("远程仓库不存在或无权限访问: " + remote);
            }
            return ToolExecutionResult.error("Git push失败: " + errorMsg);
        }
    }

    /**
     * 从远程仓库拉取
     *
     * @param directory 仓库目录
     * @param remote    远程名称(默认origin)
     * @param branch    分支名称(默认main)
     * @return 拉取结果
     */
    @MCPTool(
            name = "git_pull",
            description = "从远程仓库拉取最新代码",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    @MCPParam(name = "remote", description = "远程名称", type = "string", required = false, defaultValue = "origin")
    @MCPParam(name = "branch", description = "分支名称", type = "string", required = false, defaultValue = "main")
    @MCPParam(name = "username", description = "认证用户名", type = "string", required = false)
    @MCPParam(name = "password", description = "认证密码或Token", type = "string", required = false)
    public ToolExecutionResult gitPull(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");
        String remote = (String) params.getOrDefault("remote", "origin");
        String branch = (String) params.getOrDefault("branch", "main");
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));

            var pullCommand = git.pull()
                    .setRemote(remote)
                    .setRemoteBranchName(branch);

            if (username != null && password != null) {
                pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }

            var result = pullCommand.call();
            git.close();

            log.info("Git pull成功: {} <- {}/{}", directory, remote, branch);
            return ToolExecutionResult.success("拉取成功", Map.of(
                    "directory", directory,
                    "remote", remote,
                    "branch", branch,
                    "successful", result.isSuccessful()
            ));

        } catch (Exception e) {
            log.error("Git pull失败: {}", directory, e);
            return ToolExecutionResult.error("Git pull失败: " + e.getMessage());
        }
    }

    /**
     * 获取仓库状态
     *
     * @param directory 仓库目录
     * @return 状态信息
     */
    @MCPTool(
            name = "git_status",
            description = "获取Git仓库当前状态",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    public ToolExecutionResult gitStatus(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));
            Status status = git.status().call();
            git.close();

            Map<String, Object> statusMap = Map.of(
                    "added", status.getAdded(),
                    "changed", status.getChanged(),
                    "removed", status.getRemoved(),
                    "missing", status.getMissing(),
                    "modified", status.getModified(),
                    "untracked", status.getUntracked(),
                    "conflicting", status.getConflicting()
            );

            boolean isClean = status.isClean();
            return ToolExecutionResult.success(
                    isClean ? "工作区干净" : "工作区有未提交更改",
                    statusMap
            );

        } catch (Exception e) {
            log.error("Git status失败: {}", directory, e);
            return ToolExecutionResult.error("Git status失败: " + e.getMessage());
        }
    }

    /**
     * 创建并切换到新分支
     *
     * @param directory 仓库目录
     * @param branchName 分支名称
     * @return 创建结果
     */
    @MCPTool(
            name = "git_create_branch",
            description = "创建并切换到新分支",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    @MCPParam(name = "branchName", description = "新分支名称", type = "string", required = true)
    public ToolExecutionResult gitCreateBranch(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");
        String branchName = (String) params.get("branchName");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        if (branchName == null || branchName.isEmpty()) {
            return ToolExecutionResult.error("branchName参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));

            // 创建分支
            git.branchCreate().setName(branchName).call();

            // 切换到新分支
            git.checkout().setName(branchName).call();

            git.close();

            log.info("Git创建分支成功: {} - {}", directory, branchName);
            return ToolExecutionResult.success("分支创建成功", Map.of(
                    "directory", directory,
                    "branchName", branchName
            ));

        } catch (Exception e) {
            log.error("Git创建分支失败: {}", directory, e);
            return ToolExecutionResult.error("Git创建分支失败: " + e.getMessage());
        }
    }

    /**
     * 获取分支列表
     *
     * @param directory 仓库目录
     * @return 分支列表
     */
    @MCPTool(
            name = "git_branch_list",
            description = "获取Git仓库的所有分支",
            category = "git"
    )
    @MCPParam(name = "directory", description = "仓库目录路径", type = "string", required = true)
    public ToolExecutionResult gitBranchList(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directory = (String) params.get("directory");

        if (directory == null || directory.isEmpty()) {
            return ToolExecutionResult.error("directory参数不能为空");
        }

        try {
            Git git = Git.open(new File(directory));

            List<Ref> localBranches = git.branchList().call();
            List<Ref> remoteBranches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

            String currentBranch = git.getRepository().getBranch();

            git.close();

            List<String> local = localBranches.stream()
                    .map(ref -> ref.getName().replace("refs/heads/", ""))
                    .collect(Collectors.toList());

            List<String> remote = remoteBranches.stream()
                    .map(ref -> ref.getName().replace("refs/remotes/", ""))
                    .collect(Collectors.toList());

            return ToolExecutionResult.success("分支列表获取成功", Map.of(
                    "currentBranch", currentBranch,
                    "localBranches", local,
                    "remoteBranches", remote
            ));

        } catch (Exception e) {
            log.error("Git获取分支列表失败: {}", directory, e);
            return ToolExecutionResult.error("Git获取分支列表失败: " + e.getMessage());
        }
    }
}
