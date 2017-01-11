/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backend;
import backend.FlowNetworkDataTypesInterface;
import backend.FlowNetworkDataTypesInterface;
import network.Link;
import network.Node;
import org.apache.log4j.Logger;
import input.DisasterSpreadLinkState;
import input.DisasterSpreadNodeState;
/**
 *
 * @author dinesh
 */
public class DisasterSpreadNetworkDataTypes implements FlowNetworkDataTypesInterface{
    private static final Logger logger = Logger.getLogger(DisasterSpreadNetworkDataTypes.class);
    
    public DisasterSpreadNetworkDataTypes(){
        
    }
    
    @Override
    public Enum[] getNodeStates(){
        return DisasterSpreadNodeState.values();
    }
    
    @Override
    public Enum[] getLinkStates(){
        return DisasterSpreadLinkState.values();
    }
    
    @Override
    public DisasterSpreadNodeState parseNodeStateTypeFromString(String disasterNodeState){
        switch(disasterNodeState){
            case "id": 
                return DisasterSpreadNodeState.ID;
            case "damage":
                return DisasterSpreadNodeState.DAMAGE;
            case "alpha":
                return DisasterSpreadNodeState.ALPHA;
            case "beta":
                return DisasterSpreadNodeState.BETA;
            case "tolerance":
                return DisasterSpreadNodeState.TOLERANCE;
            case "recovery_rate":
                return DisasterSpreadNodeState.RECOVERYRATE;
            case "init_recovery_rate":
                return DisasterSpreadNodeState.INITIALRECOVERYRATE;
            default:
                logger.debug("Health node state is not recognized.");
                return null;
        }
    }
    
    
    @Override
    public DisasterSpreadLinkState parseLinkStateTypeFromString(String disasterLinkState){
        switch(disasterLinkState){
            case "id":
                return DisasterSpreadLinkState.ID;
            case "connection_strength":
                return DisasterSpreadLinkState.CONNECTION_STRENGTH;
            case "time_delay":
                return DisasterSpreadLinkState.TIME_DELAY;
            default:
                logger.debug("Disaster link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public Object parseNodeValuefromString(Enum nodeState, String rawValue){
        DisasterSpreadNodeState disasterSpreadNodeState = (DisasterSpreadNodeState)nodeState;
        switch(disasterSpreadNodeState){
            case ID:
                return Double.parseDouble(rawValue);
            case DAMAGE:
                return Double.parseDouble(rawValue);
            case ALPHA:
                return Double.parseDouble(rawValue);
            case BETA:
                return Double.parseDouble(rawValue);
            case TOLERANCE:
                return Double.parseDouble(rawValue);
            case RECOVERYRATE:
                return Double.parseDouble(rawValue);
            case INITIALRECOVERYRATE:
                return Double.parseDouble(rawValue);
            default:
                logger.debug("Disaster node state is not recognized.");
                return null;
        }    
    }  
    
    @Override
    public Object parseLinkValueFromString(Enum linkState, String rawValue){
        DisasterSpreadLinkState damageLinkState = (DisasterSpreadLinkState) linkState;
        switch(damageLinkState){
            case ID:
                return Double.parseDouble(rawValue);
            case TIME_DELAY:
                return Double.parseDouble(rawValue);
            case CONNECTION_STRENGTH:
                return Double.parseDouble(rawValue);
            default:
                logger.debug("Disaster link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public String castNodeStateTypeToString(Enum nodeState){
        DisasterSpreadNodeState disasterSpreadNodeState = (DisasterSpreadNodeState)nodeState;
        switch(disasterSpreadNodeState){
            case ID:
                return null;
            case DAMAGE:
                return "damage";
            case ALPHA:
                return "alpha";
            case BETA:
                return "beta";
            case TOLERANCE:
                return "tolerance";
            case RECOVERYRATE:
                return "recovery_rate";
            case INITIALRECOVERYRATE:
                return "init_recovery_rate";
            default:
                logger.debug("Disaster node state is not recognized.");
                return null;
        }    
    } 
    
    @Override
    public String castLinkStateTypeToString(Enum linkState){
        DisasterSpreadLinkState disasterSpreadLinkState = (DisasterSpreadLinkState)linkState;
        switch(disasterSpreadLinkState){
            case ID:
                return null;
            case TIME_DELAY:
                return "time_delay";
            case CONNECTION_STRENGTH:
                return "connection_strength";
            default:
                logger.debug("Disaster link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public String castNodeStateValueToString(Enum nodeState, Node node, String missingValue){
        DisasterSpreadNodeState disasterSpreadNodeState = (DisasterSpreadNodeState)nodeState;
        if (node.getProperty(disasterSpreadNodeState) == null)
            return missingValue;
        return String.valueOf(node.getProperty(disasterSpreadNodeState));
    }
    
    @Override
    public String castLinkStateValueToString(Enum linkState, Link link, String missingValue){
        DisasterSpreadLinkState disasterSpreadLinkState = (DisasterSpreadLinkState)linkState;
        if (link.getProperty(disasterSpreadLinkState) == null)
            return missingValue;
        return String.valueOf(link.getProperty(linkState));
    }
    
}
