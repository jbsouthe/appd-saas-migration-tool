package com.cisco.josouthe;

import com.cisco.josouthe.util.TimeUtil;

import java.io.*;
import java.util.*;

public class GenerateTestData {
    private String csvOutputFilePrefix;
    private Map<Long,TestMetricData> testMetricDataMap = new HashMap<>();
    private Map<String,PrintWriter> filesByMonthMap = new HashMap<>();

    public PrintWriter getPrintWriterForMonth( long timestamp ) throws IOException {
        String month = TimeUtil.getMonth(timestamp);
        PrintWriter printWriter = filesByMonthMap.get(month);
        if( printWriter == null ) {
            printWriter = new PrintWriter( new FileWriter( String.format("%s-%s.csv",csvOutputFilePrefix,month)));
            filesByMonthMap.put(month, printWriter);
        }
        return printWriter;
    }

    public GenerateTestData(String targetFileName) throws IOException {

        csvOutputFilePrefix = targetFileName;
        TestMetricData testMetricData = new TestMetricData(144673964L);
        testMetricData.addLine("0,0,337,278,0,35,0,1301,277,23511,100,1170532");
        testMetricData.addLine("0,0,360,162,0,2,0,114,161,23511,100,1170532");
        testMetricData.addLine("0,0,360,135,0,2,0,82,134,23511,100,1170532");
        testMetricData.addLine("0,0,360,111,0,1,1,47,110,23511,100,1170532");
        testMetricData.addLine("0,0,360,89,0,2,1,29,88,23511,100,1170532");
        testMetricData.addLine("0,0,360,77,0,1,0,24,76,23511,100,1170532");
        testMetricDataMap.put(testMetricData.metricId, testMetricData);

        testMetricData = new TestMetricData(144673965L);
        for( String line : new String[]{
                "0,1,57,337,1,6,6,2017,337,23511,100,1170532",
                "0,1,60,360,6,6,6,2160,360,23511,100,1170532",
                "0,1,60,360,6,6,6,2160,360,23511,100,1170532",
                "0,1,60,360,6,6,6,2160,360,23511,100,1170532",
                "0,1,60,360,6,6,6,2160,360,23511,100,1170532",
                "0,1,60,360,6,6,6,2160,360,23511,100,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);

        testMetricData = new TestMetricData(144673969L);
        for( String line : new String[]{
                "0,0,337,278,0,35,0,1301,277,23511,100,1170532",
                "0,0,360,162,0,2,0,114,161,23511,100,1170532",
                "0,0,360,135,0,2,0,82,134,23511,100,1170532",
                "0,0,360,111,0,1,1,47,110,23511,100,1170532",
                "0,0,360,89,0,2,1,29,88,23511,100,1170532",
                "0,0,360,77,0,1,0,24,76,23511,100,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);

        testMetricData = new TestMetricData(144673970L);
        for( String line : new String[]{
                "1,1,6,0,0,0,0,0,0,23511,200,1170532",
                "1,1,4,0,0,0,0,0,0,23511,200,1170532",
                "1,1,0,0,0,0,0,0,0,23511,200,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);


        testMetricData = new TestMetricData(144673971L);
        for( String line : new String[]{
                "1,1,6,0,0,0,0,0,0,23511,200,1170532",
                "1,1,4,0,0,0,0,0,0,23511,200,1170532",
                "1,1,0,0,0,0,0,0,0,23511,200,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);


        testMetricData = new TestMetricData(144673972L);
        for( String line : new String[]{
                "1,1,6,0,0,0,0,0,0,23511,200,1170532",
                "1,1,4,0,0,0,0,0,0,23511,200,1170532",
                "1,1,0,0,0,0,0,0,0,23511,200,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);


        testMetricData = new TestMetricData(144673973L);
        for( String line : new String[]{
                "1,1,6,0,0,0,0,0,0,23511,200,1170532",
                "1,1,4,0,0,0,0,0,0,23511,200,1170532",
                "1,1,0,0,0,0,0,0,0,23511,200,1170532"
        }) {
            testMetricData.addLine(line);
        }
        testMetricDataMap.put(testMetricData.metricId, testMetricData);
    }

    public static void main( String ... args ) throws IOException {
        GenerateTestData generateTestData = new GenerateTestData(args[0]);
        generateTestData.create90DaysOfData();
    }

    public void create90DaysOfData() throws IOException {
        long timestamp = TimeUtil.getDaysBackTimestamp(90);
        long now = TimeUtil.now();
        System.out.println(String.format("start: %d finish %d difference: %d",timestamp,now,now - timestamp));
        while( timestamp < now ) {
            for( Long metricId : testMetricDataMap.keySet() ) {
                getPrintWriterForMonth(timestamp).println(String.format("%d,%d,%s",timestamp,metricId,testMetricDataMap.get(metricId).getRandomData()));
                System.out.println(String.format("%d,%d,%s",timestamp,metricId,testMetricDataMap.get(metricId).getRandomData()));
            }
            timestamp += 60*60*1000;
            getPrintWriterForMonth(timestamp).flush();
        }
        for( PrintWriter printWriter : filesByMonthMap.values() )
            printWriter.close();
    }

    public class TestMetricData {
        private Random random = new Random();
        public Long metricId;
        public List<String> values = new ArrayList<>();
        public TestMetricData( Long id ) { this.metricId=id; }
        public void addLine( String line ) {
            this.values.add(line);
        }
        public String getRandomData() {
            return values.get(random.nextInt(values.size()));
        }
    }
}
