package cn.njust.campusexpress.common.util;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件处理工具类
 */
@Slf4j
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * 保存图片文件到指定目录
     *
     * @param dirPath 相对目录路径，例如 "upload/avatar"
     * @param file    图片文件
     * @return 图片的访问路径，例如 "/upload/avatar/xxx.png"
     */
    public static String saveImage(String dirPath, MultipartFile file) {
        //检查文件类型
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.FILE_EMPTY);
        }
        String suffix;
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        switch (contentType) {
            case "image/jpeg" -> suffix = ".jpg";
            case "image/png" -> suffix = ".png";
            case "image/webp" -> suffix = ".webp";
            default ->
                    throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        String filename;
        Path path;
        do {
            filename = UUID.randomUUID() + suffix;
            path = Paths.get(dirPath, filename);
        } while (Files.exists(path));
        try {
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            return "/" + dirPath + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 删除本地图片文件
     *
     * @param filePath 图片访问路径，例如 "/upload/avatar/xxx.png"
     */
    public static void deleteImage(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Path path = Paths.get("." + filePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除图片失败：{}", filePath, e);
        }
    }
}
