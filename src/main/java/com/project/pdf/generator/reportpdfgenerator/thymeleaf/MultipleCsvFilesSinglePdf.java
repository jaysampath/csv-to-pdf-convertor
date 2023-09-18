package com.project.pdf.generator.reportpdfgenerator.thymeleaf;

import com.project.pdf.generator.reportpdfgenerator.dto.ReportPdfDto;

import java.util.Arrays;

public class MultipleCsvFilesSinglePdf {
    public static void main(String[] args) throws Exception {
       TemplateService templateService = new TemplateService();
        ReportPdfDto reportPdfDto = new ReportPdfDto();
        reportPdfDto.setCsvFileTitle("The Test CSV File)");
        reportPdfDto.setStartDate("Feb 1, 2023");
        reportPdfDto.setEndDate("Mar 1, 2023");
        reportPdfDto.setFileNames(Arrays.asList("test1.csv", "test_5col.csv"));
        reportPdfDto.setFileDelimiter(",");

        templateService.parseMultipleCsvAndGenerateSinglePdf(reportPdfDto);
    }
}
