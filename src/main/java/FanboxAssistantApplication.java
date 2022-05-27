import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.emoji.EmojiUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 主方法类
 *
 * @author KeithFeng
 * @date 2022/05/26 09:28
 */
public class FanboxAssistantApplication {

    private static List<String> blacklist = new ArrayList<>();
    private static List<String> whitelist = new ArrayList<>();

    private static final String TRASH_BIN = "★trash_bin★";
    private static final String ZIP_DIR = "extract_zip";
    private static final String MOVIE_DIR = "extract_movie";
    private static final String PSD_DIR = "extract_psd";
    private static final String HELP_KEY = "help";

    private static String masterPath = null;
    private static String trashBinPath = null;


    public static void main(String[] args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("【错误】请至少携带一个参数，作为整理的主文件夹，\n" +
                    "且该文件夹内的全部目录或文件应按照画师名称已分类好，例：\n\n" +
                    "java -jar assistant.jar D:\\Downloads\\月供Fanbox\n\n" +
                    "更多参数设置请执行：\n\n" +
                    "java -jar assistant.jar help");
            return;
        }
        if (HELP_KEY.equals(args[0])) {
            System.out.println("【帮助】执行本脚本时最多可携带3个参数，且第一个参数为必填项，例：\n\n" +
                    "java -jar assistant.jar D:\\Downloads\\月供Fanbox whitelist/off blacklist/off\n" +
                    "                                  ↑                   ↑             ↑\n" +
                    "                               主文件夹          启用/禁用白名单  启用/禁用黑名单\n\n" +
                    "主文件夹：它表示了即将整理的主文件夹绝对路径，且该文件夹内的全部目录或文件应按照画师名称已分类好\n" +
                    "白名单：你可以在本脚本同目录下的 whitelist.txt 文件内添加不需要帮助整理的画师文件夹名称，\n" +
                    "      以换行为分割；参数填写 whitelist 表示本次启用白名单，反之请填写 off\n" +
                    "黑名单：你可以在本脚本同目录下的 blacklist.txt 文件内添加直接清理掉的画师文件夹名称，\n" +
                    "      同样以换行为分割；参数填写 blacklist 表示本次启用黑名单，反之请填写 off\n\n" +
                    "本脚本会将所有fanbox图片整理到画师文件夹的根目录下，保留其所在文件夹的名称作为前缀，\n" +
                    "同时会将压缩包、视频和PSD文件整理在单独的文件夹下，并清理所有无料（$0）文件夹和累赘的目录结构；\n\n" +
                    "本流程不可逆，且尚未考虑通用适配，建议初次使用时先用少量画师进行测试，确保效果满意后再进行大规模整理；\n\n" +
                    "最后需要注意的一点是本脚本的清理只是将疑似无用文件移动到主文件夹下的 ★trash_bin★ 目录，\n" +
                    "请自行判断其中文件是否具有价值，若无用可自行物理删除。\n\n");
            return;
        }
        if (args.length == 3) {
            switch (args[1]) {
                case "whitelist":
                    File whitelistFile = new File("./whitelist.txt");
                    if (!whitelistFile.exists()) {
                        log("error", "【错误】白名单文件不存在，请先在脚本同目录下新建 whitelist.txt 文件！\n\n");
                        return;
                    }
                    FileUtil.readLines(whitelistFile, StandardCharsets.UTF_8, whitelist);
                    log("info", "已装载白名单：" + String.join("、", whitelist));
                    break;
                case "off":
                    log("info", "白名单已禁用");
                    break;
                default:
                    log("error", "【错误】第二个参数有误，允许的参数值为 whitelist 或 off！");
                    return;
            }
            switch (args[2]) {
                case "blacklist":
                    File blacklistFile = new File("./blacklist.txt");
                    if (!blacklistFile.exists()) {
                        log("error", "【错误】黑名单文件不存在，请先在脚本同目录下新建 blacklist.txt 文件！\n\n");
                        return;
                    }
                    FileUtil.readLines(blacklistFile, StandardCharsets.UTF_8, blacklist);
                    log("info", "已装载黑名单：" + String.join("、", blacklist));
                    break;
                case "off":
                    log("info", "黑名单已禁用");
                    break;
                default:
                    log("error", "【错误】第三个参数有误，允许的参数值为 blacklist 或 off！");
                    return;
            }
        }
        masterPath(args[0]);
    }

    private static void masterPath(String parentPath) {
        // 获取工作目录
        if (StrUtil.isEmpty(parentPath)) {
            parentPath = FileUtil.getAbsolutePath(".");
        }
        masterPath = parentPath;
        // 获取主目录下所有文件
        File[] allFileAndDir = FileUtil.ls(parentPath);

        for (File dir : allFileAndDir) {
            if (!dir.isDirectory()) {
                continue;
            }
            String dirName = dir.getName();
            if (TRASH_BIN.equals(dirName)) {
                continue;
            }
            if (whitelist.contains(dirName)) {
                log("warn", "跳过一个白名单画师：{}", dirName);
                continue;
            }
            if (blacklist.contains(dirName)) {
                delete(dir);
                log("warn", "清除一个黑名单画师：{}", dirName);
                continue;
            }
            cleanOtherFiles(dir);
            // 创建一个Zip专用文件夹
            File zipDirectory = new File(dir.getAbsolutePath() + File.separator + ZIP_DIR);
            extractZipFiles(zipDirectory, dir);
            // 创建一个视频专用文件夹
            File movieDirectory = new File(dir.getAbsolutePath() + File.separator + MOVIE_DIR);
            extractMovieFiles(movieDirectory, dir);
            // 创建一个PSD专用文件夹
            File psdDirectory = new File(dir.getAbsolutePath() + File.separator + PSD_DIR);
            extractPsdFiles(psdDirectory, dir);
            // 将剩余文件提取到画师文件夹下
            File[] files = FileUtil.ls(dir.getAbsolutePath());
            int count = 0;
            for (File file : files) {
                // 普通文件不作处理
                if (!file.isDirectory()) {
                    continue;
                }
                // 跳过生成的提取文件夹
                if (StrUtil.equalsAnyIgnoreCase(file.getName(), ZIP_DIR, MOVIE_DIR, PSD_DIR)) {
                    continue;
                }
                // 将图片文件汇总到画师文件夹下
                count += moveImageFiles(file);
                // 清除剩余的无用目录
                delete(file);
            }
            log("info", "已整理完成画师[{}]的文件，共{}个！", dir.getName(), String.valueOf(count));
        }
    }

    /**
     * 清理无料、封面图和网页文件
     *
     * @param dir
     */
    private static void cleanOtherFiles(File dir) {
        // 目录不存在或非目录时跳过
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 获取目录下文件及文件夹
        File[] allFileAndDir = FileUtil.ls(dir.getAbsolutePath());
        for (File file : allFileAndDir) {
            // 如果是目录，检查是否是无料
            if (file.isDirectory()) {
                String dirName = file.getName();
                if (StrUtil.contains(dirName, "$0_")) {
                    delete(file);
                    log("warn", "清理了一个无料文件夹：{}", dirName);
                }
                cleanOtherFiles(file);
                continue;
            }
            String fileName = file.getName();
            if (StrUtil.startWith(fileName, "cover", true)) {
                delete(file);
                log("warn", "清理了一个疑似封面图：{}", file.getPath());
            }
            if (StrUtil.endWith(fileName, ".html", true)) {
                delete(file);
                log("warn", "清理了一个网页文件：{}", file.getPath());
            }
        }
    }

    /**
     * 提取Zip文件到一个文件夹
     *
     * @param zipDir 当前画师的zip文件夹
     * @param dir    当前文件目录
     */
    private static void extractZipFiles(File zipDir, File dir) {
        // 目录不存在或非目录时跳过
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 跳过生成的提取文件夹
        if (StrUtil.equalsAnyIgnoreCase(dir.getName(), ZIP_DIR, MOVIE_DIR, PSD_DIR)) {
            return;
        }
        // 获取目录下文件及文件夹
        File[] allFileAndDir = FileUtil.ls(dir.getAbsolutePath());
        for (File file : allFileAndDir) {
            // 文件夹递归操作
            if (file.isDirectory()) {
                extractZipFiles(zipDir, file);
                continue;
            }
            // 文件检查是不是zip文件
            String extName = FileUtil.extName(file);
            if (StrUtil.equalsAnyIgnoreCase(extName, "zip", "rar", "7z")) {
                if (!zipDir.exists()) {
                    FileUtil.mkdir(zipDir);
                }
                String fileName = extract(file, zipDir);
                log("info", "归档了一个压缩文件：{}", fileName);
            }
        }
    }

    /**
     * 提取视频文件到一个文件夹
     *
     * @param movieDir 当前画师的视频文件夹
     * @param dir      当前文件目录
     */
    private static void extractMovieFiles(File movieDir, File dir) {
        // 目录不存在或非目录时跳过
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 跳过生成的提取文件夹
        if (StrUtil.equalsAnyIgnoreCase(dir.getName(), ZIP_DIR, MOVIE_DIR, PSD_DIR)) {
            return;
        }
        // 获取目录下文件及文件夹
        File[] allFileAndDir = FileUtil.ls(dir.getAbsolutePath());
        for (File file : allFileAndDir) {
            // 文件夹递归操作
            if (file.isDirectory()) {
                extractMovieFiles(movieDir, file);
                continue;
            }
            // 文件检查是不是视频文件
            String extName = FileUtil.extName(file);
            if (StrUtil.equalsAnyIgnoreCase(extName, "mp4", "gif", "flv", "mov")) {
                if (!movieDir.exists()) {
                    FileUtil.mkdir(movieDir);
                }
                String fileName = extract(file, movieDir);
                log("info", "归档了一个视频文件：{}", fileName);
            }
        }
    }

    /**
     * 提取PSD文件到一个文件夹
     *
     * @param psdDir 当前画师的PSD文件夹
     * @param dir    当前文件目录
     */
    private static void extractPsdFiles(File psdDir, File dir) {
        // 目录不存在或非目录时跳过
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 跳过生成的提取文件夹
        if (StrUtil.equalsAnyIgnoreCase(dir.getName(), ZIP_DIR, MOVIE_DIR, PSD_DIR)) {
            return;
        }
        // 获取目录下文件及文件夹
        File[] allFileAndDir = FileUtil.ls(dir.getAbsolutePath());
        for (File file : allFileAndDir) {
            // 文件夹递归操作
            if (file.isDirectory()) {
                extractPsdFiles(psdDir, file);
                continue;
            }
            // 文件检查是不是视频文件
            String extName = FileUtil.extName(file);
            if (StrUtil.equalsAnyIgnoreCase(extName, "psd")) {
                if (!psdDir.exists()) {
                    FileUtil.mkdir(psdDir);
                }
                String fileName = extract(file, psdDir);
                log("info", "归档了一个PSD文件：{}", fileName);
            }
        }
    }

    private static int moveImageFiles(File dir) {
        int count = 0;
        // 画师文件夹
        File parentPath = FileUtil.getParent(dir, 1);
        // 读取文件夹下全部文件（不含文件夹）
        List<File> allFiles = FileUtil.loopFiles(dir);
        for (File file : allFiles) {
            String parentName = FileUtil.getParent(file, 1).getName();
            String newName = removeEmoji(parentName + "_" + file.getName());
            File newPath = new File(parentPath.getAbsolutePath() + File.separator + newName);
            FileUtil.move(file, newPath, true);
            count++;
        }
        return count;
    }

    private static void delete(File file) {
        // 新建临时回收站
        File trashBin = new File(masterPath + File.separator + TRASH_BIN);
        if (StrUtil.isEmpty(trashBinPath)) {
            trashBinPath = trashBin.getAbsolutePath();
        }
        if (!trashBin.exists()) {
            FileUtil.mkdir(trashBinPath);
        }
        // 获取文件原路径
        String originalPath = file.getAbsolutePath();
        String path = StrUtil.removePrefix(originalPath, masterPath);
        // 如果操作的是文件夹，直接创建对应目录
        if (file.isDirectory()) {
            String absolutePath = trashBinPath + File.separator + path;
            File trashPath = FileUtil.mkdir(absolutePath);
            FileUtil.move(file, FileUtil.getParent(trashPath, 1), true);
            return;
        }
        // 如果操作的是文件，先创建对应层级，再移动文件
        String fileName = file.getName();
        String absolutePath = trashBinPath + path;
        File trashParentPath = FileUtil.mkdir(FileUtil.getParent(absolutePath, 1));
        File trashFile = new File(trashParentPath + File.separator + fileName);
        FileUtil.move(file, trashFile, true);
    }

    /**
     * 提取文件
     *
     * @param originalFile
     * @param extractPath
     * @return
     */
    private static String extract(File originalFile, File extractPath) {
        String parentName = FileUtil.getParent(originalFile, 1).getName();
        String newName = removeEmoji(parentName + "_" + originalFile.getName());
        String targetPath = extractPath.getAbsolutePath() + File.separator + newName;
        FileUtil.move(originalFile, new File(targetPath), true);
        return newName;
    }

    private static String removeEmoji(String text) {
        return EmojiUtil.removeAllEmojis(text);
    }

    private static void log(String type, String message) {
        String print = DateUtil.now() + "\t" + message;
        switch (type) {
            case "info":
                System.out.println(print);
                break;
            case "error":
            case "warn":
                System.err.println(print);
                break;
            default:
                break;
        }
    }

    private static void log(String type, String message, String... args) {
        String print = DateUtil.now() + "\t" + StrUtil.format(message, args);
        switch (type) {
            case "info":
                System.out.println(print);
                break;
            case "error":
            case "warn":
                System.err.println(print);
                break;
            default:
                break;
        }
    }
}
