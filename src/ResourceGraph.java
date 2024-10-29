import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;

import java.awt.*;
import java.util.List;
import java.util.*;

public class ResourceGraph extends ApplicationFrame {

    private XYSeries cpuSeries;
    private XYSeries memorySeries;
    private List<XYSeries> diskSeriesList;
    private int timeElapsed = 0;

    public ResourceGraph(String title) {
        super(title);
        cpuSeries = new XYSeries("CPU Load (%)");
        memorySeries = new XYSeries("Memory Usage (MB)");
        diskSeriesList = new ArrayList<>();

        XYSeriesCollection cpuDataset = new XYSeriesCollection();
        cpuDataset.addSeries(cpuSeries);
        JFreeChart cpuChart = ChartFactory.createXYLineChart(
                "CPU Usage Over Time",
                "Time (s)",
                "CPU Load (%)",
                cpuDataset
        );

        XYLineAndShapeRenderer cpuRenderer = new XYLineAndShapeRenderer();
        cpuRenderer.setSeriesPaint(0, Color.decode("#00C7FD"));
        cpuRenderer.setSeriesShapesVisible(0, false);
        cpuChart.getXYPlot().setRenderer(cpuRenderer);
        ChartPanel cpuChartPanel = new ChartPanel(cpuChart);
        cpuChartPanel.setPreferredSize(new Dimension(800, 600));

        XYSeriesCollection memoryDataset = new XYSeriesCollection();
        memoryDataset.addSeries(memorySeries);
        JFreeChart memoryChart = ChartFactory.createXYLineChart(
                "Memory Usage Over Time",
                "Time (s)",
                "Memory Usage (MB)",
                memoryDataset
        );

        XYLineAndShapeRenderer memoryRenderer = new XYLineAndShapeRenderer();
        memoryRenderer.setSeriesPaint(0, Color.BLACK);
        memoryRenderer.setSeriesShapesVisible(0, false);
        memoryChart.getXYPlot().setRenderer(memoryRenderer);
        ChartPanel memoryChartPanel = new ChartPanel(memoryChart);
        memoryChartPanel.setPreferredSize(new Dimension(800, 600));

        Panel mainPanel = new Panel(new GridLayout(0, 1));
        mainPanel.add(cpuChartPanel);
        mainPanel.add(memoryChartPanel);

        SystemInfo systemInfo = new SystemInfo();
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();

        for (HWDiskStore disk : diskStores) {
            String diskName = disk.getName().replace("\\\\.\\PHYSICAL", ""); 
        
            XYSeries diskSeries = new XYSeries(diskName + " Load (%)");
            diskSeriesList.add(diskSeries);
        
            XYSeriesCollection diskDataset = new XYSeriesCollection();
            diskDataset.addSeries(diskSeries);
        
            JFreeChart diskChart = ChartFactory.createXYLineChart(
                    diskName + " Disk Load Over Time",
                    "Time (s)",
                    "Disk Load (%)",
                    diskDataset
            );
        
            XYLineAndShapeRenderer diskRenderer = new XYLineAndShapeRenderer();
            diskRenderer.setSeriesPaint(0, Color.RED);
            diskRenderer.setSeriesShapesVisible(0, false);
            diskChart.getXYPlot().setRenderer(diskRenderer);
        
            ChartPanel diskChartPanel = new ChartPanel(diskChart);
            diskChartPanel.setPreferredSize(new Dimension(800, 600));
            mainPanel.add(diskChartPanel);
        }
        

        setContentPane(mainPanel);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateGraph(systemInfo, diskStores);
            }
        }, 0, 1000);
    }

    private void updateGraph(SystemInfo systemInfo, List<HWDiskStore> diskStores) {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
    
        long[] prevTicks = processor.getSystemCpuLoadTicks();
    
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
    
        long memoryUsedMB = (memory.getTotal() - memory.getAvailable()) / (1024 * 1024);
    
        cpuSeries.add(timeElapsed, cpuLoad);
        memorySeries.add(timeElapsed, memoryUsedMB);
    
        for (int i = 0; i < diskStores.size(); i++) {
            HWDiskStore disk = diskStores.get(i);
            long prevRead = disk.getReadBytes();
            long prevWrite = disk.getWriteBytes();
    
            disk.updateAttributes();
            long currRead = disk.getReadBytes();
            long currWrite = disk.getWriteBytes();
    
            long readDifference = currRead - prevRead;
            long writeDifference = currWrite - prevWrite;
    
            long totalIoBytes = readDifference + writeDifference;
            double diskMaxThroughput = 200.0 * 1024 * 1024;
            double loadPercentage = (totalIoBytes / diskMaxThroughput) * 100;
          
            loadPercentage = Math.min(loadPercentage, 100.0);
    
            diskSeriesList.get(i).add(timeElapsed, loadPercentage);
        }
    
        timeElapsed++;
    }
    

    public static void main(String[] args) {
        ResourceGraph chart = new ResourceGraph("Resource Usage Graph");
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}
