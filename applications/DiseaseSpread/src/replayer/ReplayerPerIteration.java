/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package replayer;

import utilities.Metrics;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import org.apache.log4j.Logger;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;

/**
 * Loads logs, calculates and prints measurement results.
 * @author Evangelos
 */
public class ReplayerPerIteration {

    private static final Logger logger = Logger.getLogger(ReplayerPerIteration.class);
    
    private String expSeqNum;
    private String expID;
    private String resultID;

    private LogReplayer replayer;
    private final String coma=",";

//    private PrintWriter lineLossOut;
//    private PrintWriter flowOut;    
    private PrintWriter damageStatus;
    private PrintWriter damageLevel;
    private int nodeToInfect;
    static boolean writeToFile=true;

    public ReplayerPerIteration(String experimentSequenceNumber, int minLoad, int maxLoad, int nodeToInfect){
        this.nodeToInfect = nodeToInfect;
        this.expSeqNum=experimentSequenceNumber;
        this.expID="experiment-"+expSeqNum+"/";
        this.resultID="results/"+expSeqNum+"/";
        this.replayer=new LogReplayer();
        logger.info(expID);
        this.loadLogs("peerlets-log/"+expID, minLoad, maxLoad);
        if(writeToFile)
            this.prepareResultOutput();
        this.replayResults();
        if(writeToFile)
            this.closeFiles();
    }

    public void loadLogs(String directory, int minLoad, int maxLoad){
        try{
            File folder = new File(directory);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()&&!listOfFiles[i].isHidden()) {
                    MeasurementLog loadedLog=replayer.loadLogFromFile(directory+listOfFiles[i].getName());
                    MeasurementLog replayedLog=this.getMemorySupportedLog(loadedLog, minLoad, maxLoad);
                    replayer.mergeLog(replayedLog);
                }
                else
                    if (listOfFiles[i].isDirectory()) {
                        //do sth else
                    }
            }
        }
        catch(IOException io){

        }
        catch(ClassNotFoundException ex){

        }
    }
    
    private void prepareResultOutput(){
        try{
            File resultLocation = new File(resultID);
            clearExperimentFile(resultLocation);
            resultLocation.mkdirs();
            
            damageStatus = new PrintWriter(new BufferedWriter(new FileWriter(resultID+"damageStatus_"+Integer.toString(nodeToInfect)+".txt", true)));
            damageLevel = new PrintWriter(new BufferedWriter(new FileWriter(resultID+"damageLevel_"+Integer.toString(nodeToInfect)+".txt", true)));

        }
        catch (IOException e) {
                //exception handling left as an exercise for the reader
        }
    }
    
    public final static void clearExperimentFile(File experiment){
        File[] files = experiment.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    clearExperimentFile(f);
                } else {
                    f.delete();
                }
            }
        }
        experiment.delete();
    }
    
    private void closeFiles(){
        damageStatus.print("\n");
        damageLevel.print("\n");
        
        damageStatus.close();
        damageLevel.close();
    }

    public void replayResults(){
        this.printLocalMetricsTags();
        replayer.replayTo(new MeasurementLoggerListener(){
            public void measurementEpochEnded(MeasurementLog log, int epochNumber){
                calculateEpochResults(log, epochNumber);
            }
        });
    }

    private void calculatePeerResults(MeasurementLog globalLog){
        
    }

    private void calculateEpochResults(MeasurementLog log, int epochNumber){
        
        //tags.add("a1");tags.add("a2");tags.add("a3");
        //BenchmarkAnalysis smh = new BenchmarkAnalysis(expID, 0, 1000);
        //smh.powerPerIteration;
        
        for (int i=0; i<100; i++){ //42 or 114 is the total number of iterations, 41 is total number of lines
        double epochNum=epochNumber;
        double avgDamageStatusPerEpoch=(log.getAggregateByEpochNumber(epochNumber, "nodeDamageStatus"+Integer.toString(i)).getAverage());
        double avgDamageLevelPerEpoch=(log.getAggregateByEpochNumber(epochNumber, "nodeDamageLevel"+Integer.toString(i)).getAverage());

        if(writeToFile){
            damageStatus.print(avgDamageStatusPerEpoch + coma);
            damageLevel.print(avgDamageLevelPerEpoch+coma);
        }
        logger.info(String.format("%20.0f%20.0f\n", avgDamageStatusPerEpoch, avgDamageLevelPerEpoch));

    }
    }
    private MeasurementLog getMemorySupportedLog(MeasurementLog log, int minLoad, int maxLoad){
        return log.getSubLog(minLoad, maxLoad);
    }

    public void printGlobalMetricsTags(){
        logger.info("*** RESULTS PER PEER ***\n");
    }

    public void printLocalMetricsTags(){
        logger.info("*** RESULTS PER EPOCH ***\n");
        logger.info(String.format("%20s\n","AVG damage status", "AVG damage level"));
    }

    public double roundDecimals(double decimal, int decimalPlace) {
        BigDecimal bd = new BigDecimal(decimal);
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
        return bd.doubleValue();
    }

}