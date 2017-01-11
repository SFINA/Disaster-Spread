package input;


import backend.DisasterSpreadBackendParameter;
import input.SfinaParameterLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

public class DisasterSpreadBackendParameterLoader {
    
    private String columnSeparator;
    private static final Logger logger = Logger.getLogger(SfinaParameterLoader.class);
    
    public DisasterSpreadBackendParameterLoader(String columnSeparator){
        this.columnSeparator=columnSeparator;
    }
    
    public HashMap<Enum,Object> loadBackendParameters(String location){
        HashMap<Enum,Object> backendParameters = new HashMap();
        File file = new File(location);
        Scanner scr = null;
        try {
            scr = new Scanner(file);
            while(scr.hasNext()){
                StringTokenizer st = new StringTokenizer(scr.next(), columnSeparator);
                Enum param = null;
                Object value = null;
                
                switch(st.nextToken()){
                    case "strategy":
                        param = DisasterSpreadBackendParameter.STRATEGY;
                        value = Integer.parseInt(st.nextToken());
                        break;
                    case "nodeDamageHistory":
                        param = DisasterSpreadBackendParameter.NODEDAMAGEHISTORY;
                        // read and disregard the token
                        st.nextToken();
                        value = new HashMap();
                        break;
                    default:
                        logger.debug("This backend parameter is not supported or cannot be recognized");
                }
                
                backendParameters.put(param, value);
            }
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
        return backendParameters;
    }
}
