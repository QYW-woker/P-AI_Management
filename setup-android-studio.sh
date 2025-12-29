#!/bin/bash
# ============================================================
# Android Studio 一键安装配置脚本
# 适用于: Ubuntu/Debian 系统
# 功能: 安装 Android Studio + 中文语言包 + SDK 配置
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
ANDROID_STUDIO_VERSION="2024.2.1.12"
INSTALL_DIR="/opt/android-studio"
ANDROID_SDK_ROOT="$HOME/Android/Sdk"
DOWNLOAD_DIR="/tmp"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查是否以 root 运行
check_permissions() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "请不要以 root 用户运行此脚本"
        print_info "脚本会在需要时自动请求 sudo 权限"
        exit 1
    fi
}

# 步骤1: 检查并安装依赖
install_dependencies() {
    print_info "正在检查和安装依赖..."

    # 必需的依赖包
    DEPS="libc6 libncurses5 libstdc++6 lib32z1 libbz2-1.0 libxrender1 libxtst6 libxi6 libfreetype6 libxft2 xdg-utils"

    sudo apt-get update -qq
    sudo apt-get install -y $DEPS unzip wget curl

    # 安装 32 位库（某些模拟器需要）
    sudo dpkg --add-architecture i386 2>/dev/null || true
    sudo apt-get update -qq
    sudo apt-get install -y libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386 2>/dev/null || true

    print_success "依赖安装完成"
}

# 步骤2: 检查 Android Studio 下载文件
check_download() {
    local tar_file="$DOWNLOAD_DIR/android-studio.tar.gz"

    if [ -f "$tar_file" ]; then
        print_success "找到下载文件: $tar_file"
        return 0
    fi

    # 检查其他可能的位置
    for location in "$HOME/Downloads" "$HOME" "/tmp"; do
        for pattern in "android-studio*.tar.gz" "android-studio*.zip"; do
            found=$(find "$location" -maxdepth 1 -name "$pattern" 2>/dev/null | head -1)
            if [ -n "$found" ]; then
                print_success "找到下载文件: $found"
                cp "$found" "$tar_file"
                return 0
            fi
        done
    done

    print_error "未找到 Android Studio 安装包!"
    echo ""
    echo "============================================================"
    echo "请手动下载 Android Studio 并放到 /tmp 目录:"
    echo ""
    echo "下载链接 (选择其一):"
    echo "  官方: https://developer.android.com/studio"
    echo "  直链: https://dl.google.com/dl/android/studio/ide-zips/${ANDROID_STUDIO_VERSION}/android-studio-${ANDROID_STUDIO_VERSION}-linux.tar.gz"
    echo ""
    echo "下载后执行:"
    echo "  mv ~/Downloads/android-studio*.tar.gz /tmp/android-studio.tar.gz"
    echo "  ./setup-android-studio.sh"
    echo "============================================================"
    exit 1
}

# 步骤3: 安装 Android Studio
install_android_studio() {
    print_info "正在安装 Android Studio..."

    local tar_file="$DOWNLOAD_DIR/android-studio.tar.gz"

    # 删除旧安装
    if [ -d "$INSTALL_DIR" ]; then
        print_warning "发现旧版本，正在移除..."
        sudo rm -rf "$INSTALL_DIR"
    fi

    # 解压到 /opt
    print_info "正在解压到 $INSTALL_DIR..."
    sudo tar -xzf "$tar_file" -C /opt/

    # 设置权限
    sudo chown -R $USER:$USER "$INSTALL_DIR"

    print_success "Android Studio 安装完成"
}

# 步骤4: 安装中文语言包
install_chinese_plugin() {
    print_info "正在配置中文语言包..."

    local plugins_dir="$HOME/.config/Google/AndroidStudio2024.2/plugins"
    mkdir -p "$plugins_dir"

    # 创建自动安装中文插件的配置
    local idea_properties="$INSTALL_DIR/bin/idea.properties"

    # 添加中文语言设置提示
    cat > "$HOME/.android-studio-first-run.txt" << 'FIRSTRUN'
============================================================
首次启动 Android Studio 后，请安装中文语言包:

方法1 (推荐): 通过插件市场安装
  1. 启动 Android Studio
  2. 点击 "Plugins" (插件)
  3. 搜索 "Chinese (Simplified)"
  4. 点击 "Install" 安装
  5. 重启 Android Studio

方法2: 手动下载安装
  1. 访问: https://plugins.jetbrains.com/plugin/13710-chinese-simplified-language-pack
  2. 下载对应版本的插件
  3. 在 Android Studio 中: File > Settings > Plugins > ⚙️ > Install from disk
============================================================
FIRSTRUN

    print_success "中文语言包配置已准备"
}

