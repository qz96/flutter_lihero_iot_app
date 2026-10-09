package com.lh.lhble.Utils;

import androidx.annotation.NonNull;

import com.lh.lhble.Entity.ExcelInfoBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ExcelUtils implements Serializable {

    private static final WritableCellFormat wcf;

    static {
        WritableFont wf = new WritableFont(WritableFont.createFont("宋体"), 12,
                WritableFont.NO_BOLD, false);
        wcf = new WritableCellFormat(wf);
        try {
            wcf.setAlignment(jxl.format.Alignment.CENTRE);// 左右居中
            wcf.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 上下居中
//            wcf.setBorder(Border.ALL, BorderLineStyle.THIN); // 设置边框
            wcf.setWrap(true); // 是否换行;
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }

    private static class SingleHolder implements Serializable {
        public final static ExcelUtils mInstance = new ExcelUtils();
    }

    public static ExcelUtils getInstance() {
        return SingleHolder.mInstance;
    }

    /**
     * 生成excel
     *
     * @param beans       一个对象，对应一个表格
     * @param fileAllPath 文件全路径--如/ddd/dd/
     */
    public boolean exportData(String fileAllPath, String fileName, ExcelInfoBean... beans) {
        if (beans == null || beans.length == 0) {
            return true;
        }
        //数据处理
        OutputStream os = null;
        WritableWorkbook wwb = null;
        try {
            File file = new File(fileAllPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            os = new FileOutputStream(file.getAbsoluteFile() + File.separator + fileName);
            wwb = Workbook.createWorkbook(os);
            for (int i = 0; i < beans.length; i++) {
                ExcelInfoBean cacheInfo = beans[i];
                //开始处理数据
                String sheetName = cacheInfo.getSheetTitle();
                //建立表
                WritableSheet sheet = wwb.createSheet(sheetName, i);
                List<HashMap<String, String>> contentList = cacheInfo.getData();
                if (contentList != null && !contentList.isEmpty()) {
                    //写入数据
                    //第一行为属性头
                    List<String> headInfo = new ArrayList<>();
                    if (cacheInfo.getKeys()!=null&&cacheInfo.getKeys().size()>0){
                        headInfo.addAll(cacheInfo.getKeys());
                    }else {
                        HashMap<String, String> headMap = contentList.get(0);
                        for (Map.Entry<String, String> mapCache : headMap.entrySet()) {
                            headInfo.add(mapCache.getKey());
                        }
                    }


                    int positionCounter = 0;
                    if (!headInfo.isEmpty()) {
                        for (int col = 0; col < headInfo.size(); col++) {
                            sheet.setColumnView(col,15);
                            sheet.addCell(new Label(col, positionCounter, headInfo.get(col), wcf));
                        }
                        //写入表头
                        positionCounter += 1;
                        sheet.setRowView(0,500);
                    }
                    //写入内容
                    for (int c = 0; c < contentList.size(); c++) {

                        sheet.setRowView(c+1,500);
                        HashMap<String, String> contentMap = contentList.get(c);
                        if (contentMap != null && !contentMap.isEmpty()) {
                            for (int col = 0; col < headInfo.size(); col++) {
                                String headInfoCache = headInfo.get(col);
                                //通过表头，获取数据
                                String contentValue = contentMap.get(headInfoCache);
                                //写入到对应的位置
                                sheet.addCell(new Label(col, positionCounter, contentValue, wcf));
                            }
                            positionCounter += 1;
                        }
                    }
                }
            }
            //至此写入
            wwb.write();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wwb != null) {
                try {
                    wwb.close();
                } catch (IOException | WriteException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 读取excel
     */
    public List<ExcelInfoBean> importData(@NonNull String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        //读取excel
        InputStream is = null;
        Workbook workbook = null;
        try {
            is = new FileInputStream(path);
            workbook = Workbook.getWorkbook(is);
            //读取excel有多少个表
            Sheet[] sheets = workbook.getSheets();
            if (sheets == null || sheets.length == 0) {
                return new ArrayList<>();
            }
            //单个单元格
            Cell cell;
            List<ExcelInfoBean> result = new ArrayList<>();
            List<String> headList = new ArrayList<>();
            for (int s = 0; s < sheets.length; s++) {
                Sheet sheetCache = sheets[s];
                String sheetName = sheetCache.getName();
                //获取所有单元格里面的数据
                ExcelInfoBean exportBean = new ExcelInfoBean(sheetName,new ArrayList<>(), new ArrayList<>());
                int cols = sheetCache.getColumns();
                int rows = sheetCache.getRows();
                //逐行获取
                List<HashMap<String, String>> contentList = new ArrayList<>();
                for (int r = 0; r < rows; r++) {
                    HashMap<String, String> contentHashMap = new HashMap<>();
                    for (int c = 0; c < cols; c++) {
                        cell = sheetCache.getCell(c, r);
                        String content = cell.getContents();
                        if (r == 0) {
                            //头部数据
                            headList.add(content);
                        } else {
                            //非头部数据
                            contentHashMap.put(headList.get(c), content);
                        }
                    }
                    if (r != 0) {
                        contentList.add(contentHashMap);
                    }
                }
                exportBean.setData(contentList);
                result.add(exportBean);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>();
    }

}

