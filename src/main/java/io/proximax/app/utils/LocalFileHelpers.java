package io.proximax.app.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.ShareFile;
import io.proximax.app.db.LocalFile;
import io.proximax.download.DownloadParameter;
import io.proximax.upload.ByteArrayParameterData;
import io.proximax.upload.FileParameterData;
import io.proximax.upload.UploadParameter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.activation.MimetypesFileTypeMap;

/**
 *
 * @author Administrator
 */
public class LocalFileHelpers {

    public static boolean isExisted(LocalAccount localAccount, File file, int shareType) {
        try {
            return DBHelpers.isFileExisted(localAccount.fullName, localAccount.network, file.getName(), file.lastModified(), shareType);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void addFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            DBHelpers.addFile(localAccount.fullName, localAccount.network, localFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static List<LocalFile> getFiles(LocalAccount localAccount, String category) {
        try {
            DBHelpers.updateFileStatusTimeout(localAccount.fullName, localAccount.network, System.currentTimeMillis() - 3 * 60 * 1000); //timeout 3p
            return DBHelpers.getFiles(localAccount.fullName, localAccount.network, category);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static DownloadParameter createDownloadParameter(LocalFile localFile) {
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return DownloadParameter.create(localFile.nemHash)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return DownloadParameter.create(localFile.nemHash)
                        .withPasswordPrivacy(localFile.password)
                        .build();
            default:
                return DownloadParameter.create(localFile.nemHash)
                        .build();
        }
    }

    public static void shareLocalFile(LocalAccount localAccount, ShareFile shareFile) {
        try {
            DBHelpers.shareLocalFile(localAccount.fullName, localAccount.network, shareFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static String getContentType(String fileName) {
        String mimeType = null;
        if (fileName.endsWith(".html")) {
            mimeType = "text/html";
        } else if (fileName.endsWith(".css")) {
            mimeType = "text/css";
        } else if (fileName.endsWith(".js")) {
            mimeType = "application/javascript";
        } else if (fileName.endsWith(".gif")) {
            mimeType = "image/gif";
        } else if (fileName.endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".log")) {
            mimeType = "text/plain";
        } else if (fileName.endsWith(".xml")) {
            mimeType = "application/xml";
        } else if (fileName.endsWith(".json")) {
            mimeType = "application/json";
        } else {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            mimeType = mimeTypesMap.getContentType(fileName);
        }
        return mimeType;
    }

    public static String getContentType(File file) {
        return getContentType(file.getPath());
    }

    public static UploadParameter createUploadFileParameter(LocalAccount localAccount, LocalFile localFile, File uploadFile) throws IOException {
        Map<String, String> metaData = null;
        if (StringUtils.isEmpty(localFile.metadata)) {
            metaData = createMetaData(localAccount, localFile);
            localFile.metadata = metaData.toString();
        } else {
            try {
                Gson gson = new Gson();
                metaData = gson.fromJson(localFile.metadata, new TypeToken<Map<String, String>>() {
                }.getType());
            } catch (Exception ex) {
                metaData = createMetaData(localAccount, localFile);
                localFile.metadata = metaData.toString();
            }
        }
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .withPasswordPrivacy(localFile.password)
                        .build();
            default:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .build();
        }
    }

    public static void updateFile(LocalAccount localAccount, LocalFile oldFile, LocalFile newFile) {
        try {
            DBHelpers.updateFile(localAccount.fullName, localAccount.network, oldFile, newFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static List<LocalFile> getDelFiles(String fullName, String network) {
        try {
            return DBHelpers.getDelFiles(fullName, network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> createMetaData(LocalAccount localAccount, LocalFile localFile) {
        Map<String, String> metaData = new HashMap<String, String>();
        metaData.put("file", localFile.fileName);
        metaData.put("app", CONST.APP_FOLDER);
        metaData.put("user", localAccount.fullName);
        metaData.put("network", localAccount.network);
        metaData.put("utype", "" + localFile.uType);
        return metaData;
    }

    public static void updateFileFromTXN(String fullName, String network, String nemHash, int status) {
        try {
            DBHelpers.updateFile(fullName, network, nemHash, status);
        } catch (SQLException ex) {
        }
    }

    public static UploadParameter createUploadBinaryParameter(LocalAccount localAccount, LocalFile localFile, byte[] data) {
        Map<String, String> metaData = null;
        if (StringUtils.isEmpty(localFile.metadata)) {
            metaData = createMetaData(localAccount, localFile);
            localFile.metadata = metaData.toString();
        } else {
            try {
                Gson gson = new Gson();
                metaData = gson.fromJson(localFile.metadata, new TypeToken<Map<String, String>>() {
                }.getType());
            } catch (Exception ex) {
                metaData = createMetaData(localAccount, localFile);
                localFile.metadata = metaData.toString();
            }
        }
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .withPasswordPrivacy(localFile.password)
                        .build();
            default:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .build();
        }
    }

    public static File getSourceFile(LocalAccount localAccount, LocalFile localFile) {
        File file = new File(localFile.filePath);
        if (file.exists()) {
            if (file.length() == localFile.fileSize && file.lastModified() == localFile.modified) {
                return file;
            }
        }
        file = new File(getCacheDir(localAccount) + File.separator + localFile.nemHash + ".rtfx");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static File createFileCache(LocalAccount localAccount, LocalFile localFile) {
        return new File(getCacheDir(localAccount) + File.separator + localFile.nemHash + ".rtfx");
    }

    public static String getCacheDir(LocalAccount localAccount) {
        String filePath = System.getProperty("user.home") + File.separator + CONST.APP_FOLDER + File.separator + ".cache" + File.separator + localAccount.network + File.separator + localAccount.fullName;
        new File(filePath).mkdirs();
        return filePath;
    }

    public static String getTempFilePath(LocalAccount localAccount) {
        return getCacheDir(localAccount) + File.separator + UUID.randomUUID().toString().replace("-", "") + ".rtfx";
    }

    public static void uploadFileFailed(LocalAccount localAccount, LocalFile localFile) {
        try {
            localFile.status = CONST.FILE_STATUS_FAILED;
            DBHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, localFile.status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void addFileQueue(LocalAccount localAccount, LocalFile localFile) {
        try {
            localFile.status = CONST.FILE_STATUS_QUEUE;
            DBHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, localFile.status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void uploadingFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            localFile.status = CONST.FILE_STATUS_UPLOAD;
            DBHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, localFile.status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateLocalFileStatus1(String userName, String network, int fileId, int status) {
        try {
            DBHelpers.updateLocalFileStatus(userName, network, fileId, status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateLocalFile(LocalAccount localAccount, int fileId, String hash, String nemHash, String metaData, long uploadDate, int status) {
        try {
            DBHelpers.updateLocalFile(localAccount.fullName, localAccount.network, fileId, hash, nemHash, metaData, uploadDate, status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void deleteFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            DBHelpers.delFile(localAccount.fullName, localAccount.network, localFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static LocalFile getJob(LocalAccount localAccount) {
        try {
            return DBHelpers.getJob(localAccount.fullName, localAccount.network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<LocalFile> getFolders(String fullName, String network) {
        try {
            return DBHelpers.getFolders(fullName, network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<String> getListFolder(String fullName, String network) {
        try {
            return DBHelpers.getListFolder(fullName, network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void addFolder(String fullName, String network, String folder) {
        try {
            DBHelpers.addFolder(fullName, network, folder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void deleteFolder(String fullName, String network, String folder) {
        try {
            DBHelpers.delFolder(fullName, network, folder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateFolder(String fullName, String network, int folderId, String folder, String parent) {
        try {
            DBHelpers.updateFolder(fullName, network, folderId, folder, parent);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void moveFileFolder(LocalAccount localAccount, int id, String sFolder) {
        try {
            DBHelpers.moveFileFolder(localAccount.fullName, localAccount.network, id, sFolder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean delFolder(LocalAccount localAccount, String sFolder) {
        try {
            DBHelpers.delFolder(localAccount.fullName, localAccount.network, sFolder);
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void moveFilesNewFolder(LocalAccount localAccount, String oFolder, String nFolder) {
        try {
            DBHelpers.moveFilesNewFolder(localAccount.fullName, localAccount.network, oFolder, nFolder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
