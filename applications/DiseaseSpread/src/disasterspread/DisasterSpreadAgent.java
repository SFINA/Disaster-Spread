/*
 * Copyright (C) 2016 SFINA Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package disasterspread;

import core.SimulationAgent;
import disasterspread.input.DisasterSpreadNodeState;
import disasterspread.input.DisasterSpreadLinkState;
import disasterspread.backend.HelbingEtAlModelBackend;
import disasterspread.backend.DisasterSpreadBackendParameter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import network.Link;
import network.Node;
import org.apache.log4j.Logger;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.util.quantities.Time;

/**
 *
 * @author dinesh
 */
public class DisasterSpreadAgent extends SimulationAgent {

    //private HashMap<String,ArrayList<Double>> nodeHealthHistory;
    private double timeStep = 0.1;
    private static final Logger logger = Logger.getLogger(DisasterSpreadAgent.class);
    private int maxIterations = 100;
    private int strategy = 0;

    // resource distribution parameters from paper
    private final double a1 = 530;//10;//
    private final double b1 = .6;//1;//
    private final double c1 = .2;//0.01;//

    // strategy constants from paper
    private final double q = 0.15;
    private final double k = 0.8;
    public int nodeToInfect;

    private ArrayList<ArrayList<Double>> damageStatus = new ArrayList<ArrayList<Double>>();
    private ArrayList<ArrayList<Double>> damageLevel = new ArrayList<ArrayList<Double>>();
    public ArrayList<Double> averagedamageStatus = new ArrayList<Double>();
    public ArrayList<Double> averagedamageLevel = new ArrayList<Double>();

    List<Map.Entry<String, Integer>> beforeRewiring = new ArrayList<Map.Entry<String, Integer>>();
    List<Map.Entry<String, Integer>> afterRewiring = new ArrayList<Map.Entry<String, Integer>>();

    public DisasterSpreadAgent(String experimentID,
            Time bootstrapTime,
            Time runTime, int nodeToInfect) {
        super(experimentID,
                bootstrapTime,
                runTime);
        this.nodeToInfect = nodeToInfect;
    }

    @Override
    public void runFlowAnalysis() {
//        for (int i = 1; i < getFlowNetwork().getNodes().size()+1; i++) {
        getFlowNetwork().getNode(Integer.toString(nodeToInfect)).replacePropertyElement(DisasterSpreadNodeState.DAMAGE, 1.);

        for (Node node : getFlowNetwork().getNodes()) {
            node.addProperty(DisasterSpreadNodeState.DAMAGEHISTORY, new ArrayList<Double>(Collections.nCopies(maxHistory(), 0.0)));
        }
        /* DAMAGEHISTORY is not loaded from files. It is initialized with current damage level and 
         updated in every iteration. */

        for (Node node : getFlowNetwork().getNodes()) {
            ((ArrayList<Double>) node.getProperty(DisasterSpreadNodeState.DAMAGEHISTORY)).add(0, node.getFlow());
        }

        for (Node n : getFlowNetwork().getNodes()) {
            beforeRewiring.add(new AbstractMap.SimpleEntry<String, Integer>(n.getIndex(), n.getOutgoingLinks().size()));
        }
        for (int j = 0; j < maxIterations; j++) {
            if (j == 10) {

                //rewire();

            }
            /* At every iteration, compute the recovery rate of the Node. */
            for (Node n : getFlowNetwork().getNodes()) {
                n.addProperty(DisasterSpreadNodeState.RECOVERYRATE, recoveryRate(n));
            }

            callBackend(getFlowNetwork());

            System.out.println("Total Damaged Nodes: " + totalDamagedNodes());

            nextIteration();

            ArrayList<Double> damageStatusPerIteration = new ArrayList<Double>();

            ArrayList<Double> damageLevelPerIteration = new ArrayList<Double>();

            for (Node n : getFlowNetwork().getNodes()) {
                if (n.getFlow() > 0.5) {
                    damageStatusPerIteration.add(0.);
                } else {
                    damageStatusPerIteration.add(1.);
                }
                damageLevelPerIteration.add(n.getFlow());

            }

            damageStatus.add(damageStatusPerIteration);

            damageLevel.add(damageLevelPerIteration);

            averagedamageLevel.add(calculateAverage(damageLevelPerIteration));

        }
        for (Node n : getFlowNetwork().getNodes()) {
            afterRewiring.add(new AbstractMap.SimpleEntry<String, Integer>(n.getIndex(), n.getOutgoingLinks().size()));
        }

    }

