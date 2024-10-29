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
    private List<XYSeries> diskSeriesList;  // List to hold series for each disk
    private int timeElapsed = 0;

    public ResourceGraph(String title) {
        super(title);
        cpuSeries = new XYSeries("CPU Load (%)");
        memorySeries = new XYSeries("Memory Usage (MB)");
        diskSeriesList = new ArrayList<>();

        // Create the CPU graph
        XYSeriesCollection cpuDataset = new XYSeriesCollection();
        cpuDataset.addSeries(cpuSeries);
        JFreeChart cpuChart = ChartFactory.createXYLineChart(
                "CPU Usage Over Time",
                "Time (s)",
                "CPU Load (%)",
                cpuDataset
        );

        // Customize CPU chart renderer
        XYLineAndShapeRenderer cpuRenderer = new XYLineAndShapeRenderer();
        cpuRenderer.setSeriesPaint(0, Color.decode("#00C7FD"));
        cpuRenderer.setSeriesShapesVisible(0, false);
        cpuChart.getXYPlot().setRenderer(cpuRenderer);
        ChartPanel cpuChartPanel = new ChartPanel(cpuChart);
        cpuChartPanel.setPreferredSize(new Dimension(800, 600));

        // Create the Memory graph
        XYSeriesCollection memoryDataset = new XYSeriesCollection();
        memoryDataset.addSeries(memorySeries);
        JFreeChart memoryChart = ChartFactory.createXYLineChart(
                "Memory Usage Over Time",
                "Time (s)",
                "Memory Usage (MB)",
                memoryDataset
        );

        // Customize Memory chart renderer
        XYLineAndShapeRenderer memoryRenderer = new XYLineAndShapeRenderer();
        memoryRenderer.setSeriesPaint(0, Color.BLACK);
        memoryRenderer.setSeriesShapesVisible(0, false);
        memoryChart.getXYPlot().setRenderer(memoryRenderer);
        ChartPanel memoryChartPanel = new ChartPanel(memoryChart);
        memoryChartPanel.setPreferredSize(new Dimension(800, 600));

        // Create disk usage charts for each detected disk
        Panel mainPanel = new Panel(new GridLayout(0, 1));  // Use dynamic rows for disks
        mainPanel.add(cpuChartPanel);
        mainPanel.add(memoryChartPanel);

        SystemInfo systemInfo = new SystemInfo();
        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();

        // Initialize series and charts for each disk
        for (HWDiskStore disk : diskStores) {
            // Extract "DRIVE#" from the disk name
            String diskName = disk.getName().replace("\\\\.\\PHYSICAL", ""); 
        
            // Create a series for the disk load
            XYSeries diskSeries = new XYSeries(diskName + " Load (%)");
            diskSeriesList.add(diskSeries);
        
            XYSeriesCollection diskDataset = new XYSeriesCollection();
            diskDataset.addSeries(diskSeries);
        
            // Create a disk load chart
            JFreeChart diskChart = ChartFactory.createXYLineChart(
                    diskName + " Disk Load Over Time",  // Use only the simplified disk name
                    "Time (s)",
                    "Disk Load (%)",
                    diskDataset
            );
        
            // Customize Disk chart renderer
            XYLineAndShapeRenderer diskRenderer = new XYLineAndShapeRenderer();
            diskRenderer.setSeriesPaint(0, Color.RED);
            diskRenderer.setSeriesShapesVisible(0, false);
            diskChart.getXYPlot().setRenderer(diskRenderer);
        
            ChartPanel diskChartPanel = new ChartPanel(diskChart);
            diskChartPanel.setPreferredSize(new Dimension(800, 600));
            mainPanel.add(diskChartPanel);
        }
        

        setContentPane(mainPanel);

        // Start updating the graphs
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
    
        // Get the current CPU ticks for calculating CPU load
        long[] prevTicks = processor.getSystemCpuLoadTicks();
    
        // Sleep for a second to get the updated ticks
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        // Get the CPU load using the previous ticks
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
    
        // Get memory usage
        long memoryUsedMB = (memory.getTotal() - memory.getAvailable()) / (1024 * 1024);
    
        // Update CPU and Memory series
        cpuSeries.add(timeElapsed, cpuLoad);
        memorySeries.add(timeElapsed, memoryUsedMB);
    
        // Update each disk's load based on read/write activity
        for (int i = 0; i < diskStores.size(); i++) {
            HWDiskStore disk = diskStores.get(i);
            long prevRead = disk.getReadBytes();
            long prevWrite = disk.getWriteBytes();
    
            // Refresh disk stats to get updated read/write bytes
            disk.updateAttributes();
            long currRead = disk.getReadBytes();
            long currWrite = disk.getWriteBytes();
    
            // Calculate the number of bytes transferred during the interval
            long readDifference = currRead - prevRead;
            long writeDifference = currWrite - prevWrite;
    
            // Total I/O in bytes
            long totalIoBytes = readDifference + writeDifference;
    
            // Assuming a disk has a maximum throughput (example: 200 MB/s for an SSD)
            double diskMaxThroughput = 200.0 * 1024 * 1024; // Adjust based on your disks
    
            // Calculate load percentage
            double loadPercentage = (totalIoBytes / diskMaxThroughput) * 100;
    
            // Ensure loadPercentage is capped at 100%
            loadPercentage = Math.min(loadPercentage, 100.0);
    
            // Update the disk series for the specific disk
            diskSeriesList.get(i).add(timeElapsed, loadPercentage);
        }
    
        timeElapsed++; // Increment time
    }
    

    public static void main(String[] args) {
        ResourceGraph chart = new ResourceGraph("Resource Usage Graph");
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}
