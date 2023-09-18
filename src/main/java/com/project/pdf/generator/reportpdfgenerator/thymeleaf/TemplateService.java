package com.project.pdf.generator.reportpdfgenerator.thymeleaf;

import com.lowagie.text.DocumentException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.project.pdf.generator.reportpdfgenerator.dto.PdfMetadata;
import com.project.pdf.generator.reportpdfgenerator.dto.ReportPdfDto;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    private static final String PATH_PREFIX = "csv-reports/";

    private static final TemplateEngine templateEngine = new TemplateEngine();

    public void generatePdfFromHtml(String html) throws IOException, DocumentException {
        String outputFile = "C:\\Users\\jayakolisetty\\projects\\poc\\report-pdf-generator\\src\\main\\resources\\thymeleaf\\generated\\generated.pdf";
        OutputStream outputStream = new FileOutputStream(outputFile);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);

        outputStream.close();
    }

    public String parseThymeleafTemplate(ReportPdfDto reportPdfDto) throws Exception {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(templateResolver);

        List<String[]> csvData = parseCsvFile(reportPdfDto);


        String outputFile = "C:\\Users\\jayakolisetty\\projects\\poc\\report-pdf-generator\\src\\main\\resources\\thymeleaf\\generated\\generated.pdf";
        OutputStream outputStream = new FileOutputStream(outputFile);

        ITextRenderer renderer = new ITextRenderer();

        Context context = new Context();
        context.setVariable("provider", "Sampath");
        context.setVariable("reportName", reportPdfDto.getCsvFileTitle());
        context.setVariable("startDate", reportPdfDto.getStartDate());
        context.setVariable("endDate", reportPdfDto.getEndDate());
        context.setVariable("csvDataHeaders", csvData.get(0));
        context.setVariable("csvDataRows", csvData.subList(1, csvData.size()));

        try {
            String embeddedImage = getConvertorLogoEncoded();
            context.setVariable("embeddedImage", "data:image/png;base64," + embeddedImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String html = templateEngine.process("thymeleaf/template/report-html-template", context);

        //first page
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream, false);

        for (int i = 0; i < 3; i++) {
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.writeNextDocument(1);
        }

        renderer.finishPDF();
        outputStream.close();


        return templateEngine.process("thymeleaf/template/report-html-template", context);
    }

    private List<String[]> parseCsvFile(ReportPdfDto pdfDto) {

        String fileName = pdfDto.getFileNames().get(0);

        //read file
        FileReader filereader = null;
        CSVReader reader = null;
        List<String[]> csvData = Collections.emptyList();

        try {
            filereader = new FileReader(getResourceFile(PATH_PREFIX + fileName));

            CSVParser parser = new CSVParserBuilder().withSeparator(pdfDto.getFileDelimiter().charAt(0)).build();
            reader = new CSVReaderBuilder(filereader).withCSVParser(parser).build();

            csvData = reader.readAll();

            int numRows = csvData.size();
            int numColumns = csvData.get(0).length;

            logger.info("File - {} has been read. Table dimensions - {} * {} ", fileName, numRows, numColumns);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return csvData;
    }

    private List<String[]> parseCsvFile(String fileName, char delimiter) {

        //read file
        FileReader filereader = null;
        CSVReader reader = null;
        List<String[]> csvData = Collections.emptyList();

        try {
            filereader = new FileReader(getResourceFile(PATH_PREFIX + fileName));

            CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
            reader = new CSVReaderBuilder(filereader).withCSVParser(parser).build();

            csvData = reader.readAll();

            int numRows = csvData.size();
            int numColumns = csvData.get(0).length;

            logger.info("File - {} has been read. Table dimensions - {} * {} ", fileName, numRows, numColumns);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return csvData;
    }

    public void parseCsvAndBuildPdf(ReportPdfDto reportPdfDto) throws Exception {
        String fileName = reportPdfDto.getFileNames().get(0);

        //read file
        FileReader filereader = null;
        CSVReader reader = null;

        String outputFile = "C:\\Users\\jayakolisetty\\projects\\poc\\report-pdf-generator\\src\\main\\resources\\thymeleaf\\generated\\generated.pdf";
        OutputStream outputStream = new FileOutputStream(outputFile);
        ITextRenderer renderer = new ITextRenderer();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(templateResolver);
        boolean isFirstPage = true;
        Context context = createCommonContext(reportPdfDto);

        try {
            filereader = new FileReader(getResourceFile(PATH_PREFIX + fileName));

            CSVParser parser = new CSVParserBuilder().withSeparator(reportPdfDto.getFileDelimiter().charAt(0)).build();
            reader = new CSVReaderBuilder(filereader).withCSVParser(parser).build();

            int numTotalRows = reportPdfDto.getRecordCount();
            int numRowsPerPage = reportPdfDto.getRecordLimitPerPage();
            int numColumnsPerPage = reportPdfDto.getColumnLimitPerPage();


            String[] headers = reader.readNext();
            int numTotalColumns = headers.length;
            //reduce header row count
            numTotalRows--;
            List<String[]> headersPerPage = getHeadersPerPage(headers, numColumnsPerPage);
            logger.info("Header column read. Headers size - {} Headers - {}", numTotalColumns, Arrays.toString(headers));


            int normalPagesPerSlice = numTotalColumns / numColumnsPerPage;
            int numColumnsInRemainderPagePerSlice = numTotalColumns % numColumnsPerPage;

            int fullSliceRead = 0;

            int numFullSlices = numTotalRows / numRowsPerPage;
            int numRowsInRemainderSlice = numTotalRows % numRowsPerPage;

            int numPagesPerEachSlice = normalPagesPerSlice + (numColumnsInRemainderPagePerSlice > 0 ? 1 : 0);
            int totalSlices = numFullSlices + (numRowsInRemainderSlice > 0 ? 1 : 0);
            int totalPagesToBePrinted = totalSlices * numPagesPerEachSlice;

            logger.info("Started reading File - {}. Record count - {}, Column count - {}, row limit per page - {}," +
                    " column limit per page - {}", fileName, numTotalRows, numTotalColumns, numRowsPerPage, numColumnsPerPage);
            logger.info("No of pages printed per each horizontal partition - {}", numPagesPerEachSlice);
            logger.info("Total pages to be printed - {}", totalPagesToBePrinted);

            PdfMetadata defaultPdfMetadata = buildPdfMetadata(normalPagesPerSlice, numColumnsPerPage, numColumnsInRemainderPagePerSlice);

            // write rows in full slice
            while (fullSliceRead++ < numFullSlices) {
                List<String[]> slice = getRowsOfSlice(reader, numRowsPerPage);

                writePagesUsingSlice(slice, headersPerPage, context, outputStream, renderer,
                        defaultPdfMetadata, isFirstPage);
                isFirstPage = false;
            }


            //write rows in remainder slice if present
            if (numRowsInRemainderSlice > 0) {
                List<String[]> slice = getRowsOfSlice(reader, numRowsInRemainderSlice);

                writePagesUsingSlice(slice, headersPerPage, context, outputStream, renderer,
                        defaultPdfMetadata, isFirstPage);
            }

            renderer.finishPDF();
            outputStream.close();

            logger.info("Converting file - {} to pdf completed successfully. Destination pdf file - {}", fileName, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writePagesUsingSlice(List<String[]> slice, List<String[]> headersPerPage, Context context, OutputStream outputStream, ITextRenderer renderer,
                                      PdfMetadata pdfMetadata, boolean isFirstPage) throws DocumentException {
        int startColumnIndex = 0;
        int normalPagesPerSlice = pdfMetadata.getNormalPagesPerSlice();
        int numColumnsPerPage = pdfMetadata.getNumColumnsPerPage();
        int remainderPagesPerSlice = pdfMetadata.getNumColumnsInRemainderPagePerSlice();
        // write normal pages per slice
        for (int page = 0; page < normalPagesPerSlice; page++) {
            List<String[]> pageDataTable = new ArrayList<>();
            getPageDataTable(pageDataTable, slice, numColumnsPerPage, startColumnIndex);
            startColumnIndex += numColumnsPerPage;

            pdfMetadata.setPageNumber(pdfMetadata.getPageNumber() + 1);
            String html = updateContextAndProcessTemplate(context, headersPerPage.get(page),
                    pageDataTable, pdfMetadata.getPageNumber(), templateEngine);
            renderPage(renderer, outputStream, html, isFirstPage);
            isFirstPage = false;
        }

        //write remainder pages per slice
        if (remainderPagesPerSlice > 0) {
            List<String[]> pageDataTable = new ArrayList<>();
            getPageDataTable(pageDataTable, slice, remainderPagesPerSlice, startColumnIndex);

            pdfMetadata.setPageNumber(pdfMetadata.getPageNumber() + 1);
            String html = updateContextAndProcessTemplate(context, headersPerPage.get(headersPerPage.size() - 1),
                    pageDataTable, pdfMetadata.getPageNumber(), templateEngine);
            renderPage(renderer, outputStream, html, isFirstPage);
        }
    }

    private List<String[]> getRowsOfSlice(CSVReader reader, int numRowsToBeRead) throws CsvValidationException, IOException {
        List<String[]> slice = new ArrayList<>();
        for (int row = 0; row < numRowsToBeRead; row++) {
            slice.add(reader.readNext());
        }
        return slice;
    }

    private void getPageDataTable(List<String[]> pageDataTable, List<String[]> slice, int numColumnsPerPage, int columnStartIndex) {
        for (int j = 0; j < slice.size(); j++) {
            String[] pageColumnData = new String[numColumnsPerPage];
            int n = 0;
            for (int k = columnStartIndex; k < columnStartIndex + pageColumnData.length; k++) {
                pageColumnData[n++] = slice.get(j)[k];
            }
            pageDataTable.add(pageColumnData);
        }
    }

    private String updateContextAndProcessTemplate(Context context, String[] pageCsvHeaders, List<String[]> pageCsvData, Integer pageNumber, TemplateEngine templateEngine) {
        context.setVariable("csvDataHeaders", pageCsvHeaders);
        context.setVariable("csvDataRows", pageCsvData);
        context.setVariable("pageNumber", pageNumber);
        return templateEngine.process("thymeleaf/template/report-html-template", context);
    }

    private Context createCommonContext(ReportPdfDto reportPdfDto) throws IOException, URISyntaxException {
        Context context = new Context();
        context.setVariable("provider", "Sampath");
        context.setVariable("reportName", reportPdfDto.getCsvFileTitle());
        context.setVariable("startDate", reportPdfDto.getStartDate());
        context.setVariable("endDate", reportPdfDto.getEndDate());
        String embeddedImage = getConvertorLogoEncoded();
        context.setVariable("embeddedImage", "data:image/png;base64," + embeddedImage);
        String[] dateAndTime = getLocalDateTime();
        context.setVariable("createdDate", dateAndTime[0]);
        context.setVariable("createdTime", dateAndTime[1]);
        return context;
    }

    private void renderPage(ITextRenderer renderer, OutputStream outputStream, String html, boolean isFirstPage) throws DocumentException {
        //render page
        renderer.setDocumentFromString(html);
        renderer.layout();
        if (isFirstPage) {
            renderer.createPDF(outputStream, false);
        } else {
            renderer.writeNextDocument(1);
        }
    }


    private List<String[]> getHeadersPerPage(String[] headers, int numColumnsPerPage) {
        List<String[]> headersPerPageList = new ArrayList<>();
        int numHeaderColumns = headers.length;
        int numberOfPagesPerSlice = numHeaderColumns / numColumnsPerPage;
        int remainderPagesPerSlice = numHeaderColumns % numColumnsPerPage;

        int startPointer = 0;
        int endPointer = 0;

        for (int page = 0; page < numberOfPagesPerSlice; page++) {
            endPointer += numColumnsPerPage;
            headersPerPageList.add(Arrays.copyOfRange(headers, startPointer, endPointer));
            startPointer = endPointer;
        }

        if (remainderPagesPerSlice > 0) {
            headersPerPageList.add(Arrays.copyOfRange(headers, startPointer, (startPointer + remainderPagesPerSlice)));
        }

        return headersPerPageList;
    }

    private String getConvertorLogoEncoded() throws IOException, URISyntaxException {
        String logoPath = "static/images/convertor_logo.jiff";
        byte[] fileContent = FileUtils.readFileToByteArray(getResourceFile(logoPath));
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private File getResourceFile(String fileRelativePath) throws URISyntaxException {
        URL url = TemplateService.class.getClassLoader().getResource(fileRelativePath);
        return new File(url.toURI());
    }

    private String[] getLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        return localDateTime.format(myFormatObj).split(" ");
    }

    private PdfMetadata buildPdfMetadata(int normalPagesPerSlice, int numColumnsPerPage,
                                         int numColumnsInRemainderPagePerSlice) {
        PdfMetadata pdfMetadata = new PdfMetadata();
        pdfMetadata.setNormalPagesPerSlice(normalPagesPerSlice);
        pdfMetadata.setNumColumnsPerPage(numColumnsPerPage);
        pdfMetadata.setNumColumnsInRemainderPagePerSlice(numColumnsInRemainderPagePerSlice);
        pdfMetadata.setPageNumber(0);
        return pdfMetadata;
    }

    public void parseMultipleCsvAndGenerateSinglePdf(ReportPdfDto reportPdfDto) throws Exception {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(templateResolver);
        String outputFile = "C:\\Users\\jayakolisetty\\projects\\poc\\report-pdf-generator\\src\\main\\resources\\thymeleaf\\generated\\single-pdf\\generated.pdf";
        OutputStream outputStream = new FileOutputStream(outputFile);
        ITextRenderer renderer = new ITextRenderer();
        Context context = new Context();
        context.setVariable("provider", "Sampath");
        context.setVariable("reportName", reportPdfDto.getCsvFileTitle());
        context.setVariable("startDate", reportPdfDto.getStartDate());
        context.setVariable("endDate", reportPdfDto.getEndDate());

        try {
            String embeddedImage = getConvertorLogoEncoded();
            context.setVariable("embeddedImage", "data:image/png;base64," + embeddedImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, List<String[]>> tables = new LinkedHashMap<>();

        for (String file : reportPdfDto.getFileNames()) {
            tables.put(file.replaceFirst(".csv", ""),
                    parseCsvFile(file, reportPdfDto.getFileDelimiter().charAt(0)));
        }

        context.setVariable("tablesMap", tables);

        String html = templateEngine.process("thymeleaf/template/multiple-csv-single-pdf.html", context);

        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
    }


}
