package com.project.pdf.generator.reportpdfgenerator.thymeleaf;

import com.project.pdf.generator.reportpdfgenerator.dto.ReportPdfDto;

import java.util.Collections;

public class ThymeleafMain {
    public static void main(String[] args) throws Exception {
        ReportPdfDto reportPdfDto = new ReportPdfDto();
        reportPdfDto.setCsvFileTitle("Account-Details-Enhanced");
        reportPdfDto.setStartDate("2023-02-01 00:00 UTC");
        reportPdfDto.setEndDate("2023-03-01 12:00 UTC");
        reportPdfDto.setFileNames(Collections.singletonList("test1.csv"));
        reportPdfDto.setRecordCount(150);
        reportPdfDto.setRecordLimitPerPage(20);
        reportPdfDto.setColumnLimitPerPage(5);
        reportPdfDto.setFileDelimiter(",");

        TemplateService templateService = new TemplateService();
        templateService.parseCsvAndBuildPdf(reportPdfDto);
    }
}
