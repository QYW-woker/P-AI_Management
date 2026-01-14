// 项目设置配置文件
// AI智能生活管理APP - Settings Configuration

pluginManagement {
    repositories {
        // 官方仓库优先（确保 CI 环境兼容性）
        google()
        mavenCentral()
        gradlePluginPortal()
        // 阿里云镜像加速（本地开发备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 官方仓库优先（确保 CI 环境兼容性）
        google()
        mavenCentral()
        // 阿里云镜像加速（本地开发备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

// 项目根名称
rootProject.name = "AI_Management"

// 包含app模块
include(":app")