    /* Computes the recovery rate depending on current iteration (time) and recovery strategy */
    private double recoveryRate(Node node) {

        double rStart = (Double) node.getProperty(DisasterSpreadNodeState.INITIALRECOVERYRATE);
        double alpha2 = 0.58;
        double beta2 = .2;
        return (rStart - beta2) * Math.exp(-alpha2 * externalResource(node)) + beta2;

    }

    private double externalResource(Node node) {

        if (this.getIteration() < 10) {
            return 0;
        }

        double r = a1 * Math.pow(this.getIteration() - 10, b1) * Math.exp(-c1 * (this.getIteration() - 10));
        int damaged = 0;
        int challenged = 0;
        double theta = (Double) node.getProperty(DisasterSpreadNodeState.TOLERANCE);
        int totalOutgoingOfDamaged = 0;
        int totalOutgoingOfChallenged = 0;

        for (Node n : getFlowNetwork().getNodes()) {

            if ((Double) n.getProperty(DisasterSpreadNodeState.DAMAGE) > theta) {
                damaged++;
                totalOutgoingOfDamaged += node.getOutgoingLinks().size();
            }

            if ((Double) n.getProperty(DisasterSpreadNodeState.DAMAGE) > 0.) {
                challenged++;
                totalOutgoingOfChallenged += node.getOutgoingLinks().size();
            }

        }

        switch (strategy) {
            case 0:
                return 0;
            case 1:
                return r / (getFlowNetwork().getNodes().size());
            case 2:
                return r * (node.getOutgoingLinks().size()) / (getFlowNetwork().getLinks().size());
            case 3:
                if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > 0) {
                    return r / (double) challenged;
                } else {
                    return 0;
                }

            case 4:
                if (damaged == 0) {
                    if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > 0) {
                        return r / (double) challenged;
                    } else {
                        return 0;
                    }
                } else if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > theta) {
                    return r / damaged;
                } else {
                    return 0;
                }

            case 5:
                List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>();
                for (Node n : getFlowNetwork().getNodes()) {
                    entries.add(new AbstractMap.SimpleEntry<String, Integer>(n.getIndex(), n.getOutgoingLinks().size()));
                }
                Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
                    public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                        return b.getValue().compareTo(a.getValue());
                    }
                });
                int highlyConnected = (int) Math.ceil(q * getFlowNetwork().getNodes().size());

                /* if it is not highly connected, use uniform distribution */
                for (int i = 0; i < highlyConnected; i++) {
                    if (entries.get(i).getKey() == node.getIndex()) {
                        return (r * k) / (double) highlyConnected;
                    }
                }

                /* just as in strategy 4, count both damaged and challended nodes among highly connected ones */
                damaged = 0;
                challenged = 0;
                for (int i = highlyConnected; i < entries.size(); i++) {
                    if ((Double) (((Node) getFlowNetwork().getNode(entries.get(i).getKey())).getProperty(DisasterSpreadNodeState.DAMAGE)) > (Double) (((Node) getFlowNetwork().getNode(entries.get(i).getKey())).getProperty(DisasterSpreadNodeState.TOLERANCE))) {
                        damaged++;
                    }
                    if ((Double) (((Node) getFlowNetwork().getNode(entries.get(i).getKey())).getProperty(DisasterSpreadNodeState.DAMAGE)) > 0) {
                        challenged++;
                    }
                }
                if (damaged == 0) {
                    if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > 0) {
                        return r * (1 - k) / (double) challenged;
                    } else {
                        return 0;
                    }
                } else if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > theta) {
                    return (1 - k) / damaged;
                } else {
                    return 0;
                }
            case 6:
                if (totalOutgoingOfDamaged == 0) {
                    if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > 0) {
                        return r * (node.getOutgoingLinks().size()) / totalOutgoingOfChallenged;
                    } else {
                        return 0;
                    }
                } else if ((Double) node.getProperty(DisasterSpreadNodeState.DAMAGE) > theta) {
                    return r * (node.getOutgoingLinks().size()) / (double) totalOutgoingOfDamaged;
                } else {
                    return 0;
                }

            default:
                return 0;
        }
    }

    public int totalDamagedNodes() {
        int damaged = 0;
        for (Node node : getFlowNetwork().getNodes()) {
            if (node.getFlow() > node.getCapacity()) {
                damaged++;
            }
        }
        return damaged;
    }

    private int maxHistory() {
        double maxConnectionDelay = 0;
        for (Link link : getFlowNetwork().getLinks()) {
            if ((Double) link.getProperty(DisasterSpreadLinkState.TIME_DELAY) > maxConnectionDelay) {
                maxConnectionDelay = (Double) link.getProperty(DisasterSpreadLinkState.TIME_DELAY);
            }
        }
        return (int) Math.ceil(maxConnectionDelay / timeStep);
    }

    @Override
    public void scheduleMeasurements() {
        setMeasurementDumper(new MeasurementFileDumper(getPeersLogDirectory() + this.getExperimentID() + "/peer-" + getPeer().getIndexNumber()));
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener() {
            public void measurementEpochEnded(MeasurementLog log, int epochNumber) {
                int simulationTime = getSimulationTime();
                if (simulationTime >= 1) {
                    log.logTagSet(simulationTime, new HashSet(getFlowNetwork().getLinks()), simulationTime);
                    //before 0 to 42 or 114
                    for (int i = 0; i < 100; i++) { //hardcoded because there is problem in time step for logreplayer
                        for (Node node : getFlowNetwork().getNodes()) {
                            //logger.info("Index out bound is  "+node.getIndex());
                            log.log(simulationTime, "nodeDamageStatus" + Integer.toString(i), ((Double) damageStatus.get(i).get(Integer.parseInt(node.getIndex()) - 1)));
                            log.log(simulationTime, "nodeDamageLevel" + Integer.toString(i), ((Double) damageLevel.get(i).get(Integer.parseInt(node.getIndex()) - 1)));
                        }
                    }
                }
                getMeasurementDumper().measurementEpochEnded(log, simulationTime);
                log.shrink(simulationTime, simulationTime + 1);

            }
        });
    }
    
    public void randomDamageSwap(){
        // number of nodes to sample
        int nodesToSwap = (int)(2*(Math.ceil((getFlowNetwork().getNodes().size()*0.3)/2))); // this division by two then ceil then multiplication by two guarantees that it is multiple of two
        
        ArrayList<Integer> shuffleID = new ArrayList<Integer>();
        
        for(int i =0;i<getFlowNetwork().getNodes().size();i++){
            shuffleID.add(i);
        }
        
        // shuffle the array (this is used to randomize the procedure)
        Collections.shuffle(shuffleID);
        
        // in ideal world, node ID need not be fixed from 1 to 500, so we prepare a list of array indices:
        ArrayList<String> nodeID = new ArrayList<String>();
        for(Node n: getFlowNetwork().getNodes()){
            nodeID.add(n.getIndex());
        }
        
        // select two random nodes and swap values
        for(int i = 0; i < nodesToSwap; i+=2){
            String ID1=nodeID.get(shuffleID.get(i));
            String ID2=nodeID.get(shuffleID.get(i+1));
            // swap damage levels:
            Double n1Damage = (Double)getFlowNetwork().getNode(ID1).getProperty(DisasterSpreadNodeState.DAMAGE);
            Double n2Damage = (Double)getFlowNetwork().getNode(ID2).getProperty(DisasterSpreadNodeState.DAMAGE);
            // replace value of node1:
            getFlowNetwork().getNode(ID1).replacePropertyElement(DisasterSpreadNodeState.DAMAGE, n2Damage);
            getFlowNetwork().getNode(ID2).replacePropertyElement(DisasterSpreadNodeState.DAMAGE, n1Damage);
            
        }
        
    }

    public void rewire() {
        List<Map.Entry<String, Integer>> sortDegree = new ArrayList<Map.Entry<String, Integer>>();
        List<Map.Entry<String, Integer>> highlyConnected10 = new ArrayList<Map.Entry<String, Integer>>();

        for (Node n : getFlowNetwork().getNodes()) {
            sortDegree.add(new AbstractMap.SimpleEntry<String, Integer>(n.getIndex(), n.getOutgoingLinks().size()));
        }
        Collections.sort(sortDegree, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });

        // can be removed
        for (int i = 0; i < Math.ceil(0.5 * getFlowNetwork().getNodes().size()); i++) {
            highlyConnected10.add(sortDegree.get(i));
        }

        for (int i = 0; i < highlyConnected10.size(); i++) {
            List<Map.Entry<Integer, String>> remLink = new ArrayList<Map.Entry<Integer, String>>();
            Node node1 = getFlowNetwork().getNode(highlyConnected10.get(i).getKey());
            for (Link link : node1.getOutgoingLinks()) {
                remLink.add(new AbstractMap.SimpleEntry<Integer, String>(link.getEndNode().getOutgoingLinks().size(), link.getEndNode().getIndex()));
            }

            Collections.sort(remLink, new Comparator<Map.Entry<Integer, String>>() {
                public int compare(Map.Entry<Integer, String> a, Map.Entry<Integer, String> b) {
                    return a.getKey().compareTo(b.getKey());
                }
            });

            for (int k = 0; k < Math.ceil((remLink.size()) * 0.8); k++) {
                // remove each individual link

                Node node_min = getFlowNetwork().getNode(remLink.get(k).getValue());
                if (node_min.getOutgoingLinks().size() != 1 && node1.getOutgoingLinks().size() != 1) { //To-Do check error
                    if (getFlowNetwork().getLink(node1, node_min) != null && getFlowNetwork().getLink(node_min, node1) != null) {

                        Link rem_link1 = getFlowNetwork().getLink(node1, node_min);

                        String indexLink1 = rem_link1.getIndex();

                        Link rem_link2 = getFlowNetwork().getLink(node_min, node1);
                        String indexLink2 = rem_link2.getIndex();

                        node1.removeLink(rem_link1);
                        node1.removeLink(rem_link2);
                        Node node_max_degree = getFlowNetwork().getNode(sortDegree.get(sortDegree.size() - k - 1).getKey());

                        Link link1 = new Link(indexLink1, true);
                        link1.setStartNode(node1);
                        link1.setEndNode(node_max_degree);
                        link1.isActivated();
                        link1.isConnected();
                        link1.addProperty(DisasterSpreadLinkState.CONNECTION_STRENGTH, 0.5);
                        link1.addProperty(DisasterSpreadLinkState.TIME_DELAY, 1.4);
                        getFlowNetwork().addLink(link1);

                        Link link2 = new Link(indexLink2, true);
                        link2.setEndNode(node1);
                        link2.setStartNode(node_max_degree);
                        link2.isActivated();
                        link2.isConnected();
                        link2.addProperty(DisasterSpreadLinkState.CONNECTION_STRENGTH, 0.5);
                        link2.addProperty(DisasterSpreadLinkState.TIME_DELAY, 1.4);
                        getFlowNetwork().addLink(link2);

                    }

                }
            }
        }
    }

    @Override
    public void runFinalOperations() {
        try (
                PrintStream outavgDamage = new PrintStream(new File("averageDamage" + Integer.toString(nodeToInfect) + ".txt"));) {
            String sc = "";
            for (int m = 0; m < averagedamageLevel.size(); m++) {

                sc += averagedamageLevel.get(m) + " ";
            }

            outavgDamage.println(sc);

            outavgDamage.close();

        } catch (FileNotFoundException p) {

            p.printStackTrace();
        }
        
        //writeBeforeAfterTopology();
    }

    public double calculateAverage(ArrayList<Double> arrayList) {
        double sum = 0.0;
        for (int i = 0; i < arrayList.size(); i++) {
            sum += arrayList.get(i);
        }

        return sum / arrayList.size();
    }

    public void writeBeforeAfterTopology() {
        try (
                PrintStream beforeRewireID = new PrintStream(new File("beforeRewireID" + Integer.toString(nodeToInfect) + ".txt"));) {
            String beforeID = "";
        for (Map.Entry<String, Integer> e : beforeRewiring) {
            String id = e.getKey();
            int degree = e.getValue();
            beforeID += id + " ";
        }
         beforeRewireID.println(beforeID);

            beforeRewireID.close();

        } catch (FileNotFoundException p) {

            p.printStackTrace();
        }
        
        
        try (
                PrintStream beforeRewireDegree = new PrintStream(new File("beforeRewireDegree" + Integer.toString(nodeToInfect) + ".txt"));) {
            String beforeDeg = "";
        for (Map.Entry<String, Integer> e : beforeRewiring) {
            String id = e.getKey();
            int degree = e.getValue();
            beforeDeg += degree + " ";
        }
         beforeRewireDegree.println(beforeDeg);

            beforeRewireDegree.close();

        } catch (FileNotFoundException p) {

            p.printStackTrace();
        }
        
        try (
                PrintStream afterRewireID = new PrintStream(new File("afterRewireID" + Integer.toString(nodeToInfect) + ".txt"));) {
            String afterID = "";
        for (Map.Entry<String, Integer> e : afterRewiring) {
            String id = e.getKey();
            int degree = e.getValue();
            afterID += id + " ";
        }
         afterRewireID.println(afterID);

            afterRewireID.close();

        } catch (FileNotFoundException p) {

            p.printStackTrace();
        }
        
        try (
                PrintStream afterRewireDegree = new PrintStream(new File("afterRewireDegree" + Integer.toString(nodeToInfect) + ".txt"));) {
            String afterDeg = "";
        for (Map.Entry<String, Integer> e : afterRewiring) {
            String id = e.getKey();
            int degree = e.getValue();
            afterDeg += degree + " ";
        }
         afterRewireDegree.println(afterDeg);

            afterRewireDegree.close();

        } catch (FileNotFoundException p) {

            p.printStackTrace();
        }
    }
}
