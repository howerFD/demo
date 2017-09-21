package com.example.demo.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DownFileUtils {

    /**
     * ヘッダを設定する。
     * 
     * @param req
     *        要求
     * @param res
     *        応答
     * @param fileName
     *        ファイル名
     * @throws UnsupportedEncodingException
     *         If the named encoding is not supported
     */
    private static void setHeader(HttpServletRequest req, HttpServletResponse res, String fileName) throws UnsupportedEncodingException {
        res.setHeader("content-type", "application/octet-stream");
        res.setContentType("application/octet-stream; charset=UTF-8");
        fileName = URLEncoder.encode(fileName, "UTF-8");
        fileName = fileName.replace("+", "%20");

        if (req.getHeader("User-Agent").indexOf("MSIE") == -1) {
            // Firefox, Opera, Chrome
            fileName = "filename*=UTF-8''" + fileName;
        } else {
            // IE7, 8, 9
            fileName = "filename=\"" + fileName + "\"";
        }

        res.setHeader("Content-Disposition", "attachment;" + fileName);
    }

    /**
     * ファイルをダウンロード。
     * 
     * @param req
     *        要求
     * @param res
     *        応答
     * @param fileContent
     *        ファイルの内容
     * @param fileName
     *        ファイル名
     */
    public static void writeResponse(HttpServletRequest req, HttpServletResponse res, byte[] fileContent, String fileName) {
        OutputStream os = null;
        try {
            // ヘッダを設定する。
            setHeader(req, res, fileName);
            os = res.getOutputStream();
            os.write(fileContent, 0, fileContent.length);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ファイルをダウンロード。
     * 
     * @param req
     *        要求
     * @param res
     *        応答
     * @param filePath
     *        ファイルのパス
     * @param fileName
     *        ファイル名
     */
    public static void writeResponse(HttpServletRequest req, HttpServletResponse res, String filePath, String fileName) {
        byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            // ヘッダを設定する。
            setHeader(req, res, fileName);
            os = res.getOutputStream();
            bis = new BufferedInputStream(new FileInputStream(new File(filePath)));
            int i = bis.read(buff);
            while (i != -1) {
                os.write(buff, 0, buff.length);
                os.flush();
                i = bis.read(buff);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