# 步骤5: 配置环境变量
setup_environment() {
    print_info "正在配置环境变量..."

    # 创建 SDK 目录
    mkdir -p "$ANDROID_SDK_ROOT"

    # 配置环境变量
    local profile_file="$HOME/.bashrc"
    local env_marker="# Android Studio Environment"

    # 检查是否已配置
    if grep -q "$env_marker" "$profile_file" 2>/dev/null; then
        print_warning "环境变量已配置，跳过"
    else
        cat >> "$profile_file" << EOF

$env_marker
export ANDROID_HOME="\$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="\$HOME/Android/Sdk"
export PATH="\$PATH:\$ANDROID_HOME/emulator"
export PATH="\$PATH:\$ANDROID_HOME/platform-tools"
export PATH="\$PATH:\$ANDROID_HOME/tools"
export PATH="\$PATH:\$ANDROID_HOME/tools/bin"
export PATH="\$PATH:/opt/android-studio/bin"
EOF
        print_success "环境变量已添加到 ~/.bashrc"
    fi

    # 立即生效
    export ANDROID_HOME="$HOME/Android/Sdk"
    export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
    export PATH="$PATH:$ANDROID_HOME/platform-tools:/opt/android-studio/bin"
}

# 步骤6: 创建桌面快捷方式
create_desktop_entry() {
    print_info "正在创建桌面快捷方式..."

    local desktop_file="$HOME/.local/share/applications/android-studio.desktop"
    mkdir -p "$(dirname "$desktop_file")"

    cat > "$desktop_file" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Comment=Android 集成开发环境
Exec=/opt/android-studio/bin/studio.sh %f
Icon=/opt/android-studio/bin/studio.svg
Categories=Development;IDE;
Terminal=false
StartupNotify=true
StartupWMClass=jetbrains-studio
EOF

    chmod +x "$desktop_file"

    # 也创建命令行快捷方式
    sudo ln -sf /opt/android-studio/bin/studio.sh /usr/local/bin/android-studio 2>/dev/null || true

    print_success "桌面快捷方式创建完成"
}

# 步骤7: 创建项目启动脚本
create_project_launcher() {
    print_info "正在创建项目启动脚本..."

    cat > "$PROJECT_DIR/open-in-android-studio.sh" << EOF
#!/bin/bash
# 在 Android Studio 中打开 AI_Management 项目

STUDIO_PATH="/opt/android-studio/bin/studio.sh"
PROJECT_PATH="$PROJECT_DIR"

if [ -f "\$STUDIO_PATH" ]; then
    echo "正在启动 Android Studio..."
    echo "项目路径: \$PROJECT_PATH"
    nohup "\$STUDIO_PATH" "\$PROJECT_PATH" > /dev/null 2>&1 &
    echo "Android Studio 已在后台启动"
else
    echo "错误: Android Studio 未安装"
    echo "请先运行: ./setup-android-studio.sh"
fi
EOF

    chmod +x "$PROJECT_DIR/open-in-android-studio.sh"
    print_success "项目启动脚本创建完成: open-in-android-studio.sh"
}

# 步骤8: 显示完成信息
show_completion() {
    echo ""
    echo "============================================================"
    echo -e "${GREEN}✓ Android Studio 安装配置完成!${NC}"
    echo "============================================================"
    echo ""
    echo "接下来的步骤:"
    echo ""
    echo "1. 重新加载环境变量:"
    echo "   source ~/.bashrc"
    echo ""
    echo "2. 启动 Android Studio 并打开项目:"
    echo "   ./open-in-android-studio.sh"
    echo "   或者: android-studio $PROJECT_DIR"
    echo ""
    echo "3. 首次启动时:"
    echo "   - 选择 'Do not import settings'"
    echo "   - 完成 SDK 下载向导"
    echo "   - 安装中文语言包 (Plugins > 搜索 'Chinese')"
    echo ""
    echo "4. 启用 Live Edit:"
    echo "   File > Settings > Editor > Live Edit"
    echo "   勾选 'Enable Live Edit'"
    echo ""
    echo "============================================================"
    cat "$HOME/.android-studio-first-run.txt" 2>/dev/null || true
}

# 主函数
main() {
    echo ""
    echo "============================================================"
    echo "  Android Studio 一键安装配置脚本"
    echo "  项目: AI_Management"
    echo "============================================================"
    echo ""

    check_permissions
    install_dependencies
    check_download
    install_android_studio
    install_chinese_plugin
    setup_environment
    create_desktop_entry
    create_project_launcher
    show_completion
}

# 运行主函数
main "$@"
